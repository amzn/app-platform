package software.amazon.app.platform.sample.user

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import software.amazon.app.platform.presenter.molecule.test
import software.amazon.app.platform.scope.runTestWithScope

class UserPagePresenterImplTest {

  @Test
  fun `the selected item can be changed`() = runTest {
    val userManager = FakeUserManager()
    userManager.login(1L)
    val user = checkNotNull(userManager.user.value)

    val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)

    val presenter =
      UserPagePresenterImpl(user, UserPageListPresenter(sessionTimeout), UserPageDetailPresenter(sessionTimeout))

    presenter.test(this) {
      awaitItem().let { model ->
        assertThat((model.listModel as UserPageListPresenter.Model).selectedIndex).isEqualTo(0)
        assertThat((model.detailModel as UserPageDetailPresenter.Model).text).isEqualTo(FakeUser.fakeAttribute1.value)

        (model.listModel as UserPageListPresenter.Model).onEvent(UserPageListPresenter.Event.ItemSelected(1))
      }

      awaitItem().let { model ->
        assertThat((model.listModel as UserPageListPresenter.Model).selectedIndex).isEqualTo(1)
        assertThat((model.detailModel as UserPageDetailPresenter.Model).text).isEqualTo(FakeUser.fakeAttribute2.value)
      }
    }
  }

  @Test
  fun `the session timeout progress is updated`() = runTestWithScope { scope ->
    val userManager = FakeUserManager()
    userManager.login(1L)
    val user = checkNotNull(userManager.user.value)

    val sessionTimeout = SessionTimeout(userManager, FakeAnimationHelper)
    scope.register(sessionTimeout)

    val presenter =
      UserPagePresenterImpl(user, UserPageListPresenter(sessionTimeout), UserPageDetailPresenter(sessionTimeout))

    presenter.test(this) {
      assertThat((awaitItem().detailModel as UserPageDetailPresenter.Model).timeoutProgress).isEqualTo(1f)

      advanceTimeBy(SessionTimeout.initialTimeout / 2)
      runCurrent()

      assertThat((expectMostRecentItem().detailModel as UserPageDetailPresenter.Model).timeoutProgress).isEqualTo(0.5f)
    }
  }
}
