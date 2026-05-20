@file:OptIn(ExperimentalAppPlatform::class)

package software.amazon.app.platform.recipes.nav3

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.ExperimentalAppPlatform
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.backstack.nav3.PresenterBackstackRenderer
import software.amazon.app.platform.recipes.nav3.Navigation3HomePresenter.Model
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer

/** Renderer that integrates the presenter backstack with Navigation 3. */
@Inject
@ContributesRenderer
class Navigation3HomeRenderer(private val rendererFactory: RendererFactory) :
  PresenterBackstackRenderer<Model>() {
  @Composable
  override fun ComposeBackstackEntry(model: BaseModel) {
    rendererFactory.getComposeRenderer(model).renderCompose(model)
  }
}
