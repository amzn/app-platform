package software.amazon.app.platform.internal

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/** Skips annotated tests on Native platforms. */
@Target(CLASS, FUNCTION) actual annotation class IgnoreNative actual constructor()
