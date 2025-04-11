package software.amazon.app.platform.gradle.buildsrc

import kotlin.math.max
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.internal.VersionNumber
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import software.amazon.app.platform.gradle.AppPlatformPlugin
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.composeMultiplatform
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.kmpExtension
import software.amazon.app.platform.gradle.isAppModule
import software.amazon.app.platform.gradle.isRobotsModule
import software.amazon.app.platform.gradle.isTestingModule

public open class AppPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(Plugins.ANDROID_APP)

    target.plugins.apply(BasePlugin::class.java)
    target.plugins.apply(KmpPlugin::class.java)
    target.plugins.apply(BaseAndroidPlugin::class.java)

    target.configureAndroidSettings()
    target.makeSingleVariant()
    target.addDependencies()

    target.plugins.withId(Plugins.COMPOSE_MULTIPLATFORM) { target.configureDesktopApp() }
  }

  private fun Project.configureAndroidSettings() {
    android.defaultConfig.minSdk = 26
  }

  private fun Project.makeSingleVariant() {
    // Disable the release build type in the app module. We only need one build type
    // and everything else is overhead.
    androidComponents.beforeVariants { variant ->
      if (variant.buildType != "debug") {
        variant.enable = false
      }
    }
  }

  private fun Project.addDependencies() {
    // iOS exports these dependencies for the iOS Framework and requires them to be added as
    // "api" dependency to the project.
    allExportedDependencies().forEach { dependency ->
      kmpExtension.sourceSets.getByName("commonMain").dependencies { api(dependency) }
    }
  }

  internal companion object {
    fun Project.allExportedDependencies(): Set<Any> {
      return AppPlatformPlugin.exportedDependencies()
        .plus(
          project
            .project(":sample")
            .subprojects
            .filter { it.subprojects.isEmpty() }
            .filter { !it.isRobotsModule() && !it.isTestingModule() && !it.isAppModule() }
        )
    }

    fun Project.configureDesktopApp() {
      composeMultiplatform.extensions.getByType(DesktopExtension::class.java).application.apply {
        mainClass = "software.amazon.app.platform.sample.MainKt"

        nativeDistributions.targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
        nativeDistributions.packageName = "software.amazon.app.platform.demo"

        // During development the major version is 0, e.g. '0.0.1'. DMG must use a
        // major version equal or greater than 1:
        //
        // Illegal version for 'Dmg': '0.0.1' is not a valid build version.
        val version = VersionNumber.parse(versionName)
        nativeDistributions.packageVersion =
          VersionNumber(max(1, version.major), version.minor, version.patch, null).toString()
      }
    }
  }
}
