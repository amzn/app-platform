package software.amazon.app.platform.scope.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/** Provides default dispatchers for Coroutine scopes. */
@ContributesTo(AppScope::class)
public interface CoroutineDispatcherComponent {
  /** Provides the IO dispatcher in the dependency graph. */
  @Provides
  @IoCoroutineDispatcher
  public fun provideIoCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.IO

  /** Provides the default dispatcher in the dependency graph. */
  @Provides
  @DefaultCoroutineDispatcher
  public fun provideDefaultCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.Default

  /** Provides the main dispatcher in the dependency graph. */
  @Provides
  @MainCoroutineDispatcher
  public fun provideMainCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}
