package software.amazon.app.platform.recipes.backstack

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.backstack.CrossSlideBackstackPresenter.Model
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer

/**
 * Plays a cross-slide animation whenever the backstack provided [CrossSlideBackstackPresenter]
 * changes.
 */
@Inject
@ContributesRenderer
class CrossSlideBackstackRenderer(private val rendererFactory: RendererFactory) :
  ComposeRenderer<Model>() {

  @Composable
  override fun Compose(model: Model) {
    Column(modifier = Modifier.fillMaxSize()) {
      CrossSlide(
        targetState = model.delegate,
        reverseAnimation =
          model.backstackScope.lastBackstackChange.value.action ==
            PresenterBackstackScope.BackstackChange.Action.POP,
        contentKey = {
          // Include size of the backstack in the key in case the model type is the same but the
          // position in the stack differs.
          it::class.hashCode() + model.backstackScope.lastBackstackChange.value.backstack.size
        },
      ) { screen ->
        rendererFactory.getComposeRenderer(screen).renderCompose(screen)
      }
    }
  }

  // https://gist.github.com/DavidIbrahim/5f4c0387b571f657f4de976822c2a225
  //
  // This animation implementation comes from the link above, but was modified with the
  // `contentKey` parameter to allows updates of the `targetState` without playing another
  // cross slide animation.
  //
  // Further the distance was adjusted to the screen size.
  @Composable
  @Suppress("LongMethod", "MagicNumber")
  private fun <T : Any> CrossSlide(
    targetState: T,
    contentKey: (targetState: T) -> Any = { it },
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Offset> = tween(500),
    reverseAnimation: Boolean = false,
    content: @Composable (T) -> Unit,
  ) {
    data class SlideInOutAnimationState<T>(
      val key: Any,
      val state: T,
      val content: @Composable () -> Unit,
    )

    val direction: Int = if (reverseAnimation) -1 else 1
    val items = remember { mutableStateListOf<SlideInOutAnimationState<T>>() }

    val targetKey = contentKey(targetState)

    // We only play a cross slide animation / transition, if the key has changed. If the state has
    // changed, but the key didn't, then we only need to update the rendered target state in the
    // same item.
    val transitionState = remember { MutableTransitionState(targetKey) }
    transitionState.targetState = targetKey
    val transition: Transition<Any> = rememberTransition(transitionState)

    val targetContentChanged = items.firstOrNull { it.key == targetKey }?.state != targetState

    if (targetContentChanged || items.isEmpty()) {
      // Only manipulate the list when the state is changed, or in the first run.
      items
        .map {
          if (it.key == targetKey) {
            // Change the old state remembered in the list to the new state.
            it.key to targetState
          } else {
            it.key to it.state
          }
        }
        .let {
          if (it.none { (key, _) -> key == targetKey }) {
            it + Pair(targetKey, targetState)
          } else {
            it
          }
        }
        .also { items.clear() }
        .mapTo(items) { (key, state) ->
          SlideInOutAnimationState(key, state) {
            val xTransition by
              transition.animateOffset(transitionSpec = { animationSpec }, label = "") {
                val containerSize = LocalWindowInfo.current.containerSize
                if (it == key) {
                  Offset(0f, 0f)
                } else {
                  Offset(containerSize.width.toFloat(), containerSize.height.toFloat())
                }
              }
            Box(
              modifier.graphicsLayer {
                this.translationX =
                  if (transition.targetState == key) {
                    direction * xTransition.x
                  } else {
                    direction * -xTransition.x
                  }
              }
            ) {
              content(state)
            }
          }
        }
    } else if (transitionState.isIdle) {
      // Remove all the intermediate items from the list once the animation is finished.
      items.removeAll { it.key != transitionState.targetState }
    }

    Box(modifier) { items.forEach { key(it.key) { it.content() } } }
  }
}
