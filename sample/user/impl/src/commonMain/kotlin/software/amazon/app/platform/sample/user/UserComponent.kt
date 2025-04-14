package software.amazon.app.platform.sample.user

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.scope.Scoped
import software.amazon.app.platform.scope.coroutine.CoroutineScopeScoped
import software.amazon.app.platform.scope.coroutine.IoCoroutineDispatcher
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * The kotlin-inject component for the user scope. This is a subcomponent of the AppScope component.
 */
@ContributesSubcomponent(UserScope::class)
@SingleIn(UserScope::class)
interface UserComponent {
  /**
   * The factory instantiates a new instance of [UserComponent]. This interface will be implemented
   * by the AppScope component.
   */
  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    /**
     * Creates a new instance of [UserComponent]. The provided [user] argument will be added to the
     * component and the [User] can be injected in the classes part of the [UserScope].
     */
    fun createUserComponent(user: User): UserComponent
  }

  /** All [Scoped] instances part of the user scope. */
  @ForScope(UserScope::class) val userScopedInstances: Set<Scoped>

  /** The coroutine scope that runs as long as the user scope is alive. */
  @ForScope(UserScope::class) val userScopeCoroutineScopeScoped: CoroutineScopeScoped

  /**
   * Provides the [CoroutineScopeScoped] for the user scope. This is a single instance for the user
   * scope.
   */
  @Provides
  @SingleIn(UserScope::class)
  @ForScope(UserScope::class)
  fun provideUserScopeCoroutineScopeScoped(
    @IoCoroutineDispatcher dispatcher: CoroutineDispatcher
  ): CoroutineScopeScoped {
    return CoroutineScopeScoped(dispatcher + SupervisorJob() + CoroutineName("UserScope"))
  }

  /**
   * Provides the [CoroutineScope] for the user scope. A new child scope is created every time an
   * instance is injected so that the parent cannot be canceled accidentally.
   */
  @Provides
  @ForScope(UserScope::class)
  fun provideUserCoroutineScope(
    @ForScope(UserScope::class) userScopeCoroutineScopeScoped: CoroutineScopeScoped
  ): CoroutineScope {
    return userScopeCoroutineScopeScoped.createChild()
  }
}
