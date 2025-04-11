package software.amazon.app.platform

import java.lang.Thread

actual val currentThreadName: String
  get() = Thread.currentThread().name
