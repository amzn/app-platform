package software.amazon.app.platform.recipes.template

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.template.toTemplate
import software.amazon.app.platform.recipes.appbar.AppBarConfig
import software.amazon.app.platform.recipes.appbar.AppBarConfigModel
import software.amazon.app.platform.recipes.backstack.CrossSlideBackstackPresenter
import software.amazon.app.platform.recipes.landing.LandingPresenter

/**
 * A presenter that wraps any other presenter and turns the emitted models from the other presenter
 * into [RecipesAppTemplate]s.
 */
@Inject
class RootPresenter(private val landingPresenter: LandingPresenter) :
  MoleculePresenter<Unit, RecipesAppTemplate> {
  @Composable
  override fun present(input: Unit): RecipesAppTemplate {
    val backstackPresenter = remember { CrossSlideBackstackPresenter(landingPresenter) }
    val backstackModel = backstackPresenter.present(Unit)

    val backstackScope = backstackModel.backstackScope
    val showBackArrow = backstackScope.lastBackstackChange.value.backstack.size > 1

    val backArrowAction =
      if (showBackArrow) {
        { backstackScope.pop() }
      } else {
        null
      }

    return backstackModel.toTemplate { model ->
      val appBarConfig =
        if (model is AppBarConfigModel) {
          model.appBarConfig().copy(backArrowAction = backArrowAction)
        } else {
          AppBarConfig(title = AppBarConfig.DEFAULT.title, backArrowAction = backArrowAction)
        }

      RecipesAppTemplate.FullScreenTemplate(model, appBarConfig)
    }
  }
}
