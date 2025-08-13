@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.bunny.AnimatedTemporaryContainer
import software.amazon.app.platform.bunny.BunnyLayerSharedElementKey
import software.amazon.app.platform.bunny.LocalWindowSize
import software.amazon.app.platform.bunny.TemporaryContainer
import software.amazon.app.platform.bunny.WindowSize
import software.amazon.app.platform.bunny.requireAnimatedVisibilityScope
import software.amazon.app.platform.bunny.withSharedTransitionScope
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer

sealed interface ActionButtonModel : BaseModel, TemporaryContainer {
  data object Hide : ActionButtonModel {
    override val visible: Boolean = false
  }

  data class Show(val applyMaxWidth: Boolean = true, val horizontal: Boolean = true) :
    ActionButtonModel {
    override val visible: Boolean = true
  }
}

@ContributesRenderer
class ActionButtonRenderer : ComposeRenderer<ActionButtonModel>() {
  @Composable
  override fun Compose(model: ActionButtonModel) {
    val size = 96.dp

    withSharedTransitionScope {
      AnimatedTemporaryContainer<ActionButtonModel.Show>(model) { renderedModel ->
        val scale by animateFloat { visible -> if (visible) 1f else 0f }

        Box(
          modifier =
            Modifier.sharedElement(
                sharedContentState =
                  rememberSharedContentState(key = BunnyLayerSharedElementKey.ActionButton),
                animatedVisibilityScope = requireAnimatedVisibilityScope(),
              )
              .graphicsLayer(scaleX = scale, scaleY = scale)
              .let {
                if (renderedModel.horizontal) {
                  it
                    .widthIn(
                      max =
                        if (renderedModel.applyMaxWidth) {
                          if (LocalWindowSize.current == WindowSize.Portrait) {
                            494.dp
                          } else {
                            255.dp
                          }
                        } else {
                          Dp.Unspecified
                        }
                    )
                    .fillMaxWidth()
                    .height(size)
                } else {
                  it
                    .heightIn(max = if (renderedModel.applyMaxWidth) 255.dp else Dp.Unspecified)
                    .fillMaxHeight()
                    .width(size)
                }
              }
              .background(Color(0xFFF5CDC5))
              .border(width = Dp.Hairline, color = Color.Black)
        )
      }
    }
  }
}
