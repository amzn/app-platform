# Renderer

!!! note

    App Platform has a generic `Renderer` interface that can be used for multiple UI layer implementations.
    Compose Multiplatform and Android Views are stable and supported out of the box. However, Compose Multiplatform is
    an opt-in feature through the Gradle DSL and must be explicitly enabled. The default value is `false`.

    ```groovy
    appPlatform {
      enableComposeUi true
    }
    ```

## Renderer basics

A [`Renderer`](https://github.com/amzn/app-platform/blob/main/renderer/public/src/commonMain/kotlin/software/amazon/app/platform/renderer/Renderer.kt)
is the counterpart to a `Presenter`. It consumes `Models` and turns them into UI, which is shown on screen.

```kotlin
interface Renderer<in ModelT : BaseModel> {
  fun render(model: ModelT)
}
```

The `Renderer` interface is rarely used directly, instead platform specific implementations like
[`ComposeRenderer`](https://github.com/amzn/app-platform/blob/main/renderer-compose-multiplatform/public/src/commonMain/kotlin/software/amazon/app/platform/renderer/ComposeRenderer.kt)
for [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) and
[`ViewRenderer`](https://github.com/amzn/app-platform/blob/main/renderer-android-view/public/src/androidMain/kotlin/software/amazon/app/platform/renderer/ViewRenderer.kt)
for Android are used. App Platform doesn’t provide any other implementations for now, e.g. a SwiftUI or UIKit
implementation for iOS is missing.

```kotlin title="ComposeRenderer"
@ContributesRenderer
class LoginRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model) {
    if (model.loginInProgress) {
      CircularProgressIndicator()
    } else {
      Text("Login")
    }
  }
}
```

```kotlin title="ViewRenderer"
@ContributesRenderer
class LoginRenderer : ViewRenderer<Model>() {
    private lateinit var textView: TextView

    override fun inflate(
        activity: Activity,
        parent: ViewGroup,
        layoutInflater: LayoutInflater,
        initialModel: Model,
    ): View {
        return TextView(activity).also { textView = it }
    }

    override fun renderModel(model: Model) {
        textView.text = "Login"
    }
}
```

!!! warning

    Note that `ComposeRenderer` like `ViewRenderer` implements the common `Renderer` interface, but calling the
    `render(model)` function [is an error](https://github.com/amzn/app-platform/blob/0f3e242ae08bb242fbd7080d33caa069c8fae2b4/renderer-compose-multiplatform/public/src/commonMain/kotlin/software/amazon/app/platform/renderer/ComposeRenderer.kt#L52-L58).
    Instead, `ComposeRenderer` defines its own function to preserve the composable context:

    ```kotlin
    @Composable
    fun renderCompose(model: ModelT)
    ```

    In practice this is less of a concern, because the `render(model)` function is deprecated and hidden and callers
    only see the `renderCompose(model)` function.

Renderers are composable and can build hierarchies similar to `Presenters`. The parent renderer is responsible for
calling `render()` on the child renderer:

```kotlin
data class ParentModel(
  val childModel: ChildModel
): BaseModel

class ParentRenderer(
  private val childRenderer: ChildRenderer
): Renderer<ParentModel> {
  override fun render(model: ParentModel) {
    childRenderer.render(model.childModel)
  }
}
```

!!! note

    Injecting concrete child `Renderers` is possible, but less common. More frequently `RendererFactory` is injected
    to obtain a `Renderer` instance for a `Model`.

A `Renderer` sends events back to the `Presenter` through the `onEvent` lambda on a Model.

```kotlin hl_lines="6"
@ContributesRenderer
class LoginRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model) {
    Button(
      onClick = { model.onEvent(LoginPresenter.Event.Login("Demo")) },
    ) {
      Text("Login")
    }
  }
}
```

??? example "Sample"

    The sample app implements multiple `ComposeRenderers`, e.g. [`LoginRenderer`](https://github.com/amzn/app-platform/blob/main/sample/login/impl/src/commonMain/kotlin/software/amazon/app/platform/sample/login/LoginRenderer.kt),
    [`UserPageListRenderer`](https://github.com/amzn/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/amazon/app/platform/sample/user/UserPageListRenderer.kt)
    and [`UserPageDetailRenderer`](https://github.com/amzn/app-platform/blob/main/sample/user/impl/src/commonMain/kotlin/software/amazon/app/platform/sample/user/UserPageDetailRenderer.kt).

## `RendererFactory`

How `Renderers` are initialized depends on [`RendererFactory`](https://github.com/amzn/app-platform/blob/main/renderer/public/src/commonMain/kotlin/software/amazon/app/platform/renderer/RendererFactory.kt),
which only responsibility is to create and cache `Renderers` based on the given model. App Platform comes with three
different implementations:

[`ComposeRendererFactory`](https://github.com/amzn/app-platform/blob/main/renderer-compose-multiplatform/public/src/commonMain/kotlin/software/amazon/app/platform/renderer/ComposeRendererFactory.kt)

:   `ComposeRendererFactory` is an implementation for Compose Multiplatform and can be used on all supported
    platforms. It can only create instances of `ComposeRenderer`.

[`AndroidRendererFactory`](https://github.com/amzn/app-platform/blob/main/renderer-android-view/public/src/androidMain/kotlin/software/amazon/app/platform/renderer/AndroidRendererFactory.kt)

:   `AndroidRendererFactory` is only suitable for Android. It can be used to create `ViewRenderer` instances and its
    subtypes. It does not support `ComposeRenderer`. Use `ComposeAndroidRendererFactory` if you need to mix and
    match `ViewRenderer` with `ComposeRenderer`.

[`ComposeAndroidRendererFactory`](https://github.com/amzn/app-platform/blob/main/renderer-compose-multiplatform/public/src/androidMain/kotlin/software/amazon/app/platform/renderer/ComposeAndroidRendererFactory.kt)

:   `ComposeAndroidRendererFactory` is only suitable for Android when using `ComposeRenderer` together with
    `ViewRenderer`. The factory wraps the Renderers for seamless interop.

### `@ContributesRenderer`

All factory implementations rely on the dependency injection framework kotlin-inject-anvil to discover and initialize
renderers. When the factory is created, it builds the `RendererComponent`, which parent is the app component.
The `RendererComponent` lazily provides all renderers using the multibindings feature. To participate in the lookup,
renderers must tell kotlin-inject-anvil which models they can render. This is done through a component interface,
which automatically gets generated and added to the renderer scope by using the
[`@ContributesRenderer` annotation](https://github.com/amzn/app-platform/blob/main/kotlin-inject-extensions/contribute/public/src/commonMain/kotlin/software/amazon/app/platform/inject/ContributesRenderer.kt).

Which `Model` type is used for the binding is determined based on the super type. In the following example
`LoginPresenter.Model` is used.

```kotlin
@ContributesRenderer
class LoginRenderer : ComposeRenderer<LoginPresenter.Model>()
```

??? info "Generated code"

    The `@ContributesRenderer` annotation generates following code.

    ```kotlin
    @ContributesTo(RendererScope::class)
    interface LoginRendererComponent {
      @Provides
      public fun provideSoftwareAmazonAppPlatformSampleLoginLoginRenderer(): LoginRenderer = LoginRenderer()

      @Provides
      @IntoMap
      public fun provideSoftwareAmazonAppPlatformSampleLoginLoginRendererLoginPresenterModel(renderer: () -> LoginRenderer): Pair<KClass<out BaseModel>, () -> Renderer<*>> = LoginPresenter.Model::class to renderer

      @Provides
      @IntoMap
      @ForScope(scope = RendererScope::class)
      public fun provideSoftwareAmazonAppPlatformSampleLoginLoginRendererLoginPresenterModelKey(): Pair<KClass<out BaseModel>, KClass<out Renderer<*>>> = LoginPresenter.Model::class to LoginRenderer::class
    }
    ```

### Creating `RendererFactory`

The `RendererFactory` should be created and cached in the platform specific UI context, e.g. an iOS `UIViewController`
or Android `Activity`.

```kotlin title="iOS Compose Multiplatform"
fun mainViewController(rootScopeProvider: RootScopeProvider): UIViewController =
  ComposeUIViewController {
    // Only a single factory is needed.
    val rendererFactory = remember { ComposeRendererFactory(rootScopeProvider) }
    ...
  }
```

```kotlin title="Android Activity"
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val rendererFactory =
      ComposeAndroidRendererFactory(
        rootScopeProvider = application as RootScopeProvider,
        activity = this,
        parent = findViewById(R.id.main_container),
      )
    ...
  }
}
```

??? example "Sample"

    The sample app uses `ComposeAndroidRendererFactory` in [Android application](https://github.com/amzn/app-platform/blob/0f3e242ae08bb242fbd7080d33caa069c8fae2b4/sample/app/src/androidMain/kotlin/software/amazon/app/platform/sample/MainActivity.kt#L30-L35)
    and `ComposeRendererFactory` for [iOS](https://github.com/amzn/app-platform/blob/0f3e242ae08bb242fbd7080d33caa069c8fae2b4/sample/app/src/iosMain/kotlin/software/amazon/app/platform/sample/MainViewController.kt#L40)
    and [Desktop](https://github.com/amzn/app-platform/blob/0f3e242ae08bb242fbd7080d33caa069c8fae2b4/sample/app/src/desktopMain/kotlin/software/amazon/app/platform/sample/DesktopApp.kt#L36).

### Creating `Renderers`

Based on a `Model` instance or `Model` type a `RendererFactory` can create a new `Renderer` instance. The
`getRenderer()` function creates a `Renderer` only once and caches the instance after that. This makes the caller side
simpler. Whenever a new `Model` is available get the `Renderer` for the `Model` and render the content on screen.

```kotlin title="iOS Compose Multiplatform"
fun mainViewController(rootScopeProvider: RootScopeProvider): UIViewController =
  ComposeUIViewController {
    // Only a single factory is needed.
    val rendererFactory = remember { ComposeRendererFactory(rootScopeProvider) }

    val model = presenter.present(Unit)

    val renderer = factory.getComposeRenderer(model)
    renderer.renderCompose(model)
  }
```

!!! note

    Note that `getRenderer()` for `ComposeRendererFactory` returns a `ComposeRenderer`. For a `ComposeRenderer` the
    `renderCompose(model)` function must be called and not `render(model)`.

```kotlin title="Android Activity"
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val rendererFactory = ComposeAndroidRendererFactory(...)
    val models: StateFlow<Model> = ...
    ...

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        models.collect { model ->
          val renderer = rendererFactory.getRenderer(model)
          renderer.render(model)
        }
      }
    }
  }
}
```

### Injecting `RendererFactory`

The `RendererFactory` is provided in the `RendererComponent`, meaning it can be injected by any `Renderer`. This
allows you to create child renderers without knowing the concrete type of the model and injecting the child
renderers ahead of time:

```kotlin
@Inject
@ContributesRenderer
class SampleRenderer(
  private val rendererFactory: RendererFactory
) : ComposeRenderer<Model>() {

  @Composable
  override fun Compose(model: Model) {
    val childRenderer = rendererFactory.getComposeRenderer(model.childModel)
    childRenderer.renderCompose(model.childModel)
  }
}
```

??? example "Sample"

    The sample app injects `RendererFactory` in [`ComposeSampleAppTemplateRenderer`](https://github.com/amzn/app-platform/blob/main/sample/templates/impl/src/commonMain/kotlin/software/amazon/app/platform/sample/template/ComposeSampleAppTemplateRenderer.kt)
    to create `Renderers` dynamically for unknown `Model` types. There is also an [Android sample implementation](https://github.com/amzn/app-platform/blob/main/sample/templates/impl/src/androidMain/kotlin/software/amazon/app/platform/sample/template/AndroidSampleAppTemplateRenderer.kt).

!!! note

    Whenever a `Renderer` has an injected constructor parameter like `rendererFactory` in the sample above, then
    the class must be annotated with `@Inject` in addition to `@ContributesRenderer`.

## Android support

Android Views are supported out of the box using `ViewRenderer`.

### Compose interop

If an Android app uses only Compose UI with `ComposeRenderer`, then it can use `ComposeRendererFactory` similar to
iOS and Desktop to create `ComposeRenderer` instances. However, if interop with Android Views is needed, then
`ComposeAndroidRendererFactory` must be used. `ComposeAndroidRendererFactory` makes it transparent which `Renderer`
implementation is used and interop is seamless. A `ComposeRenderer` that has a child `ViewRenderer` wraps the Android
view within a `AndroidView` composable function call. A `ViewRenderer` that has a child `ComposeRenderer` wraps the
Compose UI within a `ComposeView` Android View.

```kotlin
val rendererFactory = ComposeAndroidRendererFactory(...)

val renderer = rendererFactory.getRenderer(model)
render.render(model)
```

In this example the returned `Renderer` can be a `ComposeRenderer` or `ViewRenderer`, it would not matter and either
the Compose UI or Android Views would be rendered on screen. With the seamless interop it becomes easier to migrate
from Android Views to Compose UI by simply migrating renderers one by one.

### `ViewRenderer` subtypes

[`ViewBindingRenderer`](https://github.com/amzn/app-platform/blob/main/renderer-android-view/public/src/androidMain/kotlin/software/amazon/app/platform/renderer/ViewBindingRenderer.kt).

:   View binding is supported out of the box using `ViewBindingRenderer`.

[`RecyclerViewViewHolderRenderer`](https://github.com/amzn/app-platform/blob/main/renderer-android-view/public/src/androidMain/kotlin/software/amazon/app/platform/renderer/RecyclerViewViewHolderRenderer.kt)

:   `RecyclerViewViewHolderRenderer` allows you to implement elements of a `RecyclerView` as a `Renderer`.

## Unit tests

`ComposeRenderer` can easily be tested as unit tests on Desktop and iOS. In particular tests for Desktop are helpful
due to the fast build times. Various fake `Models` can be passed to the `Renderer` and the UI state based on the
model verified.

Testing `ComposeRenderer` or `ViewRenderer` for Android requires an Android device or emulator.

This test runs as a unit test on iOS and Desktop.

```kotlin
class LoginRendererTest {

  @Test
  fun `the login button is rendered when not logging in`() {
    runComposeUiTest {
      setContent {
        val renderer = LoginRenderer()
        renderer.renderCompose(LoginPresenter.Model(loginInProgress = false) {})
      }

      onNodeWithTag("loginProgress").assertDoesNotExist()
      onNodeWithTag("loginButton").assertIsDisplayed()
    }
  }
}
```

??? example "Sample"

    The sample app demonstrates this with the [`LoginRendererTest`](https://github.com/amzn/app-platform/blob/main/sample/login/impl/src/appleAndDesktopTest/kotlin/software/amazon/app/platform/sample/login/LoginRendererTest.kt).
    To avoid duplicating the test in the `desktopTest` and `iosTest` source folders, the sample app has a custom
    source set `appleAndDesktop`, which is a shared parent source set for `apple` and `desktop`.
