package software.amazon.app.platform.presenter.metro

import dev.zacsweers.metro.Qualifier
import software.amazon.app.platform.scope.coroutine.metro.MainCoroutineDispatcher

/**
 * A qualifier to identify the coroutine scope used to run presenters. This scope is commonly
 * injected when converting a `Flow` to a `StateFlow`, see `stateInPresenter` for more details.
 *
 * This scope uses the [MainCoroutineDispatcher] by default, because presenters produce state for
 * the UI and computing their models should have the highest priority.
 *
 * Never cancel this scope yourself, otherwise the application comes to a halt.
 */
@Qualifier
public annotation class PresenterCoroutineScope
