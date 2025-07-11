package software.amazon.app.platform.template

import android.app.Application
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * The final Android app component. Note that [application] is an Android specific type and classes
 * living in the Android source folder can therefore inject [Application].
 *
 * [rootScopeProvider] is provided in the [AndroidAppComponent] and can be injected.
 */
@Component
@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class AndroidAppComponent(
    @get:Provides val application: Application,
    @get:Provides val rootScopeProvider: RootScopeProvider,
) : AndroidAppComponentMerged
