@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.bunny.AnimatedTemporaryContainer
import software.amazon.app.platform.bunny.TemporaryContainer
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer

sealed interface ItineraryButtonModel : BaseModel, TemporaryContainer {
  data object Hide : ItineraryButtonModel {
    override val visible: Boolean = false
  }

  data object Show : ItineraryButtonModel {
    override val visible: Boolean = true
  }
}

@ContributesRenderer
class ItineraryButtonRenderer : ComposeRenderer<ItineraryButtonModel>() {
  @Composable
  override fun Compose(model: ItineraryButtonModel) {
    val size = 96.dp

    AnimatedTemporaryContainer<ItineraryButtonModel.Show>(model) { renderedModel ->
      val scale by animateFloat { visible -> if (visible) 1f else 0f }
      Box(
        modifier =
          Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            .width(size)
            .height(size)
            .background(Color(0xFFA5D1AE))
            .border(width = Dp.Hairline, color = Color.Black)
      )
    }
  }
}
