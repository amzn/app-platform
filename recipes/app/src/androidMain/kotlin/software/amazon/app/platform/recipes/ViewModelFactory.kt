package software.amazon.app.platform.recipes

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import software.amazon.app.platform.scope.Scope
import kotlin.reflect.KClass
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scoped
import software.amazon.app.platform.scope.coroutine.CoroutineScopeScoped
import software.amazon.app.platform.scope.coroutine.IoCoroutineDispatcher
import software.amazon.app.platform.scope.di.kotlinInjectComponent
import software.amazon.app.platform.scope.register
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import software.amazon.app.platform.scope.coroutine.addCoroutineScopeScoped
import software.amazon.app.platform.scope.di.addKotlinInjectComponent
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

class ViewModelFactory(
  application: Application,
  scopeName: String,
) : ViewModelProvider.Factory, AndroidViewModel(application) {

  private val scope: Scope
  private val viewModels: Map<KClass<out ViewModel>, () -> ViewModel>

  init {
    val rootScope = application.rootScope

    scope = rootScope.buildChild(scopeName) {
      val component = rootScope
        .kotlinInjectComponent<ViewModelComponent.Factory>()
        .createViewModelComponent()

      addKotlinInjectComponent(component)
      addCoroutineScopeScoped(component.coroutineScopeScoped)
    }
    scope.register(scope.kotlinInjectComponent<ViewModelComponent>().viewModelScopedInstances)

    viewModels = scope.kotlinInjectComponent<ViewModelComponent>().viewModels
  }

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    @Suppress("UNCHECKED_CAST")
    return viewModels.getValue(modelClass.kotlin).invoke() as T
  }

  override fun onCleared() {
    scope.destroy()
  }

  companion object {
    fun factory(scopeName: String): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        ViewModelFactory(requireNotNull(this[APPLICATION_KEY]), scopeName)
      }
    }
  }
}

abstract class ViewModelScope private constructor()

@ContributesSubcomponent(ViewModelScope::class)
@SingleIn(ViewModelScope::class)
interface ViewModelComponent {
  /**
   * The factory to instantiate the ViewModelComponent.
   */
  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createViewModelComponent(): ViewModelComponent
  }

  /**
   * Returns all view models with their class as key.
   */
  val viewModels: Map<KClass<out ViewModel>, () -> ViewModel>

  /**
   * All [Scoped] instances in the view model scope.
   */
  @ForScope(ViewModelScope::class)
  val viewModelScopedInstances: Set<Scoped>

  /**
   * Provides the [CoroutineScopeScoped] for the ViewModel scope.
   */
  @ForScope(ViewModelScope::class)
  val coroutineScopeScoped: CoroutineScopeScoped
}

/**
 * Convenience function to receive the root scope from the application class. It assumes that
 * the application class implements [RootScopeProvider].
 *
 * Use this getter sparsely and rely on constructor injection instead.
 */
val Application.rootScope: Scope get() = rootScopeProvider.rootScope

/**
 * Convenience function to receive the [RootScopeProvider]. It assumes that the
 * application class implements [RootScopeProvider].
 */
val Context.rootScopeProvider: RootScopeProvider get() = applicationContext as RootScopeProvider

@ContributesTo(ViewModelScope::class)
interface ViewModelCoroutineScopeComponent {

  /**
   * Provides the singleton [CoroutineScopeScoped] instance for this scope.
   */
  @Provides
  @ForScope(ViewModelScope::class)
  @SingleIn(ViewModelScope::class)
  fun provideCoroutineScopeScoped(@IoCoroutineDispatcher dispatcher: CoroutineDispatcher): CoroutineScopeScoped =
    CoroutineScopeScoped(dispatcher + SupervisorJob() + CoroutineName("view-model-scope"))

  /**
   * Creates a new [CoroutineScope] every time one is injected.
   */
  @Provides
  @ForScope(ViewModelScope::class)
  fun provideCoroutineScope(@ForScope(ViewModelScope::class) coroutineScopeScoped: CoroutineScopeScoped): CoroutineScope = coroutineScopeScoped.createChild()

  /**
   * An empty binding for [Scoped].
   */
  @Provides
  @IntoSet
  @ForScope(ViewModelScope::class)
  fun provideEmptyScoped(): Scoped = object : Scoped {}
}
