pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Template"

include(":app")
include(":navigation:impl")
include(":navigation:public")
include(":navigation:testing")
include(":templates:impl")
include(":templates:public")
