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
    `render(model)` function [is an error](https://github.com/amzn/app-platform/blob/39b30e35f5fcd04c8265e19c23c36bdda39fd803/renderer-compose-multiplatform/public/src/commonMain/kotlin/software/amazon/app/platform/renderer/ComposeRenderer.kt#L52-L58).
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

## `RendererFactory`

How `Renderers` are initialized depends on [`RendererFactory`](https://github.com/amzn/app-platform/blob/main/renderer/public/src/commonMain/kotlin/software/amazon/app/platform/renderer/RendererFactory.kt),
which only responsibility is to create and cache `Renderers` based on the given model. App Platform comes with three
different implementations:

[`ComposeRendererFactory`](https://github.com/amzn/app-platform/blob/main/renderer-compose-multiplatform/public/src/commonMain/kotlin/software/amazon/app/platform/renderer/ComposeRendererFactory.kt)

:   [`ComposeRendererFactory`](https://github.com/amzn/app-platform/blob/main/renderer-compose-multiplatform/public/src/commonMain/kotlin/software/amazon/app/platform/renderer/ComposeRendererFactory.kt)
    is an implementation for Compose Multiplatform and can be used on all supported platform. It can only create
    instances of `ComposeRenderer`.

`AndroidRendererFactory`

:   [`AndroidRendererFactory`](https://github.com/amzn/app-platform/blob/main/renderer-android-view/public/src/androidMain/kotlin/software/amazon/app/platform/renderer/AndroidRendererFactory.kt)
    is only suitable for Android. It can be used to create `ViewRenderer` instances and its subtypes. It does not
    support `ComposeRenderer`. Use `ComposeAndroidRendererFactory` if you need to mix and match `ViewRenderer` with
    `ComposeRenderer`.

`ComposeAndroidRendererFactory`

:   [`ComposeAndroidRendererFactory`](https://github.com/amzn/app-platform/blob/main/renderer-compose-multiplatform/public/src/androidMain/kotlin/software/amazon/app/platform/renderer/ComposeAndroidRendererFactory.kt)
    is only suitable for Android when using `ComposeRenderer` together with `ViewRenderer`. The factory wraps the
    Renderers for seamless interop.

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

The `RendererFactory` should be created and cached in the platform specific UI context, e.g. an Android `Activity` or
iOS `UIViewController`.

```kotlin title="Compose Multiplatform"
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

### Creating `Renderers`

Based on a `Model` instance or `Model` type a `RendererFactory` can create a new `Renderer` instance. The
`getRenderer()` function creates a `Renderer` only once and caches the instance after that. This makes the caller side
simpler. Whenever a new `Model` is available get the `Renderer` for the `Model` and render the content on screen.

```kotlin title="Compose Multiplatform"
fun mainViewController(rootScopeProvider: RootScopeProvider): UIViewController =
  ComposeUIViewController {
    // Only a single factory is needed.
    val rendererFactory = remember { ComposeRendererFactory(rootScopeProvider) }

    val model = presenter.present(Unit)

    val renderer = factory.getRenderer(model::class)
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
