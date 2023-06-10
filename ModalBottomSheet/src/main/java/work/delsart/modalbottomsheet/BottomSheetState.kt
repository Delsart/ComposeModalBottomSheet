package work.delsart.modalbottomsheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign

const val TAG = "BottomSheet"

enum class BottomSheetValue {
    Hide,
    Expanded,
    Full,
}


private fun <BottomSheetValue> Map<Float, BottomSheetValue>.getNearState(offset: Float): BottomSheetValue? {
    var minDistance = Float.MAX_VALUE
    var state: BottomSheetValue? = null
    entries.forEach {
        if ((it.key - offset).absoluteValue < minDistance) {
            minDistance = (it.key - offset).absoluteValue
            state = it.value
        }
    }
    return state
}

/**
 *  Given an offset x and a set of anchors, return a list of anchors:
 *   1. [ ] if the set of anchors is empty,
 *   2. [ x' ] if x is equal to one of the anchors, accounting for a small rounding error, where x'
 *      is x rounded to the exact value of the matching anchor,
 *   3. [ min ] if min is the minimum anchor and x < min,
 *   4. [ max ] if max is the maximum anchor and x > max, or
 *   5. [ a , b ] if a and b are anchors such that a < x < b and b - a is minimal.
 */
private fun findBounds(
    offset: Float,
    anchors: Set<Float>
): List<Float> {
    // Find the anchors the target lies between with a little bit of rounding error.
    val a = anchors.filter { it <= offset + 0.001 }.maxOrNull()
    val b = anchors.filter { it >= offset - 0.001 }.minOrNull()

    return when {
        a == null ->
            // case 1 or 3
            listOfNotNull(b)

        b == null ->
            // case 4
            listOf(a)

        a == b ->
            // case 2
            listOf(a)

        else ->
            // case 5
            listOf(a, b)
    }
}

private fun computeTarget(
    offset: Float,
    lastValue: Float,
    anchors: Set<Float>,
    thresholds: (Float, Float) -> Float,
    velocity: Float,
    velocityThreshold: Float
): Float {
    val bounds = findBounds(offset, anchors)
    return when (bounds.size) {
        0 -> lastValue
        1 -> bounds[0]
        else -> {
            val lower = bounds[0]
            val upper = bounds[1]
            if (lastValue <= offset) {
                // Swiping from lower to upper (positive).
                if (velocity >= velocityThreshold) {
                    return upper
                } else {
                    val threshold = thresholds(lower, upper)
                    if (offset < threshold) lower else upper
                }
            } else {
                // Swiping from upper to lower (negative).
                if (velocity <= -velocityThreshold) {
                    return lower
                } else {
                    val threshold = thresholds(upper, lower)
                    if (offset > threshold) upper else lower
                }
            }
        }
    }
}


