package software.amazon.app.platform.bunny

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

/**
 * Interface that can be implemented by models to indicate that this layer is optional and can be
 * rendered or not. Use [AnimatedTemporaryContainer] for an easier animation and preserving the last
 * rendered model.
 */
interface TemporaryContainer {
  val visible: Boolean
}

/**
 * Keeps an internal reference to the last rendered model for the exit animation. [T] refers to the
 * rendered model type. [model] should always be the most recent model. [content] gives you access
 * to the transition between the shown and hidden state. The argument is the last shown / rendered
 * model.
 */
@Composable
inline fun <reified T : TemporaryContainer> AnimatedTemporaryContainer(
  model: TemporaryContainer,
  content: @Composable Transition<Boolean>.(T) -> Unit,
) {
  val visibleState = remember { MutableTransitionState(false) }
  visibleState.targetState = model.visible

  val transition = rememberTransition(visibleState)

  val lastShownModel = rememberPrevious(model) { _, curr -> curr.visible }
  val renderedModel = model as? T ?: lastShownModel as? T
  if (renderedModel != null) {
    content(transition, renderedModel)
  }
}

// From:
// https://stackoverflow.com/questions/67801939/get-previous-value-of-state-in-composable-jetpack-compose
/** Returns a dummy MutableState that does not cause render when setting it */
@Composable
private fun <T> rememberRef(): MutableState<T?> {
  // for some reason it always recreated the value with vararg keys,
  // leaving out the keys as a parameter for remember for now
  return remember {
    object : MutableState<T?> {
      override var value: T? = null

      override fun component1(): T? = value

      override fun component2(): (T?) -> Unit = { value = it }
    }
  }
}

@Composable
fun <T> rememberPrevious(
  current: T,
  shouldUpdate: (prev: T?, curr: T) -> Boolean = { a: T?, b: T -> a != b },
): T? {
  val ref = rememberRef<T>()

  // launched after render, so the current render will have the old value anyway
  SideEffect {
    if (shouldUpdate(ref.value, current)) {
      ref.value = current
    }
  }

  return ref.value
}
