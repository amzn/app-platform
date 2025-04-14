package software.amazon.app.platform.sample.template

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.amazon.app.platform.presenter.template.ModelDelegate
import software.amazon.app.platform.presenter.template.toTemplate
import software.amazon.app.platform.sample.template.SampleAppTemplatePresenter.Factory

/**
 * A presenter that wraps any other presenter and turns the emitted models from the other presenter
 * into [SampleAppTemplate]s.
 *
 * Inject [Factory] to create a new instance of [SampleAppTemplatePresenter].
 */
@Inject
class SampleAppTemplatePresenter(@Assisted private val rootPresenter: MoleculePresenter<Unit, *>) :
  MoleculePresenter<Unit, SampleAppTemplate> {
  @Composable
  override fun present(input: Unit): SampleAppTemplate {
    @Suppress("RemoveEmptyParenthesesFromLambdaCall")
    return returningCompositionLocalProvider(
      // Add local composition providers if needed.
    ) {
      rootPresenter.present(Unit).toTemplate<SampleAppTemplate> {
        SampleAppTemplate.FullScreenTemplate(it)
      }
    }
  }

  /**
   * A factory to instantiate a new [SampleAppTemplatePresenter] instance. This implementation hides
   * that assisted injection with kotlin-inject is used. It's easier to inject this [Factory] than
   * the lambda provided by kotlin-inject.
   */
  @Inject
  class Factory(private val factory: (MoleculePresenter<Unit, *>) -> SampleAppTemplatePresenter) {
    /**
     * Create a new [SampleAppTemplatePresenter]. The given [presenter] will be wrapped and its
     * models are transformed into a [SampleAppTemplate] with [SampleAppTemplate.FullScreenTemplate]
     * as default. The given [presenter] can override the template by either returning
     * [SampleAppTemplate] directly or making its [BaseModel] type implement [ModelDelegate].
     */
    fun createSampleAppTemplatePresenter(
      presenter: MoleculePresenter<Unit, *>
    ): SampleAppTemplatePresenter {
      return factory(presenter)
    }
  }
}
