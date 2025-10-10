package software.amazon.app.platform.recipes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.presenter.molecule.MoleculeScopeFactory
import software.amazon.app.platform.presenter.molecule.launchMoleculePresenter
import software.amazon.app.platform.recipes.template.RecipesAppTemplate
import software.amazon.app.platform.recipes.template.RootPresenter
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.di.kotlinInjectComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.reflect.KClass

/**
 * `ViewModel` that hosts the stream of templates and survives configuration changes.
 */
@Inject
class MainActivityViewModel(
  rootPresenter: AndroidRootPresenter,
  moleculeScopeFactory: MoleculeScopeFactory,
) : ViewModel() {

  private val moleculeScope by lazy {
    moleculeScopeFactory.createMoleculeScope()
  }

  val templates: StateFlow<RecipesAppTemplate> by lazy {
    moleculeScope.launchMoleculePresenter(
      presenter = rootPresenter,
      input = Unit,
    ).model
  }

  override fun onCleared() {
    Logger.i { "[jesslwan] Clearing view model"}
    moleculeScope.cancel()
  }

  /**
   * Provides the binding to instantiate the view model via our factory.
   */
  @ContributesTo(ViewModelScope::class)
  interface Component {
    /**
     * Provides the binding to instantiate the view model via our factory.
     */
    @Provides
    @IntoMap
    fun bindMainViewModel(viewModel: () -> MainActivityViewModel): Pair<KClass<out ViewModel>, () -> ViewModel> = MainActivityViewModel::class to viewModel
  }
}
