@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.bunny.LocalWindowSize
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer

data class BaseContainerModel(val template: String) : BaseModel

@ContributesRenderer
class BaseContainerRenderer : ComposeRenderer<BaseContainerModel>() {
  @Composable
  override fun Compose(model: BaseContainerModel) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F4EA))) {
      with(LocalDensity.current) {
        val containerSize = LocalWindowInfo.current.containerSize

        Text(
          text =
            "Template: ${model.template}  " +
              "Size: ${LocalWindowSize.current}  " +
              "Pixel: ${containerSize.width}x${containerSize.height}  " +
              "DP: ${containerSize.width.toDp().value.toInt()}x${containerSize.height.toDp().value.toInt()}",
          modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
        )
      }
    }
  }
}
