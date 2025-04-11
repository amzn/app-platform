package software.amazon.app.platform

/** Provides the name of the current thread this is called on. */
actual val currentThreadName: String = throw NotImplementedError()
