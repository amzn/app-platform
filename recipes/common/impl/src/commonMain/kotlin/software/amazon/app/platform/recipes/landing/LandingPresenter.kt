package software.amazon.app.platform.recipes.landing

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.recipes.appbar.menu.MenuPresenter
import software.amazon.app.platform.recipes.backstack.LocalBackstackScope
import software.amazon.app.platform.recipes.backstack.presenter.BackstackChildPresenter
import software.amazon.app.platform.recipes.landing.LandingPresenter.Model
import software.amazon.app.platform.recipes.nav3.Navigation3HomePresenter

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

        Event.MenuPresenter -> {
          backstack.push(MenuPresenter())
        }

        Event.Navigation3 -> {
          backstack.push(Navigation3HomePresenter())
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

    /** Show the presenter with a custom App Bar menu. */
    data object MenuPresenter : Event

    /** Show the presenter highlighting navigation3 integration. */
    data object Navigation3 : Event
  }
}
