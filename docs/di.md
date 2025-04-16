# DI Framework

!!! note

    App Platform uses [kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil) as default dependency
    injection framework. It's a compile-time injection
    framework and ready for Kotlin Multiplatform. Enabling dependency injection is an opt-in feature through the
    Gradle DSL. The default value is `false`.
    ```groovy
    appPlatform {
      enableKotlinInject true
    }
    ```

!!! tip

    Consider taking a look at the [kotlin-inject-anvil documentation](https://github.com/amzn/kotlin-inject-anvil)
    first. App Platform makes heavy use the of `@ContributesBinding` and `@ContributesTo` annotations to decompose
    and assemble components.

## Component

Components are added as a service to the `Scope` class and can be obtained using the `diComponent()` extension
function:

```kotlin
scope.diComponent<AppComponent>()
```

In modularized projects, final components are defined in the `:app` modules, because the object graph has to
know about all features of the app. It is strongly recommended to create a component in each platform specific
folder to provide platform specific types.

```kotlin title="androidMain"
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AndroidAppComponent(
  @get:Provides val application: Application,
  @get:Provides val rootScopeProvider: RootScopeProvider,
)
```

```kotlin title="iosMain"
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class IosAppComponent(
  @get:Provides val uiApplication: UIApplication,
  @get:Provides val rootScopeProvider: RootScopeProvider,
)
```

```kotlin title="desktopMain"
@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class DesktopAppComponent(
  @get:Provides val rootScopeProvider: RootScopeProvider
)
```
