package software.amazon.app.platform.sample.navigation

import androidx.compose.runtime.Composable
import assertk.assertThat
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.test
import software.amazon.app.platform.sample.login.LoginPresenter
import software.amazon.app.platform.sample.user.FakeUserManager
import software.amazon.app.platform.sample.user.UserPagePresenter

class NavigationPresenterImplTest {

  @Test
  fun `after login the presenter navigates from the login screen to the user page screen`() =
    runTest {
      val userManager = FakeUserManager()

      val presenter =
        NavigationPresenterImpl(
          userManager = userManager,
          loginPresenter = { FakeLoginPresenter() },
        )

      presenter.test(this) {
        assertThat(awaitItem()).isInstanceOf<LoginPresenter.Model>()

        userManager.login(
          userId = 1L,
          scope = this@runTest,
          component =
            object : NavigationPresenterImpl.UserComponent {
              override val userPresenter: UserPagePresenter
                get() = FakeUserPagePresenter()
            },
        )

        assertThat(awaitItem()).isInstanceOf<UserPagePresenter.Model>()
      }
    }

  private class FakeLoginPresenter : LoginPresenter {
    @Composable
    override fun present(input: Unit): LoginPresenter.Model =
      LoginPresenter.Model(loginInProgress = false) {}
  }

  private class FakeUserPagePresenter : UserPagePresenter {
    @Composable
    override fun present(input: Unit): UserPagePresenter.Model =
      object : UserPagePresenter.Model {
        override val listModel: BaseModel = object : BaseModel {}
        override val detailModel: BaseModel = object : BaseModel {}
      }
  }
}
