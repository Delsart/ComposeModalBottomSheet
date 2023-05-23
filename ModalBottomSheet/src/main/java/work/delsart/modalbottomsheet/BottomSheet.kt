package work.delsart.modalbottomsheet

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
@NonRestartableComposable
fun BottomSheet(sheetContent: @Composable ColumnScope.() -> Unit, content: @Composable () -> Unit) {
    val state = rememberBottomSheetDialogState(
        initialValue = BottomSheetValue.Hide,
        inAnimationSpec = spring(stiffness = 1000f),
        outAnimationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    CompositionLocalProvider(LocalBottomSheetState provides state) {
        content()

        BaseBottomSheet(
            modifier = Modifier.clip(
                RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 32.dp
                )
            ),
            state = state,
            scrimColor = MaterialTheme.colorScheme.scrim,

            ) {

            val color =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(if (state.isDragging) 1f else 0.4f)
            // handle bar
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .drawBehind {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(
                            (size.width / 2) - 21.dp.toPx(),
                            size.height / 2
                        ),
                        size = Size(
                            width = 42.dp.toPx(),
                            height = 4.dp.toPx()
                        ),
                        cornerRadius = CornerRadius(
                            21.dp.toPx(),
                            21.dp.toPx()
                        )
                    )
                }
            )
            sheetContent()

        }

    }
}


val LocalBottomSheetState: ProvidableCompositionLocal<BottomSheetState> =
    staticCompositionLocalOf { error("BottomSheetState not initialized") }
