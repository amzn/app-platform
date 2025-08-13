@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.bunny.BunnyLayerSharedElementKey
import software.amazon.app.platform.bunny.LocalWindowSize
import software.amazon.app.platform.bunny.WindowSize
import software.amazon.app.platform.bunny.requireAnimatedVisibilityScope
import software.amazon.app.platform.bunny.withSharedTransitionScope
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer

class PolaroidModel : BaseModel

@ContributesRenderer
class PolaroidRenderer : ComposeRenderer<PolaroidModel>() {
  @Composable
  override fun Compose(model: PolaroidModel) {
    val size = if (LocalWindowSize.current == WindowSize.Portrait) 500.dp else 320.dp

    withSharedTransitionScope {
      Box(
        modifier =
          Modifier.sharedElement(
              sharedContentState =
                rememberSharedContentState(key = BunnyLayerSharedElementKey.Polaroid),
              animatedVisibilityScope = requireAnimatedVisibilityScope(),
            )
            .widthIn(max = size)
            .heightIn(max = size)
            .fillMaxSize()
            .background(Color(0xFFFACC9B))
            .border(width = Dp.Hairline, color = Color.Black)
      )
    }
  }
}
