package software.amazon.app.platform.presenter.molecule

import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.CoroutineScope
import dev.zacsweers.metro.AppScope as MetroAppScope
import dev.zacsweers.metro.ContributesTo as MetroContributesTo
import dev.zacsweers.metro.Provides as MetroProvides
import dev.zacsweers.metro.SingleIn as MetroSingleIn
import me.tatarka.inject.annotations.Provides as KiProvides
import software.amazon.app.platform.presenter.PresenterCoroutineScope as KiPresenterCoroutineScope
import software.amazon.app.platform.presenter.metro.PresenterCoroutineScope as MetroPresenterCoroutineScope
import software.amazon.lastmile.kotlin.inject.anvil.AppScope as KiAppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo as KiContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn as KiSingleIn

/**
 * Runs `MoleculePresenters` on the main thread provided by [MetroPresenterCoroutineScope] and recomposes
 * only once per screen refresh when needed.
 */
public class AndroidMoleculeScopeFactory(
  coroutineScopeFactory: () -> CoroutineScope
) :
  MoleculeScopeFactory by DefaultMoleculeScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    coroutineContext = AndroidUiDispatcher.Main,
    recompositionMode = RecompositionMode.ContextClock,
  )

@KiContributesTo(KiAppScope::class)
public interface AndroidMoleculeScopeFactoryComponentKotlinInject {
  @KiProvides
  @KiSingleIn(KiAppScope::class) public fun provideAndroidMoleculeScopeFactory(
    @KiPresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
  ): MoleculeScopeFactory = AndroidMoleculeScopeFactory(coroutineScopeFactory)
}

@MetroContributesTo(MetroAppScope::class)
public interface AndroidMoleculeScopeFactoryComponentMetro {
  @MetroProvides
  @MetroSingleIn(MetroAppScope::class) public fun provideAndroidMoleculeScopeFactory(
    @MetroPresenterCoroutineScope coroutineScopeFactory: Provider<CoroutineScope>
  ): MoleculeScopeFactory = AndroidMoleculeScopeFactory { coroutineScopeFactory() }
}
