package software.amazon.app.platform.sample.backstack

import androidx.compose.runtime.Composable
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.sample.backstack.ChildPresenter.Model

class ChildPresenter(private val text: String) : MoleculePresenter<Unit, Model> {

  @Composable
  override fun present(input: Unit): Model {
    val backstackScope = checkNotNull(LocalBackstackScope.current)

    return Model(text) {
      when (it) {
        Event.Add ->
          backstackScope.push(
            ChildPresenter("From Child ${backstackScope.backstack.value.size + 1}")
          )
        Event.Remove -> backstackScope.pop()
      }
    }
  }

  data class Model(val text: String, val onEvent: (Event) -> Unit) : BaseModel

  sealed interface Event {
    data object Add : Event

    data object Remove : Event
  }
}
