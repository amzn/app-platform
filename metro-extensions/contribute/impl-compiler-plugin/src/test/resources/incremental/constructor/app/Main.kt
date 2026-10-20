package com.test

import dev.zacsweers.metro.createGraph

fun main() {
  val scoped = createGraph<Graph>().scoped.single() as TestScoped
  println("RESULT:${scoped.value}")
}
