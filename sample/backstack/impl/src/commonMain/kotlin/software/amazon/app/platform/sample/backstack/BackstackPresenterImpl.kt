package software.amazon.app.platform.sample.backstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.amazon.app.platform.sample.backstack.BackstackPresenter.Model
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/** Production implementation for [BackstackPresenter]. */
@Inject
@ContributesBinding(AppScope::class)
class BackstackPresenterImpl : BackstackPresenter {

  @Composable
  override fun present(input: Unit): Model {
    val scope = remember { BackstackScope(ChildPresenter("Screen 1")) }

    return returningCompositionLocalProvider(LocalBackstackScope.provides(scope)) {
      with(checkNotNull(LocalBackstackScope.current)) {
        val backstack by backstack
        val forward by forward

        if (backstack.isEmpty()) {
          Model.Done
        } else {
          Model.ShowBackstack(childModel = backstack.last().present(Unit), forward = forward) {
            when (it) {
              BackstackPresenter.Event.Add -> {
                push(ChildPresenter("Screen ${backstack.size + 1}"))
              }

              BackstackPresenter.Event.Remove -> {
                pop()
              }
            }
          }
        }
      }
    }
  }
}
