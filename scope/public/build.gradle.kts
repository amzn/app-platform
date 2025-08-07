import com.android.build.api.dsl.androidLibrary

plugins {
  id("com.android.kotlin.multiplatform.library").version("8.12.0")
  id("org.jetbrains.kotlin.multiplatform").version("2.2.0")
}

kotlin {
  androidLibrary {
    namespace = "some.namespace"
    compileSdk = 36
    minSdk = 36

    withHostTest {
      isReturnDefaultValues = true
    }
  }

  sourceSets {
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
      }
    }
  }
}
