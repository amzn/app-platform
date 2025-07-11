package software.amazon.app.platform.template.navigation

import androidx.compose.runtime.Composable
import software.amazon.app.platform.template.navigation.NavigationHeaderPresenter.Model
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(AppScope::class)
class NavigationHeaderPresenterImpl() : NavigationHeaderPresenter {
    @Composable
    override fun present(input: Unit): Model {
        return Model(
            exampleBoolean = true
        )
    }
}
