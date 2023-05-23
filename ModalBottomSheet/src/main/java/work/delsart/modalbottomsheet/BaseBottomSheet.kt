package work.delsart.modalbottomsheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
@NonRestartableComposable
fun BaseBottomSheet(
    modifier: Modifier = Modifier,
    state: BottomSheetState,
    sheetGesturesEnabled: Boolean = true,
    sheetBackgroundColor: Color = Color.White,
    scrimColor: Color = Color.Black.copy(alpha = 0.3f),
    scrimAnimationSpec: AnimationSpec<Float> = spring(),
    reserveHeight: Dp = 200.dp,
    thresholds: (Float, Float) -> Float = { a, b ->
        a + (b - a) / 2
    },
    velocityThreshold: Dp = 48.dp,
    sheetContent: @Composable ColumnScope.() -> Unit,
) = BoxWithConstraints(Modifier.fillMaxSize()) {

    val coroutineScope = rememberCoroutineScope()

    val fullHeight = constraints.maxHeight.toFloat()
    var bottomSheetHeight by remember { mutableStateOf(fullHeight) }

    val reserveHeightPx = with(LocalDensity.current) {
        remember(reserveHeight)
        {
            reserveHeight.toPx().toInt()
        }
    }


    val velocityThresholdPx = with(LocalDensity.current) {
        remember(reserveHeight)
        {
            velocityThreshold.toPx()
        }
    }

    val anchors = remember(fullHeight, bottomSheetHeight) {
        if (fullHeight - bottomSheetHeight > reserveHeightPx)
            mapOf(
                fullHeight to BottomSheetValue.Hide,
                fullHeight - bottomSheetHeight to BottomSheetValue.Expanded,
            )
        else
            mapOf(
                fullHeight to BottomSheetValue.Hide,
                fullHeight + reserveHeightPx - bottomSheetHeight to BottomSheetValue.Expanded,
                fullHeight - bottomSheetHeight to BottomSheetValue.Full
            )
    }


    LaunchedEffect(anchors, state) {
        state.initState(
            anchors,
            thresholds,
            velocityThresholdPx,
        )
    }

    val dragModifier = remember(fullHeight, bottomSheetHeight) {

        Modifier
            .nestedScroll(state.nestedScrollConnection)
            .draggable(
                orientation = Orientation.Vertical,
                enabled = sheetGesturesEnabled,
                startDragImmediately = state.isAnimationRunning,
                onDragStarted = { state.startDrag() },
                onDragStopped = { velocity ->
                    launch { state.performFling(velocity) }
                },
                state = state.draggableState
            )
            .pointerInput(Unit) {
                detectTapGestures {
                    if (it.y < state.offset)
                        coroutineScope.launch {
                            state.hide()
                        }
                }
            }
    }


    val scrimAlpha by animateFloatAsState(
        if (state.isShow) 0.4f else 0f,
        animationSpec = scrimAnimationSpec,
        label = ""
    )



    Layout(
        modifier = (if (state.isShow)
            dragModifier
        else
            Modifier)
            .fillMaxSize()
            .drawBehind {

                drawRect(
                    topLeft = Offset.Zero,
                    color = scrimColor,
                    alpha = scrimAlpha
                )
            },
        content = {
            Column(modifier.drawBehind {
                drawRect(sheetBackgroundColor)
            }) {
                Column(
                    modifier = Modifier.onSizeChanged {
                        bottomSheetHeight = it.height.toFloat()
                    }
                )
                {
                    sheetContent()

                }
                Spacer(
                    modifier = modifier
                        .weight(1f)
                )
            }

        }
    ) { measures, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            val sheetPlaceable =
                measures.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
            val sheetOffsetY = state.offset.roundToInt()
            sheetPlaceable.place(0, sheetOffsetY)
        }

    }

}