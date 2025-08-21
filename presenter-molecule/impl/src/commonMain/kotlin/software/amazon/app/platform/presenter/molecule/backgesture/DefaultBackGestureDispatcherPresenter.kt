package software.amazon.app.platform.presenter.molecule.backgesture

import dev.zacsweers.metro.AppScope as MetroAppScope
import dev.zacsweers.metro.ContributesBinding as MetroContributesBinding
import dev.zacsweers.metro.Inject as MetroInject
import dev.zacsweers.metro.SingleIn as MetroSingleIn
import me.tatarka.inject.annotations.Inject as KiInject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope as KiAppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding as KiContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn as KiSingleIn

/**
 * Implementation of [BackGestureDispatcherPresenter] that maintains a list of registered back
 * gesture listeners and forwards events to the last registered callback. See
 * [BackGestureDispatcherPresenter] for more details.
 */
@KiInject
@KiSingleIn(KiAppScope::class)
@KiContributesBinding(KiAppScope::class)
public class DefaultBackGestureDispatcherPresenter :
  BackGestureDispatcherPresenter by BackGestureDispatcherPresenter.createNewInstance()

@MetroInject
@MetroSingleIn(MetroAppScope::class)
@MetroContributesBinding(MetroAppScope::class)
public class DefaultBackGestureDispatcherPresenterMolecule :
  BackGestureDispatcherPresenter by BackGestureDispatcherPresenter.createNewInstance()
