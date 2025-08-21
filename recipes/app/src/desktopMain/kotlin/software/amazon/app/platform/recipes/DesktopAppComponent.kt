package software.amazon.app.platform.recipes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import software.amazon.app.platform.scope.RootScopeProvider

/**
 * The final Desktop app component. Unlike the Android and iOS specific counterpart, this class
 * doesn't have any platform specific types.
 *
 * [rootScopeProvider] is provided in the [DesktopAppComponent] and can be injected.
 */
@DependencyGraph(scope = AppScope::class)
//@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
interface DesktopAppComponent
//(@get:Provides val rootScopeProvider: RootScopeProvider)
//  :
//  DesktopAppComponentMerged{
{
  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides rootScopeProvider: RootScopeProvider): DesktopAppComponent
  }
}
