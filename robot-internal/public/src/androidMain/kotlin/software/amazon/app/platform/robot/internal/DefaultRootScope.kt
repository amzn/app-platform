package software.amazon.app.platform.robot.internal

import androidx.test.platform.app.InstrumentationRegistry
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope

/** A default instance that can be statically obtained. */
public actual val defaultRootScope: Scope?
  get() {
    val applicationContext =
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    return (applicationContext as? RootScopeProvider)?.rootScope
  }
