package software.amazon.app.platform.inject

import com.google.devtools.ksp.isDefault
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.squareup.kotlinpoet.Annotatable
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import java.util.Locale
import software.amazon.lastmile.kotlin.inject.anvil.internal.Origin

/**
 * The package in which code is generated that should be picked up during the merging phase. This
 * package is used by the open source project.
 */
internal const val OPEN_SOURCE_LOOKUP_PACKAGE = "amazon.lastmile.inject"

/** The package in which the App Platform extensions generate code. */
internal const val APP_PLATFORM_LOOKUP_PACKAGE = "app.platform.inject"

internal fun String.decapitalize(): String = replaceFirstChar { it.lowercase(Locale.US) }

internal fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}

internal fun <T : Annotatable.Builder<T>> Annotatable.Builder<T>.addOriginAnnotation(
  clazz: KSClassDeclaration
): T =
  addAnnotation(
    AnnotationSpec.builder(Origin::class)
      .addMember("value = %T::class", clazz.toClassName())
      .build()
  )

internal inline fun <reified T> KSAnnotation.argumentOfTypeAt(
  context: ContextAware,
  name: String,
): T? {
  return argumentOfTypeWithMapperAt<T, T>(context, name) { _, value -> value }
}

private inline fun <reified T, R> KSAnnotation.argumentOfTypeWithMapperAt(
  context: ContextAware,
  name: String,
  mapper: (arg: KSValueArgument, value: T) -> R,
): R? {
  return argumentAt(name)?.let { arg ->
    val value = arg.value
    context.check(value is T, arg) {
      "Expected argument '$name' of type '${T::class.qualifiedName} but was '${arg.javaClass.name}'."
    }
    (value as T)?.let { mapper(arg, it) }
  }
}

internal fun KSAnnotation.argumentAt(name: String): KSValueArgument? {
  return arguments.find { it.name?.asString() == name }?.takeUnless { it.isDefault() }
}
