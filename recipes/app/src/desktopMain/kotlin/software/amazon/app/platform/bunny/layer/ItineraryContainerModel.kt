@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import software.amazon.app.platform.bunny.AnimatedTemporaryContainer
import software.amazon.app.platform.bunny.BunnyLayerSharedElementKey
import software.amazon.app.platform.bunny.TemporaryContainer
import software.amazon.app.platform.bunny.requireAnimatedVisibilityScope
import software.amazon.app.platform.bunny.withSharedTransitionScope
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer

sealed interface ItineraryContainerModel : BaseModel, TemporaryContainer {
  data object Hide : ItineraryContainerModel {
    override val visible: Boolean = false
  }

  data object Show : ItineraryContainerModel {
    override val visible: Boolean = true
  }
}

@ContributesRenderer
class ItineraryContainerRenderer : ComposeRenderer<ItineraryContainerModel>() {
  @Composable
  override fun Compose(model: ItineraryContainerModel) {
    withSharedTransitionScope {
      AnimatedTemporaryContainer<ItineraryContainerModel.Show>(model) { renderedModel ->
        Box(
          modifier =
            Modifier.sharedElement(
                sharedContentState =
                  rememberSharedContentState(key = BunnyLayerSharedElementKey.ItineraryContainer),
                animatedVisibilityScope = requireAnimatedVisibilityScope(),
              )
              .fillMaxSize()
              .background(Color(0xFFA5D1AE))
              .border(width = Dp.Hairline, color = Color.Black)
        )
      }
    }
  }
}
