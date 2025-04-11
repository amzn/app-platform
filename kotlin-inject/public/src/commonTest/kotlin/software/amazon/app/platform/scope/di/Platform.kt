package software.amazon.app.platform.scope.di

enum class Platform {
  JVM,
  Native,
}

expect val platform: Platform
