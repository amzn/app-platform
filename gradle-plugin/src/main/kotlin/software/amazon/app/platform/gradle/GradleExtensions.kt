package software.amazon.app.platform.gradle

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import java.util.Locale
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.tasks.TaskContainer
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun PluginContainer.withIds(vararg pluginIds: String, action: (Plugin<*>) -> Unit) {
  pluginIds.forEach { id -> withId(id) { action(it) } }
}

internal fun Project.requireParent(): Project =
  requireNotNull(parent) {
    "The parent project for a module enabling the module structure should not be null."
  }

internal val Project.isKmpModule: Boolean
  get() = plugins.hasPlugin(PluginIds.KOTLIN_MULTIPLATFORM)

internal val Project.android: CommonExtension<*, *, *, *, *, *>
  get() = extensions.getByType(CommonExtension::class.java)

internal val Project.androidComponents: AndroidComponentsExtension<*, *, *>
  get() = extensions.getByType(AndroidComponentsExtension::class.java)

internal val Project.kmpExtension: KotlinMultiplatformExtension
  get() = extensions.getByType(KotlinMultiplatformExtension::class.java)

internal val Project.composeDependencies: ComposePlugin.Dependencies
  get() = ComposePlugin.Dependencies(this)

internal fun TaskContainer.namedOptional(name: String, configurationAction: (Task) -> Unit) {
  try {
    named(name, configurationAction)
  } catch (_: UnknownTaskException) {}
}

internal fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
