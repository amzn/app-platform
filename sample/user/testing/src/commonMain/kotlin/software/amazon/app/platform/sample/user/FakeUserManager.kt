package software.amazon.app.platform.sample.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.buildTestScope
import software.amazon.app.platform.scope.di.addDiComponent

/**
 * Fake implementation of [UserManager], which is useful in unit tests.
 *
 * This class is part of the `:testing` module and shared with other modules.
 */
class FakeUserManager(override val user: MutableStateFlow<User?> = MutableStateFlow(null)) :
  UserManager {

  override fun login(userId: Long) {
    user.value = FakeUser(userId = userId)
  }

  /**
   * Overloaded function to change the coroutine scope and kotlin-inject component for the
   * [FakeUser].
   */
  fun login(userId: Long, scope: TestScope, component: Any) {
    user.value =
      FakeUser(userId = userId, scope = Scope.buildTestScope(scope) { addDiComponent(component) })
  }

  override fun logout() {
    user.value?.scope?.destroy()
    user.value = null
  }
}
