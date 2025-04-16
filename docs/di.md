# DI Framework

!!! note

    App Platform uses [kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil) as default dependency
    injection framework. It's a compile-time injection framework and ready for Kotlin Multiplatform. It verifies
    correctness of the object graph at build time and avoids crashes at runtime.

    Enabling dependency injection is an opt-in feature through the Gradle DSL. The default value is `false`.
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

## Platform implementations

`kotlin-inject-anvil` makes it simple to provide platform specific implementations for abstract APIs without needing
to use `expect / actual` declarations or any specific wiring. Since the final components live in the platform specific
source folders, all contributions for a platform are automatically picked up. Platform specific implementations can
use and inject types from the platform.

```kotlin title="commonMain"
interface LocationProvider
```

```kotlin title="androidMain"
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidLocationProvider(
  val application: Application,
) : LocationProvider
```

```kotlin title="iosMain"
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosLocationProvider(
  val uiApplication: UIApplication,
) : LocationProvider
```

Other common code within `commonMain` can safely inject and use `LocationProvider`.

## Injecting dependencies

It's recommended to rely on constructor injection as much as possible, because it removes boilerplate and makes
testing easier. But it some cases it's required to get a dependency from a component where constructor injection
is not possible, e.g. in a static context or types created by the platform. In this case a contributed component
interface with access to the `Scope` help:

```kotlin
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

  private val component = (application as RootScopeProvider).rootScope.diComponent<Component>()
  private val templateProvider = component.templateProviderFactory.createTemplateProvider()

  @ContributesTo(AppScope::class)
  interface Component {
    val templateProviderFactory: TemplateProvider.Factory
  }
}
```

This sample shows an Android `ViewModel` that doesn't use constructor injection. Instead, the `Scope` is retrieved
from the `Application` class and the `kotlin-inject-anvil` component is found through the `diComponent()` function.

??? example "Sample"

    The `ViewModel` example comes from the [sample app](https://github.com/amzn/app-platform/blob/main/sample/app/src/androidMain/kotlin/software/amazon/app/platform/sample/MainActivityViewModel.kt).
    `ViewModels` can use constructor injection, but this requires more setup. This approach of using a component
    interface was simpler and faster.

    Another example where this approach is handy is in [`NavigationPresenterImpl`](https://github.com/amzn/app-platform/blob/main/sample/navigation/impl/src/commonMain/kotlin/software/amazon/app/platform/sample/navigation/NavigationPresenterImpl.kt).
    This class waits for the user scope to be available and then optionally retrieves the `Presenter` that is part
    of the user component. Constructor injection cannot be used, because `NavigationPresenterImpl` is part of the app
    scope and cannot inject dependencies from the user scope, which is a child scope of app scope. This would violate
    dependency inversion rules.

    ```kotlin hl_lines="17"
    @ContributesTo(UserScope::class)
    interface UserComponent {
      val userPresenter: UserPagePresenter
    }

    @Composable
    override fun present(input: Unit): BaseModel {
      val scope = getUserScope()
      if (scope == null) {
        // If no user is logged in, then show the logged in screen.
        val presenter = remember { loginPresenter() }
        return presenter.present(Unit)
      }

      // A user is logged in. Use the user component to get an instance of UserPagePresenter, which is only
      // part of the user scope.
      val userPresenter = remember(scope) { scope.diComponent<UserComponent>().userPresenter }
      return userPresenter.present(Unit)
    }
    ```

## Default bindings

App Platform provides a few defaults that can be injected, including a `CoroutineScope` and `CoroutineDispatchers`.

```kotlin
@Inject
class SampleClass(
  @ForScope(AppScope::class) appScope: CoroutineScope,

  @IoCoroutineDispatcher ioDispatcher: CoroutineDispatcher,
  @DefaultCoroutineDispatcher defaultDispatcher: CoroutineDispatcher,
  @MainCoroutineDispatcher mainDispatcher: CoroutineDispatcher,
)
```

!!! info "CoroutineScope"

    The `CoroutineScope` uses the IO dispatcher by default. The qualifier `@ForScope(AppScope::class)` is needed to
    allow other scopes to have their own `CoroutineScope`. For example, the sample app provides a `CoroutineScope`
    [for the user scope](https://github.com/amzn/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/amazon/app/platform/sample/user/UserComponent.kt),
    which gets canceled when the user scope gets destroyed. The `CoroutineScope` for the user scope uses the qualifier
    `@ForScope(UserScope::class)

    ```kotlin
    /**
     * Provides the [CoroutineScopeScoped] for the user scope. This is a single instance for the user
     * scope.
     */
    @Provides
    @SingleIn(UserScope::class)
    @ForScope(UserScope::class)
    fun provideUserScopeCoroutineScopeScoped(
      @IoCoroutineDispatcher dispatcher: CoroutineDispatcher
    ): CoroutineScopeScoped {
      return CoroutineScopeScoped(dispatcher + SupervisorJob() + CoroutineName("UserScope"))
    }

    /**
     * Provides the [CoroutineScope] for the user scope. A new child scope is created every time an
     * instance is injected so that the parent cannot be canceled accidentally.
     */
    @Provides
    @ForScope(UserScope::class)
    fun provideUserCoroutineScope(
      @ForScope(UserScope::class) userScopeCoroutineScopeScoped: CoroutineScopeScoped
    ): CoroutineScope {
      return userScopeCoroutineScopeScoped.createChild()
    }
    ```

!!! info "CoroutineDispatcher"

    It's recommended to inject `CoroutineDispatcher` through the constructor instead of using `Dispatcher.*`. This
    allows to easily swap them within unit tests to remove concurrency and improve stability.
