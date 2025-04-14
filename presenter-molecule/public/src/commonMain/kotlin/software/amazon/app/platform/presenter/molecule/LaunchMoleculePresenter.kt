package software.amazon.app.platform.presenter.molecule

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.Presenter

/**
 * Launch a coroutine into this [CoroutineScope] which will continually recompose
 * [MoleculePresenter.present] to produce a [StateFlow]. The [StateFlow] will be provided by the
 * returned [Presenter].
 */
public fun <InputT : Any, ModelT : BaseModel> CoroutineScope.launchMoleculePresenter(
  presenter: MoleculePresenter<InputT, ModelT>,
  input: StateFlow<InputT>,
  recompositionMode: RecompositionMode,
): Presenter<ModelT> {
  return object : Presenter<ModelT> {
    override val model: StateFlow<ModelT> =
      launchMolecule(recompositionMode) {
        val inputElement by input.collectAsState()
        presenter.present(inputElement)
      }
  }
}

/**
 * Launch a coroutine into this [MoleculeScope] which will continually recompose
 * [MoleculePresenter.present] to produce a [StateFlow]. The [StateFlow] will be provided by the
 * returned [Presenter].
 */
public fun <InputT : Any, ModelT : BaseModel> MoleculeScope.launchMoleculePresenter(
  presenter: MoleculePresenter<InputT, ModelT>,
  input: StateFlow<InputT>,
): Presenter<ModelT> =
  coroutineScope.launchMoleculePresenter(
    presenter = presenter,
    input = input,
    recompositionMode = recompositionMode,
  )

/**
 * Launch a coroutine into this [MoleculeScope] which will continually recompose
 * [MoleculePresenter.present] to produce a [StateFlow]. The [StateFlow] will be provided by the
 * returned [Presenter].
 */
public fun <InputT : Any, ModelT : BaseModel> MoleculeScope.launchMoleculePresenter(
  presenter: MoleculePresenter<InputT, ModelT>,
  input: InputT,
): Presenter<ModelT> =
  launchMoleculePresenter(presenter = presenter, input = MutableStateFlow(input))
