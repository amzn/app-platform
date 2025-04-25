package software.amazon.app.platform.sample.user

import app_platform.sample.user.impl.generated.resources.Res
import app_platform.sample.user.impl.generated.resources.allDrawableResources
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.coroutine.addCoroutineScopeScoped
import software.amazon.app.platform.scope.di.addDiComponent
import software.amazon.app.platform.scope.register
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Production implementation of [UserManager].
 *
 * This class is responsible for creating the [UserScope] and [UserComponent].
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class UserManagerImpl(
  private val rootScopeProvider: RootScopeProvider,
  private val userComponentFactory: UserComponent.Factory,
) : UserManager {

  private val _user = MutableStateFlow<User?>(null)
  override val user: StateFlow<User?> = _user

  override fun login(userId: Long) {
    logout()

    val user =
      UserImpl(
        userId = userId,
        attributes =
          listOf(
            User.Attribute("First Name", firstName()),
            User.Attribute("Last Name", lastName()),
            User.Attribute("Age", age()),
            User.Attribute(User.Attribute.PICTURE_KEY, profilePicture(), metadata = true),
          ),
      )

    val userComponent = userComponentFactory.createUserComponent(user)

    val userScope =
      rootScopeProvider.rootScope.buildChild("user-$userId") {
        addDiComponent(userComponent)

        addCoroutineScopeScoped(userComponent.userScopeCoroutineScopeScoped)
      }

    user.scope = userScope

    _user.value = user

    // Register instances after the userScope has been set to avoid race conditions for Scoped
    // instances that may use the userScope.
    userScope.register(userComponent.userScopedInstances)
  }

  override fun logout() {
    val currentUserScope = user.value?.scope

    _user.value = null

    currentUserScope?.destroy()
  }

  private fun firstName(): String {
    return listOf("Jim", "Andrea", "Alan", "Alice").random()
  }

  private fun lastName(): String {
    return listOf("Lee", "Smith", "Anderson", "Miller").random()
  }

  @Suppress("MagicNumber")
  private fun age(): String {
    return Random.nextInt(100).toString()
  }

  @OptIn(ExperimentalResourceApi::class)
  private fun profilePicture(): String {
    val keys = Res.allDrawableResources.keys.toList()
    return if (Random.nextBoolean()) keys[0] else keys[1]
  }
}
