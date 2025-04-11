package software.amazon.app.platform.sample

import android.app.Application
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/** kotlin-inject component that is used in instrumented tests. */
@Component
@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class TestAndroidAppComponent(
  @get:Provides val application: Application,
  @get:Provides val rootScopeProvider: RootScopeProvider,
) : TestAndroidAppComponentMerged
