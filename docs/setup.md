# Setup

## Gradle

App Platform, its various features and dependencies are all configured through a Gradle plugin. All settings
are opt-in
```groovy
plugins {
  id 'software.amazon.app.platform' version 'x.y.z'
}

appPlatform {
  // false by default. Adds dependencies on the APIs for scopes, presenters and renderers in order to use the App Platform.
  addPublicModuleDependencies true

  // false by default. Helpful for final application modules that must consume concrete implementations and not only APIs.
  addImplModuleDependencies true

  // false by default. Configures KSP and adds the kotlin-inject-anvil library as dependency.
  enableKotlinInject true

  // false by default. Configures Molecule and provides access to the MoleculePresenter API.
  enableMoleculePresenters true

  // false by default. Adds the necessary dependencies to use Compose Multiplatform with Renderers.
  enableComposeUi true

  // false by default. Verifies that this module follows conventions for our module structure and
  // adds default dependencies. For Android projects it sets the namespace to avoid conflicts.
  enableModuleStructure true
}
```
The various options are explained in more detail in many of the following sections.

## Snapshot

To import snapshot builds use following repository:

```gradle
maven {
  url = 'https://aws.oss.sonatype.org/content/repositories/snapshots/'
}
```
