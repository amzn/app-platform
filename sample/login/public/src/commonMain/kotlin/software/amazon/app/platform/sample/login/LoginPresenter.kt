package software.amazon.app.platform.sample.login

import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.scope.Scoped
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/** A presenter to render the login screen. */
interface LoginPresenter : MoleculePresenter<Unit, LoginPresenter.Model> {
  /** The state of the login screen. */
  data class Model(
    /** Whether login is currently in progress. */
    val loginInProgress: Boolean,

    /** Callback to send events back to the presenter. */
    val onEvent: (Event) -> Unit,
  ) : BaseModel

  /** All events that [LoginPresenter] can process. */
  sealed interface Event {
    /** Sent when the user presses the login button with the entered [userName]. */
    data class Login(val userName: String) : Event
  }
}

interface LocationProvider

@Inject // (1)!
@SingleIn(AppScope::class) // (2)!
@ContributesBinding(AppScope::class) //(3)!
class AndroidLocationProvider(
) : LocationProvider, Scoped
