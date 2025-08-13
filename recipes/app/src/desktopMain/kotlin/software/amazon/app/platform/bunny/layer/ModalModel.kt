package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.bunny.AnimatedTemporaryContainer
import software.amazon.app.platform.bunny.LocalWindowSize
import software.amazon.app.platform.bunny.TemporaryContainer
import software.amazon.app.platform.bunny.WindowSize
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer

sealed interface ModalModel : BaseModel, TemporaryContainer {

  data object Hide : ModalModel {
    override val visible: Boolean = false
  }

  sealed interface Show : ModalModel {

    override val visible: Boolean
      get() = true

    val text: String

    data class Interactive(override val text: String = "Modal container") : Show

    data class FullScreen(override val text: String = "Modal container") : Show
  }
}

@ContributesRenderer
class ModalRenderer : ComposeRenderer<ModalModel>() {
  @Composable
  override fun Compose(model: ModalModel) {
    AnimatedTemporaryContainer<ModalModel.Show>(model) { renderedModel ->
      val scale by animateFloat { visible -> if (visible) 1f else 0f }

      when (renderedModel) {
        is ModalModel.Show.FullScreen -> {
          Box(
            modifier =
              Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                .fillMaxSize()
                .background(Color(0xFFFFD445))
          ) {
            Text(text = renderedModel.text, modifier = Modifier.align(Alignment.Center))
          }
        }

        is ModalModel.Show.Interactive -> {
          Box(
            modifier =
              Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                .let {
                  if (LocalWindowSize.current == WindowSize.Portrait) {
                    it.fillMaxWidth().padding(horizontal = 32.dp)
                  } else {
                    it.fillMaxWidth(0.75f)
                  }
                }
                .fillMaxHeight(0.5f)
                .background(Color(0xFFFFFFFF))
                .border(width = Dp.Hairline, color = Color.Black)
          ) {
            Text(text = renderedModel.text, modifier = Modifier.align(Alignment.Center))
          }
        }
      }
    }
  }
}
