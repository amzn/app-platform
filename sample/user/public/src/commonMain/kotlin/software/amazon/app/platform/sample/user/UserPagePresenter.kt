package software.amazon.app.platform.sample.user

import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.template.ModelDelegate
import software.amazon.app.platform.sample.user.UserPagePresenter.Model

/** Presenter to render user details on screen. */
interface UserPagePresenter : MoleculePresenter<Unit, Model> {

  /**
   * The state of the user page. Note that the actual implementation class implements
   * [ModelDelegate] to override which `SampleAppTemplate` to use. This Model hosts to other models
   * [listModel] and [detailModel], which will be produced by child presenters.
   *
   * This class is an interface and not the final `data class`, because the actual implementation
   * contains more logic, which was moved therefore into the :impl module.
   */
  interface Model : BaseModel {
    val listModel: BaseModel
    val detailModel: BaseModel
  }
}
