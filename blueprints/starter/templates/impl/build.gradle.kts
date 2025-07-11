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
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType(KotlinNativeTarget::class).configureEach {
        binaries.framework {
            baseName = project.name.replace("-", "").replaceFirstChar(Char::uppercaseChar)
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
                implementation(libs.compose.material)
            }
        }
    }
}

android {
    namespace = "software.amazon.app.platform.template.templates.impl"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packagingOptions {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}
