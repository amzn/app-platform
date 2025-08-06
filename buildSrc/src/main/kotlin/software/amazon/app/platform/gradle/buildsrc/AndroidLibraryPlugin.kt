package software.amazon.app.platform.gradle.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import software.amazon.app.platform.gradle.buildsrc.AppPlatformExtension.Companion.appPlatformBuildSrc
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.configureKtfmt

public open class AndroidLibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BasePlugin::class.java)

    target.plugins.apply(Plugins.ANDROID_LIBRARY)
    target.plugins.apply(Plugins.KOTLIN_ANDROID)
    target.plugins.apply(BaseAndroidPlugin::class.java)

    target.configureKotlin()
    target.configureTests()
    target.configureCoroutines()
    target.configureKtfmt()
  }

  private fun Project.configureKotlin() {
    dependencies.add(
      "api",
      dependencies.platform(libs.findLibrary("kotlin.bom").get().get().toString()),
    )

    extensions.getByType(KotlinAndroidExtension::class.java).compilerOptions {
      allWarningsAsErrors.set(appPlatformBuildSrc.isKotlinWarningsAsErrors())

      jvmTarget.set(javaTarget)
    }
  }

  private fun Project.configureTests() {
    releaseTask.configure { task -> task.dependsOn("test") }

    dependencies.add("testImplementation", libs.findLibrary("kotlin.test").get().get().toString())
    dependencies.add("testImplementation", libs.findLibrary("assertk").get().get().toString())
  }

  private fun Project.configureCoroutines() {
    dependencies.add("implementation", libs.findLibrary("coroutines.core").get().get().toString())
    dependencies.add(
      "testImplementation",
      libs.findLibrary("coroutines.test").get().get().toString(),
    )
    dependencies.add("testImplementation", libs.findLibrary("turbine").get().get().toString())
  }
}
