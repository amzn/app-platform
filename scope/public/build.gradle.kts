plugins {
  id("com.android.library").version("8.12.0")
  id("org.jetbrains.kotlin.multiplatform").version("2.2.0")
}

kotlin {
  androidTarget()

  sourceSets {
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
      }
    }
  }
}

android {
  namespace = "some.namespace"
  compileSdk = 36

  defaultConfig.minSdk = 36

  testOptions.unitTests.isReturnDefaultValues = true
}
