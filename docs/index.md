# App Platform

The App Platform is a lightweight application framework for state and memory management suitable
for Kotlin Multiplatform projects, in particular Android, iOS, JVM, native and Web (1). It makes the
dependency inversion (2) and dependency injection (DI) design patterns first class principles to develop
features and support the variety of platforms. The UI layer is entirely decoupled from the business logic,
which allows different application targets to change the look and feel.
{ .annotate }

1.  Web support is still in development.
2.  Dependency inversion means that high-level APIs don’t depend on low-level details and low-level details only import other high-level APIs.

App Platform pushes for code reuse by sharing APIs and implementations, while making it easy to leverage
platform strengths and changing app or device specific behavior when needed.

=== "Android"
    ![Android screenshot](docs/images/Android.png)

=== "iOS"
    ![iOS screenshot](docs/images/iOS.png)

=== "Desktop"
    ![Desktop screenshot](docs/images/Desktop.png)
