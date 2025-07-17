package software.amazon.app.platform.recipes.template

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer

/**
 * A Compose renderer implementation for templates used in the recipes application.
 *
 * [rendererFactory] is used to get the [Renderer] for the [BaseModel] wrapped in the template.
 */
@Inject
@ContributesRenderer
class RootPresenterRenderer(private val rendererFactory: RendererFactory) :
  ComposeRenderer<RecipesAppTemplate>() {
  @Composable
  override fun Compose(model: RecipesAppTemplate) {
    when (model) {
      is RecipesAppTemplate.FullScreenTemplate -> FullScreen(model)
    }
  }

  @Composable
  private fun FullScreen(template: RecipesAppTemplate.FullScreenTemplate) {
    val renderer = rendererFactory.getComposeRenderer(template.model)
    renderer.renderCompose(template.model)
  }
}
