@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.appPlatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinMultiplatform)
}

appPlatform {
  enableComposeUi(true)
  enableModuleStructure(true)
  enableKotlinInject(true)
  enableMoleculePresenters(true)
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

  wasmJs {
    browser {
      outputModuleName = project.name.replace("-", "")
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.compose.material)
        implementation(libs.compose.material.icons)
        implementation(project(":templates:public"))
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.assertk)
        implementation(libs.coroutines.test)
        implementation(project(":navigation:testing"))
      }
    }
  }
}

android {
  namespace = "software.amazon.app.platform.template.navigation.impl"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
  }

  testOptions.unitTests {
    isReturnDefaultValues = true
  }
}
