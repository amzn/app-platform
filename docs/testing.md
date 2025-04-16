# Testing

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
