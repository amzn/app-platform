package software.amazon.app.platform.template

import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.scope.Scoped
import software.amazon.app.platform.scope.coroutine.CoroutineScopeScoped
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Shared interface for the app component. The final components live in the platform specific source
 * folders in order to have access to platform specific code.
 */
@ContributesTo(AppScope::class)
@SingleIn(AppScope::class)
interface AppComponent {
    /** All [Scoped] instances part of the app scope. */
    @ForScope(AppScope::class)
    val appScopedInstances: Set<Scoped>

    /** The coroutine scope that runs as long as the app scope is alive. */
    @ForScope(AppScope::class)
    val appScopeCoroutineScopeScoped: CoroutineScopeScoped

    /**
     * Provide at least one implementation in the scope, otherwise kotlin-inject will complain. The
     * template app actually does have a [Scoped] instance (ExampleValueGenerator) in the app scope,
     * so the following is not needed, but will provide here since once ExampleValueGenerator is removed
     * kotlin-inject will complain until replaces with another [Scoped] instance.
     */
    @Provides @IntoSet
    @ForScope(AppScope::class)
    fun provideEmptyScoped(): Scoped = Scoped.NO_OP
}
