package software.amazon.app.platform.sample

/**
 * Application class that is used in instrumented tests. Note that it provides a
 * [TestAndroidApplication] instead of [AndroidApplication].
 */
class TestAndroidApplication : AndroidApplication() {
  override fun component(demoApplication: DemoApplication): AppComponent {
    return TestAndroidAppComponent::class.create(this, demoApplication)
  }
}
