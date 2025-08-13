@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

data class EtaModel(val showAddress: Boolean, val showTime: Boolean) : BaseModel

@ContributesRenderer
class EtaRenderer : ComposeRenderer<EtaModel>() {
  @Composable
  override fun Compose(model: EtaModel) {
    withSharedTransitionScope {
      Column(
        modifier =
          Modifier.sharedElement(
              sharedContentState = rememberSharedContentState(key = BunnyLayerSharedElementKey.Eta),
              animatedVisibilityScope = requireAnimatedVisibilityScope(),
            )
            .fillMaxWidth()
      ) {
        if (model.showAddress) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF90D796))
                .border(width = Dp.Hairline, color = Color.Black)
          )
        }

        AnimatedVisibility(visible = model.showTime) {
          Column {
            if (model.showAddress) {
              Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(96.dp)
                  .background(Color(0xFFA0FFBE))
                  .border(width = Dp.Hairline, color = Color.Black)
            )
          }
        }
      }
    }
  }
}
