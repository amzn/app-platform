package software.amazon.app.platform.sample.backstack

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer
import software.amazon.app.platform.sample.backstack.BackstackPresenter.Model

/** Renders the content for [BackstackPresenter] on screen using Compose Multiplatform. */
@Inject
@ContributesRenderer
class BackstackRenderer(private val rendererFactory: RendererFactory) : ComposeRenderer<Model>() {

  @Composable
  override fun Compose(model: Model) {
    check(model is Model.ShowBackstack)

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          CrossSlide(targetState = model.childModel, reverseAnimation = !model.forward) { screen ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              rendererFactory.getComposeRenderer(screen).renderCompose(screen)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          Button(
            onClick = { model.onEvent(BackstackPresenter.Event.Add) },
            modifier = Modifier.weight(1f).padding(end = 8.dp),
          ) {
            Text("Add")
          }
          Button(
            onClick = { model.onEvent(BackstackPresenter.Event.Remove) },
            modifier = Modifier.weight(1f).padding(start = 8.dp),
          ) {
            Text("Remove")
          }
        }
      }
    }
  }

  // https://gist.github.com/DavidIbrahim/5f4c0387b571f657f4de976822c2a225
  @Composable
  private fun <T> CrossSlide(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Offset> = tween(500),
    reverseAnimation: Boolean = false,
    content: @Composable (T) -> Unit,
  ) {
    val direction: Int = if (reverseAnimation) -1 else 1
    val items = remember { mutableStateListOf<SlideInOutAnimationState<T>>() }
    val transitionState = remember { MutableTransitionState(targetState) }
    val targetChanged = (targetState != transitionState.targetState)
    transitionState.targetState = targetState
    val transition: Transition<T> = rememberTransition(transitionState)

    if (targetChanged || items.isEmpty()) {
      // Only manipulate the list when the state is changed, or in the first run.
      val keys =
        items
          .map { it.key }
          .run {
            if (!contains(targetState)) {
              toMutableList().also { it.add(targetState) }
            } else {
              this
            }
          }
      items.clear()
      keys.mapTo(items) { key ->
        SlideInOutAnimationState(key) {
          val xTransition by
            transition.animateOffset(transitionSpec = { animationSpec }, label = "") {
              if (it == key) Offset(0f, 0f) else Offset(1000f, 1000f)
            }
          Box(
            modifier.graphicsLayer {
              this.translationX =
                if (transition.targetState == key) direction * xTransition.x
                else direction * -xTransition.x
            }
          ) {
            content(key)
          }
        }
      }
    } else if (transitionState.currentState == transitionState.targetState) {
      // Remove all the intermediate items from the list once the animation is finished.
      items.removeAll { it.key != transitionState.targetState }
    }

    Box(modifier) { items.forEach { key(it.key) { it.content() } } }
  }

  private data class SlideInOutAnimationState<T>(val key: T, val content: @Composable () -> Unit)
}
