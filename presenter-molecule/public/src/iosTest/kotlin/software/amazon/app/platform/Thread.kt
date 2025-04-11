package software.amazon.app.platform

actual val currentThreadName: String
  get() = throw NotImplementedError()
