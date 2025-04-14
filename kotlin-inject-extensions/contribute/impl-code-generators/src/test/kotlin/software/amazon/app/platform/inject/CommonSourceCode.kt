@file:OptIn(ExperimentalCompilerApi::class)

package software.amazon.app.platform.inject

import com.tschuchort.compiletesting.JvmCompilationResult
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.descriptors.runtime.structure.primitiveByWrapper
import software.amazon.lastmile.kotlin.inject.anvil.internal.Origin

internal val JvmCompilationResult.componentInterface: Class<*>
  get() = classLoader.loadClass("software.amazon.test.ComponentInterface")

internal val Class<*>.inner: Class<*>
  get() = classes.single { it.simpleName == "Inner" }

internal val Class<*>.origin: Class<*>
  get() = getAnnotation(Origin::class.java).value.java

internal val Class<*>.generatedComponent: Class<*>
  get() =
    classLoader.loadClass(
      "$OPEN_SOURCE_LOOKUP_PACKAGE." +
        canonicalName.split(".").joinToString(separator = "") { it.capitalize() }
    )

internal fun <T : Any> Class<*>.newComponent(vararg arguments: Any): T {
  @Suppress("UNCHECKED_CAST")
  return classLoader
    .loadClass("$packageName.Inject$simpleName")
    .getDeclaredConstructor(
      *arguments.map { arg -> arg::class.java.primitiveByWrapper ?: arg::class.java }.toTypedArray()
    )
    .newInstance(*arguments) as T
}
