package software.amazon.app.platform.robot.internal

import software.amazon.app.platform.scope.RootScopeProvider

/**
 * A global value that can be set before a test runs. The returned root scope will be used to
 * initialize robots.
 */
public var rootScopeProvider: RootScopeProvider? = null
