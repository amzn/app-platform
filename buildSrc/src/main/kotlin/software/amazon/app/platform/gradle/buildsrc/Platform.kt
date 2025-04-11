package software.amazon.app.platform.gradle.buildsrc

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
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
        baseName = if (project.path == ":sample:app") "SampleApp" else project.safePathString.capitalize()
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
      project.kmpExtension.androidTarget().compilations.configureEach {
        it.kotlinOptions.jvmTarget = project.javaVersion.toString()
      }

      project.android.sourceSets.getByName("main").apply {
        project.file("src/androidMain/AndroidManifest.xml").takeIf { it.exists() }?.let { manifest.srcFile(it) }
        project.file("src/androidMain/res").takeIf { it.exists() }?.let { res.srcDirs(it) }
        project.file("src/commonMain/resources").takeIf { it.exists() }?.let { resources.srcDirs(it) }
      }
    }
  }

  class DesktopPlatform(private val project: Project) : Platform {

    override val unitTestTaskName: String = "desktopTest"

    override fun configurePlatform() {
      project.kmpExtension.jvm("desktop").compilations.configureEach {
        it.kotlinOptions.jvmTarget = project.javaVersion.toString()
      }

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
    override val unitTestTaskName
      get() = "linuxX64Test"

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

  companion object {

    private val projectsUsingCompose =
      setOf(":renderer-compose-multiplatform:public", ":robot-compose-multiplatform:public", ":sample:")

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

        // TODO: check with Github
        if (!ci) {
          // Compose Multiplatform does not support Linux, so exclude these modules.
          //
          // Also don't build Linux in CI, because the KMP plugin attempts to download
          // resources that are blocked:
          //
          // > Failed to apply plugin class 'software.amazon.app.platform.gradle.buildsrc.KmpPlugin'.
          //   > Could not resolve all files for configuration ':kotlin-inject:impl:detachedConfiguration7'.
          //      > Could not resolve :kotlin-native-prebuilt-linux-x86_64:1.9.23.
          //        Required by:
          //            project :kotlin-inject:impl
          //         > Could not resolve :kotlin-native-prebuilt-linux-x86_64:1.9.23.
          //            > Could not get resource
          // 'https://download.jetbrains.com/kotlin/native/builds/releases/1.9.23/linux-x86_64/kotlin-native-prebuilt-linux-x86_64-1.9.23.tar.gz'.
          //               > Could not HEAD
          // 'https://download.jetbrains.com/kotlin/native/builds/releases/1.9.23/linux-x86_64/kotlin-native-prebuilt-linux-x86_64-1.9.23.tar.gz'.
          //                  > download.jetbrains.com
          //
          // https://youtrack.jetbrains.com/issue/KT-47026/Use-gradle-dependency-resolution-for-konan-dependencies
          if (projectsUsingCompose.none { path.startsWith(it) }) {
            add(LinuxArm64(project = this@allPlatforms))
            add(LinuxX64(project = this@allPlatforms))
          }
        }
      }
    }
  }
}
