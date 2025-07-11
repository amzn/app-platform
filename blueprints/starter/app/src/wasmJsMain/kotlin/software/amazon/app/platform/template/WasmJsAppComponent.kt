package software.amazon.app.platform.template

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * The final Wasm app component. Unlike the Android and iOS specific counterpart, this class doesn't
 * have any platform specific types.
 *
 * [rootScopeProvider] is provided in the [WasmJsAppComponent] and can be injected.
 */
@Component
@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class WasmJsAppComponent(
    @get:Provides val rootScopeProvider: RootScopeProvider,
) :
    WasmJsAppComponentMerged {
    abstract val templateProviderFactory: TemplateProvider.Factory
}
