# FAQ

#### How can I incrementally adopt App Platform?

App Platform offers many recommendations and best practices and hardly enforces any principles, e.g.
it’s possible to adopt the concept of the module structure without the `Scope` class or `Presenters`.
`Presenters` can be used without Compose UI. This and the fact that App Platform is extensible allows
for an incremental adoption. Apps can leverage the concepts and the framework without migrating all code at
once.

For example, instead of going all in on the unidirectional dataflow, Android apps can start adopting `Presenters` and
`Renderers` on an Activity by Activity or Fragment by Fragment basis. Our Android app initially used
[Dagger 2](https://dagger.dev/) and [Anvil](https://github.com/square/anvil) as dependency injection framework and
made it interop with `kotlin-inject-anvil` before switching fully.


#### Can I use [Dagger 2](https://dagger.dev/) or any other DI framework?

It depends, but likely yes. We've chosen [kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil) because
it supports Kotlin Multiplatform and verifies the dependency graph at compile time.

App Platform provides support for [Metro](di.md) out of the box, but there are still rough edges around the KMP
support. Long term we may consider moving App Platform to Metro alone.

Dagger 2 is more challenging, because it only supports Android and JVM application. That said, App Platform started on
Android we used to use Dagger 2. We bridged the Dagger 2 components with the `kotlin-inject-anvil` components for
interop and this served us well for a long time until we fully migrated to `kotlin-inject-anvil`.


#### How does App Platform compare to [Circuit](https://slackhq.github.io/circuit/)?

Circuit shares certain aspects with App Platform in regards to implementing the unidirectional dataflow,
e.g. presenters and decoupling UI. How `Screens` with Circuit work vs how App Platform relies on composing presenters
and renderers is different.

App Platform goes further and has feature that Circuit doesn't provide, e.g. the module structure, the strong
emphasis on fakes and robots.

At Amazon we built App Platform months before Circuit was released in 2022 and at this point there was no reason for
us to migrate off of App Platform and to Circuit.

!!! note "Help needed"

    Help from the community for a more in-depth comparison is needed.


#### Is App Platform used in production by Amazon?

App Platform was developed within the Amazon Delivery organization and is used to share code between several
applications and platforms. Public products include the [in-vehicle delivery app](https://www.youtube.com/watch?v=0T_zvUEqsD4),
[Amazon Flex for Android and iOS](https://flex.amazon.com/) and the Linux based
[Vision-Assisted Package Retrieval](https://www.aboutamazon.com/news/transportation/amazon-vapr-delivery-van-packages).
