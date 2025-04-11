package software.amazon.app.platform.sample

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/** kotlin-inject component that is used in UI tests. */
@Component
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class TestDesktopAppComponent(@get:Provides val rootScopeProvider: RootScopeProvider) :
  TestDesktopAppComponentMerged
