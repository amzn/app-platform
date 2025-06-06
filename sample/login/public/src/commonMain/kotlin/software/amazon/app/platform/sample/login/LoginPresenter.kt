package software.amazon.app.platform.sample.login

import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.template.ModelDelegate

/** A presenter to render the login screen. */
interface LoginPresenter : MoleculePresenter<Unit, LoginPresenter.Model> {
  /** The state of the login screen. */
  sealed interface Model : BaseModel {
    data class LoginScreen(
      /** Whether login is currently in progress. */
      val loginInProgress: Boolean,

      /** Callback to send events back to the presenter. */
      val onEvent: (Event) -> Unit,
    ) : Model

    data class ChildScreen(val delegatedModel: BaseModel) : Model, ModelDelegate {
      override fun delegate(): BaseModel = delegatedModel
    }
  }

  /** All events that [LoginPresenter] can process. */
  sealed interface Event {
    /** Sent when the user presses the login button with the entered [userName]. */
    data class Login(val userName: String) : Event

    data object Backstack : Event
  }
}
