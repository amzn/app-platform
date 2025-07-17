package software.amazon.app.platform.recipes.template

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.amazon.app.platform.presenter.template.toTemplate
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
    @Suppress("RemoveEmptyParenthesesFromLambdaCall")
    return returningCompositionLocalProvider(
      // Add local composition providers if needed.
    ) {
      landingPresenter.present(Unit).toTemplate<RecipesAppTemplate> {
        RecipesAppTemplate.FullScreenTemplate(it)
      }
    }
  }
}
