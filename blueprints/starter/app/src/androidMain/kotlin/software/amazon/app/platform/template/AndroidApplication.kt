package software.amazon.app.platform.template

import android.app.Application
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope

/**
 * The [Application] class of our sample app. Note that this class implements [RootScopeProvider].
 * This is helpful to get access to the root scope from Android components such as activities.
 */
open class AndroidApplication : Application(), RootScopeProvider {
    private val application = software.amazon.app.platform.template.Application()

    override val rootScope: Scope
        get() = application.rootScope

    override fun onCreate() {
        application.create(component(application))
        super.onCreate()
    }

    /** Create the [AppComponent]. In UI tests we use a different instance. */
    protected open fun component(application: software.amazon.app.platform.template.Application): AppComponent {
        return AndroidAppComponent::class.create(this, application)
    }
}
