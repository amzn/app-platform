@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer

data object RearCameraModel : BaseModel

@ContributesRenderer
class RearCameraRenderer : ComposeRenderer<RearCameraModel>() {
  @Composable
  override fun Compose(model: RearCameraModel) {
    Box(modifier = Modifier.width(906.dp).height(510.dp).background(Color.Black))
  }
}
