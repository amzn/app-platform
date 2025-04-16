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

```kotlin title="ComposeRenderer sample"
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

```kotlin title="ViewRenderer sample"
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

A `Renderer` sends events back to the `Presenter` through the `onEvent` lambda on a Model. The model and presenter
from an earlier example looked like this:

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

`ComposeRendererFactory`

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

All implementations rely on our dependency injection framework kotlin-inject-anvil to discover and initialize
renderers.
