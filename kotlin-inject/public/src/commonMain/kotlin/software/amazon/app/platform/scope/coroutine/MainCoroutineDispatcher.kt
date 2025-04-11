package software.amazon.app.platform.scope.coroutine

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import me.tatarka.inject.annotations.Qualifier

/** Qualifier for the main dispatcher in the app scope. */
@Qualifier
@Retention(RUNTIME)
@Target(CLASS, FUNCTION, PROPERTY_GETTER, VALUE_PARAMETER, TYPE, PROPERTY)
public annotation class MainCoroutineDispatcher
