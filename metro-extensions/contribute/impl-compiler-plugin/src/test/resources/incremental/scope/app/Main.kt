package com.test

import dev.zacsweers.metro.createGraph

fun main() {
  val first = createGraph<FirstGraph>().scoped
  val second = createGraph<SecondGraph>().scoped
  check((first + second).all { it is Contribution })
  println("RESULT:${first.size},${second.size}")
}
