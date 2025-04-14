package software.amazon.app.platform.sample.user

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.Scoped
import software.amazon.app.platform.scope.buildTestScope
import software.amazon.app.platform.scope.coroutine.CoroutineScopeScoped
import software.amazon.app.platform.scope.di.addDiComponent
import software.amazon.app.platform.scope.di.diComponent

class UserManagerImplTest {

  @Test
  fun `a user can login and logout`() = runTest {
    val rootScopeProvider = rootScopeProvider()
    val userManager = userManager(rootScopeProvider)

    assertThat(userManager.user.value).isNull()

    userManager.login(1L)
    assertThat(userManager.user.value).isNotNull()

    userManager.logout()
    assertThat(userManager.user.value).isNull()
  }

  @Test
  fun `logging out destroys the user scope`() = runTest {
    val rootScopeProvider = rootScopeProvider()
    val userManager = userManager(rootScopeProvider)

    userManager.login(1L)
    val userScope = checkNotNull(userManager.user.value?.scope)
    assertThat(userScope.isDestroyed()).isFalse()

    userManager.logout()
    assertThat(userScope.isDestroyed()).isTrue()
  }

  @Test
  fun `logging in while another user is logged in logs out the user`() = runTest {
    val rootScopeProvider = rootScopeProvider()
    val userManager = userManager(rootScopeProvider)

    userManager.login(1L)
    assertThat(userManager.user.value?.userId).isEqualTo(1L)
    val userScope = checkNotNull(userManager.user.value?.scope)
    assertThat(userScope.isDestroyed()).isFalse()

    userManager.login(2L)
    assertThat(userManager.user.value?.userId).isEqualTo(2L)
    assertThat(userScope.isDestroyed()).isTrue()
  }

  private fun userManager(rootScopeProvider: RootScopeProvider): UserManagerImpl =
    UserManagerImpl(rootScopeProvider, rootScopeProvider.rootScope.diComponent())

  private fun TestScope.appComponent(): Any =
    object : UserComponent.Factory {
      override fun createUserComponent(user: User): UserComponent {
        return object : UserComponent {
          override val userScopedInstances: Set<Scoped> = emptySet()
          override val userScopeCoroutineScopeScoped: CoroutineScopeScoped =
            CoroutineScopeScoped(coroutineContext + Job() + CoroutineName("TestUserScope"))
        }
      }
    }

  private fun TestScope.rootScopeProvider(appComponent: Any = appComponent()): RootScopeProvider {
    return object : RootScopeProvider {
      override val rootScope: Scope =
        Scope.buildTestScope(this@rootScopeProvider) { addDiComponent(appComponent) }
    }
  }
}
