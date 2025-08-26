@file:OptIn(ExperimentalCompilerApi::class)

package software.amazon.app.platform.inject.metro

import com.tschuchort.compiletesting.JvmCompilationResult
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import software.amazon.test.TestRendererComponent

internal val JvmCompilationResult.graphInterface: Class<*>
  get() = classLoader.loadClass("software.amazon.test.GraphInterface")

internal fun Class<*>.newTestRendererComponent(): TestRendererComponent {
  val companionObject = fields.single().get(null)
  return classes
    .single { it.simpleName == "Companion" }
    .declaredMethods
    .single { it.name == "create" }
    .invoke(companionObject) as TestRendererComponent
}
