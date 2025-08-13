@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.bunny.BunnyLayerSharedElementKey
import software.amazon.app.platform.bunny.requireAnimatedVisibilityScope
import software.amazon.app.platform.bunny.withSharedTransitionScope
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer

class RecenterButtonModel : BaseModel

@ContributesRenderer
class RecenterButtonRenderer : ComposeRenderer<RecenterButtonModel>() {
  @Composable
  override fun Compose(model: RecenterButtonModel) {
    val size = 96.dp

    withSharedTransitionScope {
      Box(
        modifier =
          Modifier.sharedElement(
              sharedContentState =
                rememberSharedContentState(key = BunnyLayerSharedElementKey.RecenterButton),
              animatedVisibilityScope = requireAnimatedVisibilityScope(),
            )
            .width(size)
            .height(size)
            .background(Color(0xFFF3E0CD))
            .border(width = Dp.Hairline, color = Color.Black)
      )
    }
  }
}