@Stable
class BottomSheetState(
    initialValue: BottomSheetValue,
    val inAnimationSpec: AnimationSpec<Float> = spring(),
    val outAnimationSpec: AnimationSpec<Float> = inAnimationSpec,
    val confirmStateChange: (BottomSheetValue) -> Boolean = { true },
    val afterStateChange: (BottomSheetValue) -> Unit = { }
) {
    val isShow: Boolean
        get() = anchors.getOffset(BottomSheetValue.Hide) - this.offset > 1


    val isExpanded: Boolean
        get() = currentValue == BottomSheetValue.Expanded
    val isFull: Boolean
        get() = currentValue == BottomSheetValue.Full

    val isHidden: Boolean
        get() = currentValue == BottomSheetValue.Hide

    suspend fun show() = animateTo(BottomSheetValue.Expanded)

    suspend fun hide() = animateTo(BottomSheetValue.Hide)


    var currentValue: BottomSheetValue by mutableStateOf(initialValue)
        private set

    /**
     * Whether the state is currently animating.
     */
    var isAnimationRunning: Boolean by mutableStateOf(false)
        private set

    var isDragging: Boolean by mutableStateOf(false)
        private set


    var offset by mutableStateOf(0f)
        private set

    var overflow by mutableStateOf(0f)
        private set

    // the source of truth for the "real"(non ui) position
    // basically position in bounds + overflow
    private var absoluteOffset by mutableStateOf(0f)

    // current animation target, if animating, otherwise null
    private var animationTarget by mutableStateOf<Float?>(null)

    private var anchors by mutableStateOf(emptyMap<Float, BottomSheetValue>())

    private var thresholds: (Float, Float) -> Float by mutableStateOf({ _, _ -> 0f })

    private var velocityThreshold by mutableStateOf(0f)


    private var minBound = Float.NEGATIVE_INFINITY
    private var maxBound = Float.POSITIVE_INFINITY

    private fun getAnimationSpec(targetValue: BottomSheetValue): AnimationSpec<Float> {
        return when (targetValue) {
            BottomSheetValue.Hide -> outAnimationSpec
            else -> inAnimationSpec
        }
    }

    private fun <BottomSheetValue> Map<Float, BottomSheetValue>.getOffset(state: BottomSheetValue): Float {
        var result = Float.MAX_VALUE
        entries.forEach {
            if (it.key < result)
                result = it.key
            if (it.value == state)
                return result
        }
        return result
    }

    internal suspend fun initState(
        newAnchors: Map<Float, BottomSheetValue>,
        thresholds: (Float, Float) -> Float,
        velocityThreshold: Float
    ) {
        val oldAnchors = anchors
        anchors = newAnchors
        this.thresholds = thresholds
        this.velocityThreshold = velocityThreshold

        if (oldAnchors.isEmpty()) {
            minBound = newAnchors.keys.minOrNull()!!
            maxBound = newAnchors.keys.maxOrNull()!!
            snapInternal(currentValue)
        } else if (newAnchors != oldAnchors) {
            minBound = Float.NEGATIVE_INFINITY
            maxBound = Float.POSITIVE_INFINITY
            val animationTargetValue = animationTarget
            val targetState = if (animationTargetValue != null) {
                // if we are animating
                oldAnchors[animationTargetValue]
            } else {
                // we're not animating, proceed by finding the new anchors for an old value
                oldAnchors[offset]

            }!!
            try {
                animateInternal(targetState)
            } catch (c: CancellationException) {
                // If the animation was interrupted for any reason, snap as a last resort.
                snapInternal(targetState)
            } finally {
                currentValue = targetState
                minBound = newAnchors.keys.minOrNull()!!
                maxBound = newAnchors.keys.maxOrNull()!!
            }
        }
    }


    internal val draggableState = DraggableState {
        val newAbsolute = absoluteOffset + it
        val clamped = newAbsolute.coerceIn(minBound, maxBound)
        val overflow = newAbsolute - clamped
        this.offset = clamped
        this.overflow = overflow
        absoluteOffset = newAbsolute
    }

    private suspend fun snapInternal(target: BottomSheetValue) {
        draggableState.drag {
            dragBy(anchors.getOffset(target) - absoluteOffset)
        }
        currentValue = targetValue
    }


    private suspend fun animateInternal(target: BottomSheetValue) {
        draggableState.drag {
            var prevValue = absoluteOffset
            animationTarget = anchors.getOffset(target)
            isAnimationRunning = true
            try {
                val animationSpec = getAnimationSpec(target)
                Animatable(prevValue).animateTo(animationTarget!!, animationSpec) {
                    dragBy(this.value - prevValue)
                    prevValue = this.value
                }
                currentValue = targetValue
            } finally {
                animationTarget = null
                isAnimationRunning = false
            }
        }
    }


    val targetValue: BottomSheetValue
        get() {
            val target = animationTarget ?: computeTarget(
                offset = this.offset,
                lastValue = anchors.getOffset(currentValue),
                anchors = anchors.keys,
                thresholds = thresholds,
                velocity = 0f,
                velocityThreshold = Float.POSITIVE_INFINITY
            )
            return anchors[target] ?: currentValue
        }


    val direction: Float
        get() = sign(this.offset - anchors.getOffset(currentValue))

    suspend fun snapTo(targetValue: BottomSheetValue) {
        snapInternal(targetValue)
    }


    private suspend fun animateTo(targetValue: BottomSheetValue) {
        try {
            animateInternal(targetValue)
        } finally {
            val endOffset = absoluteOffset
            val endValue = anchors
                // fighting rounding error once again, anchor should be as close as 0.5 pixels
                .filterKeys { anchorOffset -> abs(anchorOffset - endOffset) < 2f }
                .values.firstOrNull() ?: currentValue
            currentValue = endValue
            afterStateChange(targetValue)
        }
    }

    suspend fun performFling(velocity: Float) {
        isDragging = false
        val lastAnchor = currentValue
        val targetValue = computeTarget(
            offset = this.offset,
            lastValue = anchors.getOffset(lastAnchor),
            anchors = anchors.keys,
            thresholds = thresholds,
            velocity = velocity,
            velocityThreshold = velocityThreshold
        )
        val targetState = anchors[targetValue]
        if (targetState != null && confirmStateChange(targetState)) {
            animateTo(targetState)
        }
        // If the user vetoed the state change, rollback to the previous state.
        else animateInternal(lastAnchor)


    }


    fun startDrag() {
        isDragging = true
    }

    fun performDrag(delta: Float): Float {
        val potentiallyConsumed = absoluteOffset + delta
        val clamped = potentiallyConsumed.coerceIn(minBound, maxBound)
        val deltaToConsume = clamped - absoluteOffset
        if (abs(deltaToConsume) > 0) {
            isDragging = true
            draggableState.dispatchRawDelta(deltaToConsume)
        }
        return deltaToConsume
    }


    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.Drag) {

                performDrag(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            return if (source == NestedScrollSource.Drag) {

                performDrag(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            isDragging = false
            val toFling = Offset(available.x, available.y).toFloat()
            return if (toFling < 0 && this@BottomSheetState.offset > minBound) {
                performFling(velocity = toFling)
                // since we go to the anchor with tween settling, consume all for the best UX
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            performFling(velocity = Offset(available.x, available.y).toFloat())
            return available
        }

        private fun Float.toOffset(): Offset = Offset(0f, this)

        private fun Offset.toFloat(): Float = this.y
    }


    companion object {
        fun Saver(
            inAnimationSpec: AnimationSpec<Float>,
            outAnimationSpec: AnimationSpec<Float>,
            confirmStateChange: (BottomSheetValue) -> Boolean
        ): Saver<BottomSheetState, *> = Saver(save = { it.currentValue }, restore = {
            BottomSheetState(
                initialValue = it,
                inAnimationSpec = inAnimationSpec,
                outAnimationSpec = outAnimationSpec,
                confirmStateChange = confirmStateChange
            )
        })
    }
}


@Composable
fun rememberBottomSheetDialogState(
    initialValue: BottomSheetValue,
    inAnimationSpec: AnimationSpec<Float> = spring(),
    outAnimationSpec: AnimationSpec<Float> = inAnimationSpec,
    confirmStateChange: (BottomSheetValue) -> Boolean = { true },
    afterStateChange: (BottomSheetValue) -> Unit = { }
): BottomSheetState {
    return rememberSaveable(
        inAnimationSpec, outAnimationSpec, saver = BottomSheetState.Saver(
            inAnimationSpec = inAnimationSpec,
            outAnimationSpec = outAnimationSpec,
            confirmStateChange = confirmStateChange
        )
    ) {
        BottomSheetState(
            initialValue = initialValue,
            inAnimationSpec = inAnimationSpec,
            outAnimationSpec = outAnimationSpec,
            confirmStateChange = confirmStateChange,
            afterStateChange = afterStateChange
        )
    }
}