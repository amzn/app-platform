package software.amazon.app.platform.sample.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.sample.login.LoginPresenter
import software.amazon.app.platform.sample.user.UserManager
import software.amazon.app.platform.sample.user.UserPagePresenter
import software.amazon.app.platform.sample.user.UserScope
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.di.diComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/**
 * Production implementation of [NavigationPresenter].
 *
 * [loginPresenter] is injected lazily to delay initialization until it's actually needed. See
 * [MoleculePresenter] for more details.
 */
@Inject
@ContributesBinding(AppScope::class)
class NavigationPresenterImpl(
  private val userManager: UserManager,
  private val loginPresenter: () -> LoginPresenter,
) : NavigationPresenter {

  @Composable
  override fun present(input: Unit): BaseModel {
    val scope = getUserScope()
    if (scope == null) {
      // If no user is logged in, then show the logged in screen.
      val presenter = remember { loginPresenter() }
      return presenter.present(Unit)
    }

    // A user is logged in. Use the user component to get an instance of UserPagePresenter, which is
    // only
    // part of the user scope.
    val userPresenter = remember(scope) { scope.diComponent<UserComponent>().userPresenter }
    return userPresenter.present(Unit)
  }

  @Composable
  private fun getUserScope(): Scope? {
    val user by userManager.user.collectAsState()
    return if (user?.scope?.isDestroyed() == true) null else user?.scope
  }

  /**
   * This component interface gives us access to objects from the user scope. We cannot inject
   * `UserPresenter` in the constructor, because it's part of the user scope.
   */
  @ContributesTo(UserScope::class)
  interface UserComponent {
    /** The [UserPagePresenter] provided by the user scope. */
    val userPresenter: UserPagePresenter
  }
}
