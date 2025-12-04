# Change Log

## [Unreleased]

### Added

- Added a recipe for `Presenter` integration with SwiftUI, see #154.

### Changed

### Deprecated

### Removed

### Fixed

### Security

### Other Notes & Contributions

- Special thanks to [@rvenable](https://github.com/rvenable) for creating the original Swift APIs that served as the foundation for #154!

## [0.0.7] - 2025-09-26

### Changed

- Changed the min SDK from 21 to 23, see #149.

### Fixed

- Fix NPE when removing Android Views from multiple child renderers with the same parent on activity destruction, see #150.


## [0.0.6] - 2025-09-05

### Added

- Added support for [Metro](https://zacsweers.github.io/metro/) as dependency injection framework. User can choose between [`kotlin-inject-anvil`](https://github.com/amzn/kotlin-inject-anvil) and [Metro](https://zacsweers.github.io/metro/). For more details see the [documentation](https://amzn.github.io/app-platform/di/) for how to setup and use both dependency injection frameworks with App Platform.

### Changed

- Changed the provided `CoroutineScope` within `ViewRenderer` from a custom scope to `MainScope()`, see #124.
- Disallow changing the parent View for `ViewRenderers`. For a different parent view `RendererFactory.getRenderer()` will now return a new `Renderer` instead of the cached instance. The cached instance is only returned for the same parent view, see #139.

### Deprecated

- Deprecated `diComponent()` and introduce `kotlinInjectComponent()` as replacement, see #106.
- Deprecated `RendererFactory.getChildRendererForParent()`. `RendererFactory.getRenderer()` now provides the same functionality, see #139.

### Fixed

- Fix and stop suppressing NPE when removing Android Views, which lead to an inconsistent state and potential crashes laters, see #136.
- Cancel the `CoroutineScope` in `ViewRenderer` in rare cases where `onDetach` for the view isn't triggered. This caused potential leaks, see #140.


## [0.0.5] - 2025-08-15

### Added

- Added support for the new [Android-KMP library plugin](https://developer.android.com/kotlin/multiplatform/plugin) in App Platform's Gradle plugin.
- Added a [recipe](https://amzn.github.io/app-platform/presenter/#navigation-3) for how to use the Navigation 3 library with App Platform.

### Changed

- Upgraded Kotlin to `2.2.10`.


## [0.0.4] - 2025-07-25

### Added

- Added a search field to the wiki.
- Added a [blueprint project](https://github.com/amzn/app-platform/tree/main/blueprints/starter) for App Platform that can be copied to spin up new projects faster, see #63.
- Added support for back press events in `Presenters`. The API is similar to the one from Compose Multiplatform and Android Compose. See the [documentation in the wiki](https://amzn.github.io/app-platform/presenter/#back-gestures) for more details.
- Added a [recipes application](https://amzn.github.io/app-platform/#web-recipe-app) showing solutions to common problems. All solutions have been [documented in the wiki](https://amzn.github.io/app-platform/presenter/#recipes).

### Changed

- Upgraded Kotlin to `2.2.0`.


## [0.0.3] - 2025-05-28

### Added

- Wasm JS is now officially supported and artifacts are published.

### Changed

- Snapshots are now published to the Central Portal Snapshots repository at https://central.sonatype.com/repository/maven-snapshots/.
- Upgraded Kotlin to `2.1.21`.

### Removed

- Removed the deprecated `onEvent` function used in `MoleculePresenters`. This is no longer needed since Kotlin 2.0.20, see #21.


## [0.0.2] - 2025-05-02

### Changed

- **Breaking change:** Changed the constructor from `ComposeAndroidRendererFactory` to two factory functions instead. A new API allows you to use this factory without an Android View as parent, see #39.

### Deprecated

- Deprecated the `onEvent` function used in `MoleculePresenters`. This is no longer needed since Kotlin 2.0.20, see #21.

### Fixed

- Made the `ModuleStructureDependencyCheckTask` cacheable, see #19.
- Fixed violations for Gradle's project isolation feature, see #20.

### Other Notes

- Updated the sample application with a shared transition animation to highlight how animations can be implemented for `Template` updates, see #37.


## [0.0.1] - 2025-04-17

- Initial release.

[Unreleased]: https://github.com/amzn/app-platform/compare/0.0.7...HEAD
[0.0.7]: https://github.com/amzn/app-platform/compare/0.0.7
[0.0.6]: https://github.com/amzn/app-platform/compare/0.0.6
[0.0.5]: https://github.com/amzn/app-platform/compare/0.0.5
[0.0.4]: https://github.com/amzn/app-platform/compare/0.0.4
[0.0.3]: https://github.com/amzn/app-platform/compare/0.0.3
[0.0.2]: https://github.com/amzn/app-platform/compare/0.0.2
[0.0.1]: https://github.com/amzn/app-platform/compare/0.0.1
