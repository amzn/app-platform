@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.appPlatform)
  alias(libs.plugins.androidKmpLibrary)
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

  androidLibrary {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

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
      }
    }
  }
}
