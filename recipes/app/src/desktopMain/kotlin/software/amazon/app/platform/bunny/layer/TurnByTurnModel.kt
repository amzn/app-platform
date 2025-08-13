@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny.layer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

data class TurnByTurnModel(val showAddress: Boolean) : BaseModel

@ContributesRenderer
class TurnByTurnRenderer : ComposeRenderer<TurnByTurnModel>() {
  @Composable
  override fun Compose(model: TurnByTurnModel) {
    withSharedTransitionScope {
      Column(
        modifier =
          Modifier.sharedElement(
              sharedContentState =
                rememberSharedContentState(key = BunnyLayerSharedElementKey.TurnByTurn),
              animatedVisibilityScope = requireAnimatedVisibilityScope(),
            )
            .fillMaxWidth()
      ) {
        AnimatedVisibility(visible = model.showAddress) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFF65D2F7))
                .border(width = Dp.Hairline, color = Color.Black)
          )
        }

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(120.dp)
              .background(Color(0xFF9DE3FB))
              .border(width = Dp.Hairline, color = Color.Black)
        )
      }
    }
  }
}
