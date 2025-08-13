@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import software.amazon.app.platform.bunny.AnimatedTemporaryContainer
import software.amazon.app.platform.bunny.BunnyLayerSharedElementKey
import software.amazon.app.platform.bunny.TemporaryContainer
import software.amazon.app.platform.bunny.requireAnimatedVisibilityScope
import software.amazon.app.platform.bunny.withSharedTransitionScope
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer

sealed interface AboutMenuModel : BaseModel, TemporaryContainer {
  data object Hide : AboutMenuModel {
    override val visible: Boolean = false
  }

  data class Show(val text: String = "About") : AboutMenuModel {
    override val visible: Boolean = true
  }
}

@ContributesRenderer
class AboutMenuRenderer : ComposeRenderer<AboutMenuModel>() {
  @Composable
  override fun Compose(model: AboutMenuModel) {
    withSharedTransitionScope {
      AnimatedTemporaryContainer<AboutMenuModel.Show>(model) { renderedModel ->
        val scale by animateFloat { visible -> if (visible) 1f else 0f }

        Box(
          modifier =
            Modifier.sharedElement(
                sharedContentState =
                  rememberSharedContentState(key = BunnyLayerSharedElementKey.AboutMenu),
                animatedVisibilityScope = requireAnimatedVisibilityScope(),
              )
              .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                transformOrigin = TransformOrigin(1f, 0f),
              )
              .fillMaxSize()
              .background(Color(0xFFC68CEE))
              .border(width = Dp.Hairline, color = Color.Black)
        ) {
          Text(text = renderedModel.text, modifier = Modifier.align(Alignment.Center))
        }
      }
    }
  }
}
