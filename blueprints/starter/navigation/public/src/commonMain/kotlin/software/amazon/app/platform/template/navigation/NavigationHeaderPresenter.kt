package software.amazon.app.platform.template.navigation

import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter

/**
 * Presenter responsible for the state of the top navigation bar (header).
 *
 * This typically controls high-level UI elements such as titles, toggle buttons,
 * or contextual actions that affect the overall screen.
 */
interface NavigationHeaderPresenter : MoleculePresenter<Unit, NavigationHeaderPresenter.Model> {
    data class Model(
        val exampleBoolean: Boolean,
    ) : BaseModel
}
