package software.amazon.app.platform.sample.template

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer

/**
 * A Compose renderer implementation for templates used in the sample application.
 *
 * [rendererFactory] is used to get the [Renderer] for the [BaseModel] wrapped in the template.
 */
@Inject
@ContributesRenderer
class ComposeSampleAppTemplateRenderer(private val rendererFactory: RendererFactory) :
  ComposeRenderer<SampleAppTemplate>() {

  @Composable
  override fun Compose(model: SampleAppTemplate) {
    when (model) {
      is SampleAppTemplate.FullScreenTemplate -> FullScreen(model)
      is SampleAppTemplate.ListDetailTemplate -> ListDetail(model)
    }
  }

  @Composable
  private fun FullScreen(template: SampleAppTemplate.FullScreenTemplate) {
    val renderer = rendererFactory.getComposeRenderer(template.model)
    renderer.renderCompose(template.model)
  }

  @Composable
  private fun ListDetail(template: SampleAppTemplate.ListDetailTemplate) {
    Row {
      Column(Modifier.weight(1f)) {
        rendererFactory.getComposeRenderer(template.list).renderCompose(template.list)
      }
      Column(Modifier.weight(2f)) {
        rendererFactory.getComposeRenderer(template.detail).renderCompose(template.detail)
      }
    }
  }
}
