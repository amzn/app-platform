package software.amazon.app.platform.sample.backstack

import androidx.compose.foundation.combinedClickable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.sample.backstack.ChildPresenter.Model

@ContributesRenderer
class ChildRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model) {
    Text(
      text = model.text,
      fontSize = 24.sp,
      modifier =
        Modifier.combinedClickable(
          onClick = { model.onEvent(ChildPresenter.Event.Add) },
          onLongClick = { model.onEvent(ChildPresenter.Event.Remove) },
        ),
    )
  }
}
