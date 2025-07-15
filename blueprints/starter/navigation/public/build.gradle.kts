@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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

  targets.withType<KotlinNativeTarget>().configureEach {
    binaries.framework {
      baseName = project.name.replace("-", "").replaceFirstChar { it.uppercase() }
    }
  }

  wasmJs {
    binaries.executable()

    browser {
      commonWebpackConfig {
        outputFileName = "${project.name}.js"
      }
      outputModuleName = project.name.replace("-", "")
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
      }
    }
  }
}

android {
  namespace = "software.amazon.app.platform.template.navigation"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions {
    targetSdk = libs.versions.android.targetSdk.get().toInt()
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  packaging {
    resources {
      excludes += listOf("/META-INF/{AL2.0,LGPL2.1}")
    }
  }
}
