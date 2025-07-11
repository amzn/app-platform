package software.amazon.app.platform.template

import me.tatarka.inject.annotations.Provides
import platform.UIKit.UIApplication
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.KClass

/**
 * The final iOS app component. Note that [uiApplication] is an iOS specific type and classes living
 * in the iOS source folder can therefore inject [UIApplication].
 *
 * [rootScopeProvider] is provided in the [IosAppComponent] and can be injected.
 */
@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class IosAppComponent(
    @get:Provides val uiApplication: UIApplication,
    @get:Provides val rootScopeProvider: RootScopeProvider,
) {
    abstract val templateProviderFactory: TemplateProvider.Factory
}

/**
 * Factory function to instantiate the component. This is necessary, because `iosMain` is a shared
 * source folder and generated components live in the x64, arm64 and simulatorArm64 source folders.
 */
@MergeComponent.CreateComponent
expect fun KClass<IosAppComponent>.createComponent(
    uiApplication: UIApplication,
    rootScopeProvider: RootScopeProvider,
): IosAppComponent

/** This function is called from Swift to create a new component instance. */
@Suppress("unused")
fun createIosAppComponent(
    application: UIApplication,
    rootScopeProvider: RootScopeProvider,
): AppComponent {
    return IosAppComponent::class.createComponent(application, rootScopeProvider) as AppComponent
}
