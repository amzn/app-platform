package software.amazon.app.platform

/** Skips annotated tests on Native platforms. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION) expect annotation class IgnoreNative()
