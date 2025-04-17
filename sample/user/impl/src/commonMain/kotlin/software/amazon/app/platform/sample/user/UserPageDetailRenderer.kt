package software.amazon.app.platform.sample.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.sample.user.UserPageDetailPresenter.Model

/** Renders the content for [UserPageDetailPresenter] on screen using Compose Multiplatform. */
@ContributesRenderer
class UserPageDetailRenderer : ComposeRenderer<Model>() {

  @Composable
  override fun Compose(model: Model) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
      LinearProgressIndicator(progress = model.timeoutProgress, modifier = Modifier.fillMaxWidth())

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
