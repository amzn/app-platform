package com.test

import dev.zacsweers.metro.createGraph
import kotlin.reflect.KClass
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer
import software.ralf.app.platform.renderer.RendererFactory
import software.ralf.app.platform.renderer.RendererGraph

private object UnusedRendererFactory : RendererFactory {
  override fun <T : BaseModel> createRenderer(modelType: KClass<out T>): Renderer<T> =
    error("Unused renderer factory")

  override fun <T : BaseModel> getRenderer(
    modelType: KClass<out T>,
    rendererId: Int,
  ): Renderer<T> = error("Unused renderer factory")
}

fun main() {
  val factory = createGraph<AppGraph>() as RendererGraph.Factory
  val graph = factory.createRendererGraph(UnusedRendererFactory)
  check(Model::class in graph.renderers.keys)
  check(graph.renderers.keys == graph.modelToRendererMapping.keys)
  check(graph.modelToRendererMapping.values.all { it == TestRenderer::class })
  check(graph.renderers.values.all { it() is TestRenderer })
  val modelNames = graph.renderers.keys.map { requireNotNull(it.simpleName) }.sorted()
  println("RESULT:${modelNames.joinToString(",")}")
}
