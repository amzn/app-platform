package software.amazon.app.platform.template.templates

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.amazon.app.platform.presenter.template.ModelDelegate
import software.amazon.app.platform.presenter.template.toTemplate

/**
 * A presenter that wraps any other presenter and turns the emitted models from the other presenter
 * into [AppTemplate]s.
 *
 * Inject [Factory] to create a new instance of [AppTemplatePresenter].
 */
@Inject
class AppTemplatePresenter(
    @Assisted private val rootPresenter: MoleculePresenter<Unit, *>,
) :
    MoleculePresenter<Unit, AppTemplate> {
    @Composable
    override fun present(input: Unit): AppTemplate {
        @Suppress("RemoveEmptyParenthesesFromLambdaCall")
        return returningCompositionLocalProvider(
            // Add local composition providers if needed.
        ) {
            rootPresenter.present(Unit).toTemplate<AppTemplate> {
                AppTemplate.FullScreenTemplate(it)
            }
        }
    }

    /**
     * A factory to instantiate a new [AppTemplatePresenter] instance. This implementation hides
     * that assisted injection with kotlin-inject is used. It's easier to inject this [Factory] than
     * the lambda provided by kotlin-inject.
     */
    @Inject
    class Factory(private val factory: (MoleculePresenter<Unit, *>) -> AppTemplatePresenter) {
        /**
         * Create a new [AppTemplatePresenter]. The given [presenter] will be wrapped and its
         * models are transformed into a [AppTemplate] with [AppTemplate.FullScreenTemplate]
         * as default. The given [presenter] can override the template by either returning
         * [AppTemplate] directly or making its [BaseModel] type implement [ModelDelegate].
         */
        fun createAppTemplatePresenter(presenter: MoleculePresenter<Unit, *>): AppTemplatePresenter {
            return factory(presenter)
        }
    }
}
