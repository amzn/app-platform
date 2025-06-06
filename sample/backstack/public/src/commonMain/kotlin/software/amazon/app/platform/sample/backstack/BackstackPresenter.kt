package software.amazon.app.platform.sample.backstack

import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/** A presenter to render a backstack of presenters. */
interface BackstackPresenter : MoleculePresenter<Unit, BackstackPresenter.Model> {
  /** The state of the backstack presenter. */
  sealed interface Model : BaseModel {
    data class ShowBackstack(
      val childModel: BaseModel,
      val forward: Boolean,

      /** Callback to send events back to the presenter. */
      val onEvent: (Event) -> Unit,
    ) : Model

    data object Done : Model
  }

  /** All events that [BackstackPresenter] can process. */
  sealed interface Event {
    data object Add : Event

    data object Remove : Event
  }
}
