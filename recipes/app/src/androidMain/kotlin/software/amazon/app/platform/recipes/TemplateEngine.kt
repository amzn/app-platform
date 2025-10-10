package software.amazon.app.platform.recipes

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.renderer.RendererFactory
import me.tatarka.inject.annotations.Assisted
import software.amazon.app.platform.recipes.template.RecipesAppTemplate
import software.amazon.app.platform.renderer.getRenderer
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * The root implementation for rendering models within an Activity or Fragment. This takes a
 * [RendererFactory] via an assisted factory to make this class DI friendly.
 *
 * The [take] method serves as the ingress point for consuming a StateFlow of template models
 * from the root presenter.
 */
@Inject
class TemplateEngine(
  @Assisted private val factory: RendererFactory,
) {
  /**
   * The ingress point for consuming a StateFlow of templates.
   * @param templates a StateFlow<Template> output from a [Presenter]
   * @param lifecycle the lifecycle of the activity, used for consuming a StateFlow
   * @param repeatOnLifecycle state in which to begin coroutine, default is [STARTED]
   */
  fun take(
    templates: StateFlow<RecipesAppTemplate>,
    lifecycle: Lifecycle,
    repeatOnLifecycle: Lifecycle.State = Lifecycle.State.STARTED,
  ) {
    val rootRenderer = factory.getRenderer(templates.value)

    lifecycle.coroutineScope.launch {
      lifecycle.repeatOnLifecycle(repeatOnLifecycle) {
        templates.collect {
          Logger.i { "[jesslwan] rendering: $it"}
          rootRenderer.render(it)
        }
      }
    }
  }

  /**
   * An AssistedFactory so this class can be created with additional dependencies.
   */
  interface Factory {
    /**
     * A Factory method for instantiating this class with a [RendererFactory].
     */
    fun create(factory: RendererFactory): TemplateEngine
  }

  @Inject
  @ContributesBinding(AppScope::class)
  class RealFactory(
    private val templateEngine: (RendererFactory) -> TemplateEngine,
  ) : Factory {
    override fun create(factory: RendererFactory): TemplateEngine {
      return templateEngine(factory)
    }
  }
}
