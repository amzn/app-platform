package software.amazon.app.platform.template

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import software.amazon.app.platform.renderer.ComposeRendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.di.diComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/**
 * Responsible for creating the app component [component] and producing templates. Call [destroy] to
 * clean up any resources.
 *
 */
class DesktopApp(private val component: (RootScopeProvider) -> AppComponent) : RootScopeProvider {

  override val rootScope: Scope
    get() = application.rootScope

  private val application = Application().apply { create(component(this)) }

  private val templateProvider =
    rootScope.diComponent<Component>().templateProviderFactory.createTemplateProvider()

  /** Call this composable function to start rendering templates on the screen. */
  @Composable
  fun renderTemplates() {
    val template by templateProvider.templates.collectAsState()

    val factory = remember { ComposeRendererFactory(application) }

    val renderer = factory.getComposeRenderer(template)
    renderer.renderCompose(template)
  }

  /** Cancels and releases all resources. */
  fun destroy() {
    templateProvider.cancel()
    application.destroy()
  }

  /** Component interface to give us access to objects from the app component. */
  @ContributesTo(AppScope::class)
  interface Component {
    /** Gives access to the [TemplateProvider.Factory] from the object graph. */
    val templateProviderFactory: TemplateProvider.Factory
  }
}
