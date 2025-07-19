package software.amazon.app.platform.recipes.saveable

import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider

/**
 * Allows to save the state defined with [rememberSaveable] for the subtree before disposing it to
 * make it possible to compose it back next time with the restored state. It allows different
 * navigation patterns to keep the ui state like scroll position for the currently not composed
 * screens from the backstack.
 *
 * The content should be composed using [SaveableStateProvider] while providing a key representing
 * this content. Next time [SaveableStateProvider] will be used with the same key its state will be
 * restored.
 *
 * This implementation is similar to [SaveableStateHolder] with the only difference that
 * [ReturningSaveableStateHolder] returns a result and doesn't have `Unit` as return type.
 */
interface ReturningSaveableStateHolder {
  /**
   * Put your content associated with a [key] inside the [content]. This will automatically save all
   * the states defined with [rememberSaveable] before disposing the content and will restore the
   * states when you compose with this key again.
   *
   * @param key to be used for saving and restoring the states for the subtree. Note that on Android
   *   you can only use types which can be stored inside the Bundle.
   * @param content the content for which [key] is associated.
   */
  @Suppress("ComposableNaming")
  @Composable
  fun <ModelT : BaseModel> SaveableStateProvider(
    key: Any,
    content: @Composable () -> ModelT,
  ): ModelT

  /** Removes the saved state associated with the passed [key]. */
  fun removeState(key: Any)
}

/**
 * Creates and remembers the instance of [ReturningSaveableStateHolder].
 *
 * This implementation is similar to [rememberSaveableStateHolder] with the only difference that
 * [ReturningSaveableStateHolder] returns a result and doesn't have `Unit` as return type.
 */
@Composable
fun rememberReturningSaveableStateHolder(): ReturningSaveableStateHolder =
  rememberSaveable(saver = CustomSaveableStateHolderImpl.Saver) { CustomSaveableStateHolderImpl() }
    .apply { parentSaveableStateRegistry = LocalSaveableStateRegistry.current }

private class CustomSaveableStateHolderImpl(
  private val savedStates: MutableMap<Any, Map<String, List<Any?>>> = mutableMapOf()
) : ReturningSaveableStateHolder {
  private val registries = mutableScatterMapOf<Any, SaveableStateRegistry>()
  var parentSaveableStateRegistry: SaveableStateRegistry? = null
  private val canBeSaved: (Any) -> Boolean = { parentSaveableStateRegistry?.canBeSaved(it) ?: true }

  @Composable
  override fun <ModelT : BaseModel> SaveableStateProvider(
    key: Any,
    content: @Composable () -> ModelT,
  ): ModelT {
    return returningReusableContent(key) {
      val registry = remember {
        require(canBeSaved(key)) {
          "Type of the key $key is not supported. On Android you can only use types " +
            "which can be stored inside the Bundle."
        }
        SaveableStateRegistry(savedStates[key], canBeSaved)
      }
      val model =
        returningCompositionLocalProvider(
          LocalSaveableStateRegistry provides registry,
          content = content,
        )
      DisposableEffect(Unit) {
        require(key !in registries) { "Key $key was used multiple times " }
        savedStates -= key
        registries[key] = registry
        onDispose {
          if (registries.remove(key) === registry) {
            registry.saveTo(savedStates, key)
          }
        }
      }

      model
    }
  }

  private fun saveAll(): MutableMap<Any, Map<String, List<Any?>>>? {
    val map = savedStates
    registries.forEach { key, registry -> registry.saveTo(map, key) }
    return map.ifEmpty { null }
  }

  override fun removeState(key: Any) {
    if (registries.remove(key) == null) {
      savedStates -= key
    }
  }

  private fun SaveableStateRegistry.saveTo(
    map: MutableMap<Any, Map<String, List<Any?>>>,
    key: Any,
  ) {
    val savedData = performSave()
    if (savedData.isEmpty()) {
      map -= key
    } else {
      map[key] = savedData
    }
  }

  companion object {
    val Saver: Saver<CustomSaveableStateHolderImpl, *> =
      Saver(save = { it.saveAll() }, restore = { CustomSaveableStateHolderImpl(it) })
  }

  @Composable
  private inline fun <ModelT : BaseModel> returningReusableContent(
    key: Any?,
    content: @Composable () -> ModelT,
  ): ModelT {
    lateinit var result: ModelT
    ReusableContent(key) { result = content() }
    return result
  }
}
