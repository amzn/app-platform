package software.amazon.app.platform.recipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.backgesture.LocalBackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.amazon.app.platform.presenter.template.toTemplate
import software.amazon.app.platform.recipes.appbar.AppBarConfig
import software.amazon.app.platform.recipes.appbar.AppBarConfigModel
import software.amazon.app.platform.recipes.backstack.CrossSlideBackstackPresenter
import software.amazon.app.platform.recipes.landing.LandingPresenter
import software.amazon.app.platform.recipes.template.RecipesAppTemplate
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * A presenter that wraps any other presenter and turns the emitted models from the other presenter
 * into [RecipesAppTemplate]s.
 */
@Inject
class AndroidRootPresenter(
  private val savedInstanceStateRegistry: SavedInstanceStateRegistry,
  private val landingPresenter: LandingPresenter,
  private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
) : MoleculePresenter<Unit, RecipesAppTemplate> {

  init {
    val x = 1
  }

  @Composable
  override fun present(input: Unit): RecipesAppTemplate {
    Logger.i { "[jesslwan] Present in android root presenter"}

    return returningCompositionLocalProvider(
      LocalSavedInstanceStateRegistry provides savedInstanceStateRegistry,
      LocalBackGestureDispatcherPresenter provides backGestureDispatcherPresenter
    ) {
      val backstackPresenter = remember { CrossSlideBackstackPresenter(landingPresenter) }
      val backstackModel = backstackPresenter.present(Unit)

      backstackModelToTemplate(backstackModel)
    }
  }

  @Composable
  private fun backstackModelToTemplate(
    backstackModel: CrossSlideBackstackPresenter.Model
  ): RecipesAppTemplate {
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

//  /**
//   * This class requires an additional manually provided [MoleculePresenter] parameter to instantiate it.
//   * The rootPresenter will drive the models this presenter outputs as this class only
//   * provides some lightweight wrapper logic.
//   */
//  interface Factory {
//    /**
//     * Creates an instance of [TemplatePresenter] using the provided rootPresenter as a child.
//     */
//    fun create(rootPresenter: MoleculePresenter<Unit, *>): RootPresenter
//  }
//
//  @Inject
//  @ContributesBinding(AppScope::class)
//  class RealFactory(
//    private val templatePresenter: (MoleculePresenter<Unit, *>) -> RootPresenter,
//  ) : Factory {
//    override fun create(rootPresenter: MoleculePresenter<Unit, *>): RootPresenter {
//      return templatePresenter(rootPresenter)
//    }
//  }
}

