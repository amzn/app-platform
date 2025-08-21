package software.amazon.app.platform.recipes

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import software.amazon.app.platform.scope.RootScopeProvider

/**
 * The final Android app component. Note that `application` is an Android specific type and classes
 * living in the Android source folder can therefore inject [Application].
 *
 * `rootScopeProvider` is provided in the [AndroidAppComponent] and can be injected.
 */
@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
interface AndroidAppComponent {
  @DependencyGraph.Factory
  fun interface Factory {
    fun create(
      @Provides application: Application,
      @Provides rootScopeProvider: RootScopeProvider,
    ): AndroidAppComponent
  }
}
