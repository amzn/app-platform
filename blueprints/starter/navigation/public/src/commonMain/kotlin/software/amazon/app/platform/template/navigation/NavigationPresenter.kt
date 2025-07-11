package software.amazon.app.platform.template.navigation

import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * A presenter that hosts other presenters and returns their models. For that reason this presenter
 * doesn't have its own [BaseModel] type and returns [BaseModel].
 */
interface NavigationPresenter : MoleculePresenter<Unit, BaseModel>
