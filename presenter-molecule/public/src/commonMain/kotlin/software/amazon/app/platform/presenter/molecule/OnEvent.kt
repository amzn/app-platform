package software.amazon.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.launch
import software.amazon.app.platform.presenter.BaseModel

/**
 * Used to process events returned by the UI layer in the scope that runs our presenters, which
 * usually is `PresenterCoroutineScope`. Without this wrapper events would be processed on the main
 * thread and could potentially block the UI or cause lag.
 *
 * Furthermore, the returned lambda is remembered as state within the composable, which allows to
 * make [BaseModel] implementations data classes. Without remembering every [BaseModel] instance
 * would not equal another instance, since different lambda instances are never equal to each other.
 *
 * A common pattern looks as follows:
 * ```
 * @Inject
 * class LoginPresenter(..) : MoleculePresenter<Unit, Model> {
 *
 *     @Composable
 *     override fun present(input: Unit): Model {
 *         // ...
 *         return Model(
 *             name = ..
 *             onEvent = onEvent {
 *                 when (it) {
 *                     is Event.OnNameClick -> ...
 *                     is Event.Login -> ...
 *                 }
 *             }
 *         )
 *     }
 *
 *     data class Model(
 *         val name: String,
 *         val onEvent: (Event) -> Unit
 *     ) : BaseModel
 *
 *     sealed interface Event {
 *         object OnNameClick : Event
 *         class Login(val userName: String) : Event
 *     }
 * }
 * ```
 */
@Suppress("unused", "UnusedReceiverParameter")
@Composable
@Deprecated("This function isn't needed anymore and can be removed")
public fun <EventT : Any> MoleculePresenter<*, *>.onEvent(
  handler: @DisallowComposableCalls suspend (EventT) -> Unit
): (EventT) -> Unit {
  // This function creates, remembers and returns a separate lambda from the `handler`
  // argument. The newly created lambda forwards events to the last `handler` lambda this
  // function was called with.
  val scope = rememberCoroutineScope()
  val lambdaReference = remember { AtomicRefWrapper(handler) }

  lambdaReference.value = handler

  DisposableEffect(Unit) {
    onDispose {
      // Release the lambda
      lambdaReference.value = null
    }
  }

  return remember {
    { event ->
      // Launch a coroutine in this scope so that we process the event on the
      // PresenterCoroutineScope.
      scope.launch { lambdaReference.value?.invoke(event) }
    }
  }
}

// This wrapper is needed because the compiler plugin for AtomicFU doesn't work within
// a composable function.
private class AtomicRefWrapper<T : Any>(value: T) {
  private val atomicRef: AtomicRef<T?> = atomic(value)

  var value: T?
    get() = atomicRef.value
    set(value) {
      atomicRef.update { value }
    }
}
