package software.amazon.app.platform.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import software.amazon.app.platform.sample.navigation.NavigationPresenter
import software.amazon.app.platform.sample.template.SampleAppTemplate
import software.amazon.app.platform.sample.template.SampleAppTemplatePresenter
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.di.diComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/**
 * `ViewModel` that hosts the stream of templates and survives configuration changes. Note that we
 * use [application] to get access to the root scope.
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

  private val component = (application as RootScopeProvider).rootScope.diComponent<Component>()
  private val templateProvider = component.templateProviderFactory.createTemplateProvider()

  /** The stream of templates that are rendered by [MainActivity]. */
  val templates: StateFlow<SampleAppTemplate> = templateProvider.templates

  val factory = component.factory
  val navigationPresenter = component.navigationPresenter

  override fun onCleared() {
    templateProvider.cancel()
  }

  /** Component interface to give us access to objects from the app component. */
  @ContributesTo(AppScope::class)
  interface Component {
    /** Gives access to the [TemplateProvider.Factory] from the object graph. */
    val templateProviderFactory: TemplateProvider.Factory

    val factory : SampleAppTemplatePresenter.Factory
    val navigationPresenter : NavigationPresenter
  }
}
