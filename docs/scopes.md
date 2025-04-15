# Scopes

!!! note

    Importing the `Scopes` API is an opt-in feature through the Gradle DSL. The default value is `false`.
    ```groovy
    appPlatform {
      addPublicModuleDependencies true
    }
    ```

## Overview

Scopes define the boundary our software components operate in. A scope is a space with a well-defined lifecycle
that can be created and torn down. Scopes host other objects and can bind them to their lifecycle. Sub-scopes
or child scopes have the same or a shorter lifecycle as their parent scope.

A leak happens when one scope references another scope with a different lifecycle, e.g. a background thread,
which is started and finishes after a certain amount of time, references an Android `Activity` that is being
destroyed while the thread is still running. In this case the thread with the longer lifecycle leaks the
`Activity` with the shorter lifecycle. Another example is a singleton object, which lives as long as the
application process runs, keeping a strong reference to a user object, which should be released after the
user session expires.

Relying purely on platform specific scopes is problematic, because these scopes are out of our control.
When the platform decides to destroy one of its scopes, then we need to adjust and tear down our operations.
This doesn’t always align with our use cases, e.g. we might want to finish uploading data in the background
after the platform scope such as an `Activity` has been destroyed. Further, the platform scopes may not align
with how we'd represent logical scopes for our apps, e.g. they often lack a user scope. This forces us to
push objects and lifecycles into the application scope and this could cause data to leak across sessions and
trigger out of memory scenarios.

We need to be in charge of our own scopes. In simple terms this means having an object that can be created and
destroyed.

The App Platform provides the
[Scope](https://github.com/amzn/app-platform/blob/main/scope/public/src/commonMain/kotlin/software/amazon/app/platform/scope/Scope.kt)
interface to implement this concept.

```kotlin title="Scope.kt"
interface Scope {

  val name: String
  val parent: Scope?

  fun buildChild(name: String, builder: (Builder.() -> Unit)? = null): Scope
  fun children(): Set<Scope>

  fun isDestroyed(): Boolean
  fun destroy()

  fun register(scoped: Scoped)
  fun <T : Any> getService(key: String): T?
}
```

## Creating a `Scope`

A `Scope` is created through the builder function. The
[Builder](https://github.com/amzn/app-platform/blob/274ff2261edb13127dbf0b20429fa42970d62cb8/scope/public/src/commonMain/kotlin/software/amazon/app/platform/scope/Scope.kt#L57)
allows you to add services before the Scope is finalized:

```kotlin
val rootScope = Scope.buildRootScope {
  addService("key", service)
}
```

Child scopes are created using the parent:

```kotlin
rootScope.buildChild("user scope") {
  addService("child-service", childService)
}
```

Tests usually leverage the test scope, which comes with better defaults for services such as the coroutine scope:

```kotlin
@Test
fun `my test`() = runTest {
  val scope = Scope.buildTestScope(this)
}

// Or
@Test
fun `my test`() = runTestWithScope { scope ->
  // `scope` is equivalent to calling `Scope.buildTestScope(this)`.
}
```

## Services
