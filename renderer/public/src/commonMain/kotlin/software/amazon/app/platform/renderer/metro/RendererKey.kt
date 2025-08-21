package software.amazon.app.platform.renderer.metro

import dev.zacsweers.metro.MapKey
import software.amazon.app.platform.presenter.BaseModel
import kotlin.reflect.KClass

@Target(
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.CLASS,
  AnnotationTarget.TYPE,
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
public annotation class RendererKey(val value: KClass<out BaseModel>)
