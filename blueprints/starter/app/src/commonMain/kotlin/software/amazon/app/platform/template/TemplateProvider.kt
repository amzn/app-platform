package software.amazon.app.platform.template

import software.amazon.app.platform.template.templates.AppTemplate
import software.amazon.app.platform.template.templates.AppTemplatePresenter
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculeScope
import software.amazon.app.platform.presenter.molecule.MoleculeScopeFactory
import software.amazon.app.platform.presenter.molecule.launchMoleculePresenter
import software.amazon.app.platform.template.navigation.NavigationPresenter

/**
 * Shared class between all platforms to start collecting [AppTemplate] in a [StateFlow].
 * Inject [Factory] to create a new instance. Once the instance is no longer needed, call [cancel]
 * to clean up any resources.
 *
 * [NavigationPresenter] serves as the root presenter and gets wrapped in a
 * [AppTemplatePresenter].
 */
@Inject
class TemplateProvider(
    presenter: NavigationPresenter,
    templatePresenterFactory: AppTemplatePresenter.Factory,
    @Assisted private val moleculeScope: MoleculeScope,
) {
    /** The templates that should be rendered in the UI. */
    val templates: StateFlow<AppTemplate> by lazy {
        moleculeScope
            .launchMoleculePresenter(
                presenter = templatePresenterFactory.createAppTemplatePresenter(presenter),
                input = Unit,
            )
            .model
    }

    /** Releases all resources and stops [templates] from updating further. */
    fun cancel() {
        moleculeScope.cancel()
    }

    /** Factory class to create a new instance of [TemplateProvider]. */
    @Inject
    class Factory(
        private val moleculeScopeFactory: MoleculeScopeFactory,
        private val templateProvider: (MoleculeScope) -> TemplateProvider,
    ) {
        /**
         * Creates a new instance of [TemplateProvider]. Call [TemplateProvider.cancel] when the
         * instance not needed anymore to avoid leaking resources.
         */
        fun createTemplateProvider(): TemplateProvider {
            return templateProvider(moleculeScopeFactory.createMoleculeScope())
        }
    }
}
