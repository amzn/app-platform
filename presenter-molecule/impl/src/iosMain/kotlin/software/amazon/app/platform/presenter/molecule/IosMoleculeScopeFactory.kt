package software.amazon.app.platform.presenter.molecule

import app.cash.molecule.DisplayLinkClock
import app.cash.molecule.RecompositionMode
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.PresenterCoroutineScope
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Runs `MoleculePresenters` on the main thread provided by [PresenterCoroutineScope] and recomposes
 * only once per screen refresh when needed.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
public class IosMoleculeScopeFactory(
  @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
) :
  MoleculeScopeFactory by DefaultMoleculeScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    coroutineContext = DisplayLinkClock,
    recompositionMode = RecompositionMode.ContextClock,
  )
