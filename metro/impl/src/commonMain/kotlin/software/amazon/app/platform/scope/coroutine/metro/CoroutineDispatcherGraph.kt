package software.amazon.app.platform.scope.coroutine.metro

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import software.amazon.app.platform.scope.coroutine.DefaultCoroutineDispatcher
import software.amazon.app.platform.scope.coroutine.IoCoroutineDispatcher
import software.amazon.app.platform.scope.coroutine.MainCoroutineDispatcher

/** Provides default dispatchers for coroutine scopes. */
@ContributesTo(AppScope::class)
public interface CoroutineDispatcherGraph {
  /** Provides the IO dispatcher in the dependency graph. */
  @Provides
  @IoCoroutineDispatcher
  public fun provideIoCoroutineDispatcher(): CoroutineDispatcher = ioDispatcher

  /** Provides the default dispatcher in the dependency graph. */
  @Provides
  @DefaultCoroutineDispatcher
  public fun provideDefaultCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.Default

  /** Provides the main dispatcher in the dependency graph. */
  @Provides
  @MainCoroutineDispatcher
  public fun provideMainCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}
