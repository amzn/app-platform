package software.amazon.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import dev.zacsweers.metro.AppScope as MetroAppScope
import dev.zacsweers.metro.ContributesTo as MetroContributesTo
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides as MetroProvides
import dev.zacsweers.metro.SingleIn as MetroSingleIn
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides as KiProvides
import software.amazon.app.platform.presenter.PresenterCoroutineScope
import software.amazon.lastmile.kotlin.inject.anvil.AppScope as KiAppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo as KiContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn as KiSingleIn

/**
 * Runs `MoleculePresenters` on the main thread provided by [PresenterCoroutineScope] and recomposes
 * as fast as possible.
 */
public class LinuxMoleculeScopeFactory(coroutineScopeFactory: () -> CoroutineScope) :
  MoleculeScopeFactory by DefaultMoleculeScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    recompositionMode = RecompositionMode.Immediate,
  )

/** Provides the [LinuxMoleculeScopeFactory] in the kotlin-inject graph. */
@KiContributesTo(KiAppScope::class)
public interface LinuxMoleculeScopeFactoryComponent {
  /** Provides the [LinuxMoleculeScopeFactory] in the kotlin-inject graph as a singleton. */
  @KiProvides
  @KiSingleIn(KiAppScope::class)
  public fun provideLinuxMoleculeScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): MoleculeScopeFactory = LinuxMoleculeScopeFactory(coroutineScopeFactory)
}

/** Provides the [LinuxMoleculeScopeFactory] in the Metro graph. */
@MetroContributesTo(MetroAppScope::class)
public interface LinuxMoleculeScopeFactoryGraph {
  /** Provides the [LinuxMoleculeScopeFactory] in the Metro graph as a singleton. */
  @MetroProvides
  @MetroSingleIn(MetroAppScope::class)
  public fun provideLinuxMoleculeScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: Provider<CoroutineScope>
  ): MoleculeScopeFactory = LinuxMoleculeScopeFactory { coroutineScopeFactory() }
}
