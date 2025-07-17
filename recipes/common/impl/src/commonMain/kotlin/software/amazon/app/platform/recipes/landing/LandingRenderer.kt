package software.amazon.app.platform.recipes.landing

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.landing.LandingPresenter.Model
import software.amazon.app.platform.renderer.ComposeRenderer

/** Renders the content for [LandingPresenter] on screen. */
@ContributesRenderer
class LandingRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model) {
    Text("Hello")
  }
}
