# Testing

A fundamental design pattern to make testing effective is dependency inversion, which means that high-level
APIs don’t depend on low-level details and low-level details only import other high-level APIs.
It significantly reduces coupling between components.

App Platform implements the pattern in its [module structure](module-structure.md#gradle-modules) and in
[Kotlin code](module-structure.md#kotlin-code). By relying on dependency inversion, we decouple projects from
their dependencies and enable testing in isolation. This approach is important for unit tests, instrumented tested
and integration tests. These three types of tests rely on a chain of trust, where we assume that dependencies
are functioning and tests don’t need to be repeated.

![Testing pyramid](images/testing-pyramid.png){ width="400" }

## Unit tests
