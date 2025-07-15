@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import software.amazon.app.platform.gradle.AppPlatformPlugin

plugins {
  alias(libs.plugins.appPlatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeHotReload)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

appPlatform {
  enableComposeUi(true)
  enableKotlinInject(true)
  enableModuleStructure(true)
  enableMoleculePresenters(true)
  addImplModuleDependencies(true)
}

kotlin {
  jvm("desktop") {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  androidTarget {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  targets.withType<KotlinNativeTarget>().configureEach {
    binaries.framework {
      baseName = "TemplateApp"
      AppPlatformPlugin.exportedDependencies().forEach { export(it) }
    }
  }

  wasmJs {
    binaries.executable()

    browser {
      commonWebpackConfig {
        outputFileName = "template-app.js"
      }
      outputModuleName = project.name.replace("-", "")
    }
  }

  sourceSets {
    val desktopMain by getting
    commonMain {
      dependencies {
        implementation(libs.androidx.lifecycle.viewmodel)

        implementation(project(":navigation:impl"))
        implementation(project(":navigation:public"))
        implementation(project(":templates:impl"))
        implementation(project(":navigation:public"))

        AppPlatformPlugin.exportedDependencies().forEach { api(it) }
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.activity.compose)
      }
    }

    desktopMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.coroutines.swing)
    }
  }
}

android {
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "software.amazon.app.platform.template"
    versionCode = 1
    versionName = "1.0"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
}

compose.desktop {
  application {
    mainClass = "software.amazon.app.platform.template.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "TemplateApp"
      packageVersion = "1.0.0"
    }
  }
}
