package software.amazon.app.platform.sample.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app_platform.sample.user.impl.generated.resources.Res
import app_platform.sample.user.impl.generated.resources.allDrawableResources
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.sample.user.UserPageDetailPresenter.Model

/** Renders the content for [UserPageDetailPresenter] on screen using Compose Multiplatform. */
@ContributesRenderer
class UserPageDetailRenderer : ComposeRenderer<Model>() {

  @OptIn(ExperimentalResourceApi::class)
  @Composable
  override fun Compose(model: Model) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
      LinearProgressIndicator(progress = model.timeoutProgress, modifier = Modifier.fillMaxWidth())

      Image(
        painter = painterResource(Res.allDrawableResources.getValue(model.pictureKey)),
        contentDescription = "Profile picture",
        modifier =
          Modifier.padding(start = 64.dp, top = 16.dp, end = 64.dp)
            .shadow(
              elevation = 16.dp,
              shape = CircleShape,
              clip = false,
              ambientColor = MaterialTheme.colors.primary,
              spotColor = MaterialTheme.colors.primary,
            )
            .clip(CircleShape) // clip to the circle shape
            .border(2.dp, MaterialTheme.colors.primary, CircleShape),
      )

      AnimatedContent(targetState = model.text) { text ->
        Text(
          text = text,
          style = MaterialTheme.typography.h6,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
      }
    }
  }
}
