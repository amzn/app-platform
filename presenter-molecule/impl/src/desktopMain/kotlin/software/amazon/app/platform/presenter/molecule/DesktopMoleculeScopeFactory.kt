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
public class DesktopMoleculeScopeFactory(coroutineScopeFactory: () -> CoroutineScope) :
  MoleculeScopeFactory by DefaultMoleculeScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    recompositionMode = RecompositionMode.Immediate,
  )

/** Provides the [DesktopMoleculeScopeFactory] in the kotlin-inject graph. */
@KiContributesTo(KiAppScope::class)
public interface DesktopMoleculeScopeFactoryComponent {
  /** Provides the [DesktopMoleculeScopeFactory] in the kotlin-inject graph as a singleton. */
  @KiProvides
  @KiSingleIn(KiAppScope::class)
  public fun provideDesktopMoleculeScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): MoleculeScopeFactory = DesktopMoleculeScopeFactory(coroutineScopeFactory)
}

/** Provides the [DesktopMoleculeScopeFactory] in the Metro graph. */
@MetroContributesTo(MetroAppScope::class)
public interface DesktopMoleculeScopeFactoryGraph {
  /** Provides the [DesktopMoleculeScopeFactory] in the Metro graph as a singleton. */
  @MetroProvides
  @MetroSingleIn(MetroAppScope::class)
  public fun provideDesktopMoleculeScopeFactory(
    @PresenterCoroutineScope coroutineScopeFactory: Provider<CoroutineScope>
  ): MoleculeScopeFactory = DesktopMoleculeScopeFactory { coroutineScopeFactory() }
}
