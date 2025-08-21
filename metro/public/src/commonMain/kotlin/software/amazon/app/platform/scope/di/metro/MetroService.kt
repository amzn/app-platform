package software.amazon.app.platform.scope.di.metro

import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.parents

@PublishedApi internal const val METRO_COMPONENT_KEY: String = "metroComponent"

/**
 * Provides the Metro component that has been added to this [Scope]. A common pattern is to use this
 * function to look up component interfaces in static contexts like test methods, static functions
 * or where constructor injection cannot be used, e.g.
 *
 * ```
 * interface HudComponent {
 *     val hudManager: HudManager
 * }
 *
 * rootScope.metroComponent<HudComponent>().hudManager
 * ```
 *
 * The given component type [T] of the DI component can be provided by this scope or a parent scope.
 */
public inline fun <reified T : Any> Scope.metroComponent(): T {
  parents(includeSelf = true)
    .firstNotNullOfOrNull { scope ->
      val component = scope.getService<T>(METRO_COMPONENT_KEY)
      if (T::class.isInstance(component)) {
        component
      } else {
        null
      }
    }
    ?.let {
      return it
    }

  val diComponents =
    parents(includeSelf = true)
      .map { it.getService<Any>(METRO_COMPONENT_KEY) }
      .filterNotNull()
      .map { it::class }

  // The replace() will align inner class references across platforms. Native uses a '.',
  // whereas the JVM platform use '$'.
  throw NoSuchElementException(
    "Couldn't find component implementing ${T::class}. Inspected: " +
      "[${diComponents.joinToString { it.simpleName.toString() }}] (fully qualified " +
      "names: [${diComponents.joinToString { it.toString().replace('\$', '.') }}])"
  )
}

/**
 * Adds the given [component] to this builder. The instance can be later retrieved with
 * [metroComponent].
 */
public fun Scope.Builder.addMetroComponent(component: Any) {
  addService(METRO_COMPONENT_KEY, component)
}
