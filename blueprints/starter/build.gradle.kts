import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension
import org.jetbrains.kotlin.gradle.targets.web.yarn.CommonYarnPlugin

plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidKmpLibrary) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.composeMultiplatform) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.metro) apply false
  alias(libs.plugins.appPlatform)
}

plugins.withType(CommonYarnPlugin::class.java).configureEach {
  with(extensions.getByType(BaseYarnRootExtension::class.java)) {
    resolution("webpack-dev-server", "5.2.5")
    resolution("fast-uri", "3.1.2")
    resolution("picomatch", "2.3.2")
    resolution("path-to-regexp", "0.1.13")
    resolution("ws", "8.21.0")
    resolution("ajv", "8.20.0")
    resolution("qs", "6.15.2")
  }
}
