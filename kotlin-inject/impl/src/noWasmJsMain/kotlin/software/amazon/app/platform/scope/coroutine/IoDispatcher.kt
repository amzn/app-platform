package software.amazon.app.platform.scope.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** Expect declaration for the IO dispatcher, because it doesn't exist for WASM. */
internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
