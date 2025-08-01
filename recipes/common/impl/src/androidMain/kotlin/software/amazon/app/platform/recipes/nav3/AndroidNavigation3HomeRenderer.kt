package software.amazon.app.platform.recipes.nav3

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.nav3.Navigation3HomePresenter.Event
import software.amazon.app.platform.recipes.nav3.Navigation3HomePresenter.Model
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer

/**
 * Navigation3 is only supported on Android, therefore this renderer lives in `androidMain`. This
 * Renderer integrates the Navigation3 library. The backstack is managed in the presenter for
 * idiomatic navigation with presenters, but interactions with the backstack are handled by the
 * Navigation3 library, e.g. back gestures.
 */
@Inject
@ContributesRenderer
class AndroidNavigation3HomeRenderer(private val rendererFactory: RendererFactory) :
  ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model) {
    // Use the position of the model in the backstack as key for `NavDisplay`. This way
    // we can update models without Navigation 3 treating those changes as a new screen.
    val backstack = model.backstack.mapIndexed { index, _ -> index }

    NavDisplay(
      backStack = backstack,
      onBack = { model.onEvent(Event.Pop) },
      entryProvider = { key ->
        NavEntry(key) {
          val model = model.backstack[it]
          rendererFactory.getComposeRenderer(model).renderCompose(model)
        }
      },
    )
  }
}
