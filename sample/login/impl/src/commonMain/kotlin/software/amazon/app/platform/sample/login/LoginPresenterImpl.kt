package software.amazon.app.platform.sample.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.sample.login.LoginPresenter.Model
import software.amazon.app.platform.sample.user.UserManager
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/** Production implementation for [LoginPresenter]. */
@Inject
@ContributesBinding(AppScope::class)
class LoginPresenterImpl(private val userManager: UserManager) : LoginPresenter {
  @Composable
  @Suppress("MagicNumber")
  override fun present(input: Unit): Model {
    var loginInProgress by remember { mutableStateOf(false) }

    if (loginInProgress) {
      LaunchedEffect(loginInProgress) {
        delay(1.seconds)
        userManager.login(Random.nextLong(10_000))
        loginInProgress = false
      }
    }

    return Model(loginInProgress = loginInProgress) {
      when (it) {
        is LoginPresenter.Event.Login -> {
          loginInProgress = true
        }
      }
    }
  }
}
