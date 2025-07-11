# Template App for Amazon App Platform

This is a Kotlin Multiplatform template application built using the [Amazon App Platform](https://github.com/amzn/app-platform). It provides a modern, opinionated starting point for building scalable, testable, and multiplatform Compose applications.

## 🚀 Overview

This template demonstrates:

- ✅ Kotlin Multiplatform targeting Android, iOS, and WebAssembly (WASM)
- ✅ [App Platform](https://github.com/amzn/app-platform) conventions for DI, state, rendering, and navigation
- ✅ Molecule-powered presenters
- ✅ Scoped dependency injection using `@ContributesBinding`, `@SingleIn`, and `@ContributesRenderer`
- ✅ Reactive state with `StateFlow`
- ✅ Compose UI for Android and WASM
- ✅ Modular code structure for feature separation

## 🧱 Features

- `ExampleRepository`: A simple `StateFlow`-based repository that emits data
- `ExampleValueGenerator`: A scoped class that updates the repository with random values every 3 seconds
- `NavigationHeaderPresenter` and `NavigationDetailPresenter`: Molecule presenters driving the top bar and content UI
- `NavigationHeaderRenderer` and `NavigationDetailRenderer`: A ComposeRenderer showing example state

## 📦 Modules

- `:app` – Main app entrypoint using Compose + App Platform
- `:templates` - Main module for templates and the entry point into the application
- `:navigation` – Example feature module

## 🧪 Running the App

### Android

```bash
./gradlew :app:installDebug
```

### WASM (WebAssembly)

```bash
./gradlew :app:wasmJsBrowserDevelopmentRun
```

### iOS

#### Option 1: Run via Xcode (Recommended)

1. Open the Xcode project:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

2. Select a simulator and run the app (`Cmd + R`)

> 💡 The required Kotlin Multiplatform framework will be built automatically as part of the Xcode build process (`./gradlew :app:embedAndSignAppleFrameworkForXcode`).

Then open the browser link shown in the terminal.

## 🔧 Configuration

You can modify app behavior by editing:

- `gradle.properties` – JVM and native memory settings
- `libs.versions.toml` – Centralized dependency version catalog
- `app/build.gradle` – Platform-specific targets and UI modules

## 🤝 Contributing

Feel free to fork and adapt this template for your own projects. If you find bugs or improvements related to App Platform usage, consider opening issues or PRs against [amzn/app-platform](https://github.com/amzn/app-platform).

## 📄 License

This project inherits the license of the [Amazon App Platform](https://github.com/amzn/app-platform).
