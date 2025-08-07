package software.amazon.app.platform.gradle.buildsrc

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import software.amazon.app.platform.gradle.buildsrc.AppPlugin.App.Companion.app
import software.amazon.app.platform.gradle.buildsrc.AppPlugin.Companion.allExportedDependencies
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.composeDependencies
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.kmpExtension
import software.amazon.app.platform.gradle.isAppModule

internal sealed interface Platform {
  val unitTestTaskName: String?

  fun configurePlatform()

  fun configureCoroutines() = Unit

  fun configureCompose() = Unit

  abstract class Native : Platform {
    protected abstract val project: Project
  }

  abstract class Ios : Native() {

    abstract val target: KotlinNativeTarget

    override fun configurePlatform() {
      target.binaries.framework {
        baseName =
          if (project.isAppModule()) {
            project.app.iosFrameworkName
          } else {
            project.safePathString.capitalize()
          }
        isStatic = project.isAppModule()

        if (project.isAppModule()) {
          project.allExportedDependencies().forEach { dependency -> export(dependency) }
        }
      }
    }
  }

  private class AndroidPlatform(private val project: Project) : Platform {

    override val unitTestTaskName: String = "testDebugUnitTest"

    override fun configurePlatform() {
      project.kmpExtension.androidTarget().compilerOptions { jvmTarget.set(project.javaTarget) }

      project.android.sourceSets.getByName("main").apply {
        project
          .file("src/androidMain/AndroidManifest.xml")
          .takeIf { it.exists() }
          ?.let { manifest.srcFile(it) }
        project.file("src/androidMain/res").takeIf { it.exists() }?.let { res.srcDirs(it) }
        project
          .file("src/commonMain/resources")
          .takeIf { it.exists() }
          ?.let { resources.srcDirs(it) }
      }
    }
  }

  class DesktopPlatform(private val project: Project) : Platform {

    override val unitTestTaskName: String = "desktopTest"

    override fun configurePlatform() {
      project.kmpExtension.jvm("desktop").compilerOptions { jvmTarget.set(project.javaTarget) }

      with(project.extensions.getByType(JavaPluginExtension::class.java)) {
        sourceCompatibility = project.javaVersion
        targetCompatibility = project.javaVersion
      }
    }

    override fun configureCoroutines() {
      project.kmpExtension.sourceSets.getByName("desktopMain").dependencies {
        implementation(project.libs.findLibrary("coroutines.swing").get().get().toString())
      }
    }

    override fun configureCompose() {
      project.kmpExtension.sourceSets.getByName("desktopMain").dependencies {
        implementation(project.composeDependencies.desktop.currentOs)
      }

      project.kmpExtension.sourceSets.getByName("desktopTest").dependencies {
        implementation(project.composeDependencies.desktop.uiTestJUnit4)
        implementation(project.composeDependencies.desktop.currentOs)
      }
    }
  }

  private abstract class Linux : Platform {

    abstract val project: Project
    abstract val target: KotlinNativeTarget

    override fun configurePlatform() {
      target.binaries { sharedLib { baseName = project.safePathString.capitalize() } }
    }
  }

  private class LinuxArm64(override val project: Project) : Linux() {
    // Tests aren't supported, because the KMP Gradle plugin doesn't generate the Gradle tasks.
    override val unitTestTaskName: String? = null

    override val target: KotlinNativeTarget by lazy { project.kmpExtension.linuxArm64() }
  }

  private class LinuxX64(override val project: Project) : Linux() {
    override val unitTestTaskName = "linuxX64Test"

    override val target: KotlinNativeTarget by lazy { project.kmpExtension.linuxX64() }
  }

  private class IosSimulatorArm64(override val project: Project) : Ios() {

    override val unitTestTaskName: String = "iosSimulatorArm64Test"

    override val target: KotlinNativeTarget by lazy { project.kmpExtension.iosSimulatorArm64() }
  }

  private class IosArm64(override val project: Project) : Ios() {

    override val unitTestTaskName: String? = null

    override val target: KotlinNativeTarget by lazy { project.kmpExtension.iosArm64() }
  }

  private class IosX64(override val project: Project) : Ios() {

    override val unitTestTaskName: String = "iosX64Test"

    override val target: KotlinNativeTarget by lazy { project.kmpExtension.iosX64() }
  }

  private class Wasm(private val project: Project) : Platform {
    override val unitTestTaskName: String = "wasmJsTest"

    override fun configurePlatform() {
      @Suppress("OPT_IN_USAGE")
      project.kmpExtension.wasmJs { browser { outputModuleName.set(project.safePathString) } }
    }
  }

  companion object {

    private val projectsUsingCompose =
      setOf(":renderer-compose-multiplatform:public", ":robot-compose-multiplatform:public") +
        AppPlugin.App.entries.map { it.rootProjectPath }

    fun Project.allPlatforms(): Set<Platform> = buildSet {
      // Always add Android. It's our most important platform and buildable in all
      // environments (locally and CI)
      add(AndroidPlatform(project = this@allPlatforms))

      // Android-only modules have "android" in their name and don't need other
      // platforms.
      if ("android" !in path.lowercase()) {
        add(DesktopPlatform(project = this@allPlatforms))

        add(IosSimulatorArm64(project = this@allPlatforms))
        add(IosArm64(project = this@allPlatforms))
        add(IosX64(project = this@allPlatforms))

        add(Wasm(project = this@allPlatforms))

        // Compose Multiplatform does not support Linux, so exclude these modules.
        if (projectsUsingCompose.none { path.startsWith(it) }) {
          add(LinuxArm64(project = this@allPlatforms))
          add(LinuxX64(project = this@allPlatforms))
        }
      }
    }
  }
}
