package software.amazon.app.platform.sample.backstack

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

class BackstackScope(initial: MoleculePresenter<Unit, out BaseModel>) {
  private var _backstack = mutableStateOf(listOf(initial))
  val backstack: State<List<MoleculePresenter<Unit, out BaseModel>>> = _backstack

  private var _forward = mutableStateOf(false)
  val forward: State<Boolean> = _forward

  fun push(presenter: MoleculePresenter<Unit, out BaseModel>) {
    _backstack.value = backstack.value + presenter
    _forward.value = true
  }

  fun pop() {
    _backstack.value =
      if (backstack.value.isEmpty()) emptyList()
      else backstack.value.subList(0, backstack.value.size - 1)
    _forward.value = false
  }
}

val LocalBackstackScope = compositionLocalOf<BackstackScope?> { null }
