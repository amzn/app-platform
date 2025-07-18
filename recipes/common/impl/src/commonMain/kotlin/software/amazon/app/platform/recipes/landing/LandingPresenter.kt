package software.amazon.app.platform.recipes.landing

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.recipes.backstack.LocalBackstackScope
import software.amazon.app.platform.recipes.backstack.presenter.BackstackChildPresenter
import software.amazon.app.platform.recipes.landing.LandingPresenter.Model

/** The presenter that is responsible to show the content of the landing page in the Recipes app. */
@Inject
class LandingPresenter : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    val backstack = checkNotNull(LocalBackstackScope.current)

    return Model {
      when (it) {
        Event.AddPresenterToBackstack -> {
          backstack.push(BackstackChildPresenter(0))
        }
      }
    }
  }

  /** The state of the landing screen. */
  data class Model(
    /** Callback to send events back to the presenter. */
    val onEvent: (Event) -> Unit
  ) : BaseModel

  /** All events that [LandingPresenter] can process. */
  sealed interface Event {
    /** Add a new presenter to the backstack. */
    data object AddPresenterToBackstack : Event
  }
}
