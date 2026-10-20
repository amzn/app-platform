// MODULE: lib
// FILE: SuperType.kt
package com.test

interface SuperType

// FILE: TestClass.kt
package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

@ContributesScoped(AppScope::class)
class TestClass : SuperType, Scoped

// MODULE: main(lib)
// FILE: main.kt
package com.test

import software.ralf.app.platform.scope.Scoped

fun bindSuperType(
  contribution: TestClass.ScopedContribution,
  instance: TestClass,
): SuperType = contribution.bindSuperType(instance)

fun bindScoped(
  contribution: TestClass.ScopedContribution,
  instance: TestClass,
): Scoped = contribution.bindTestClassScoped(instance)

fun box(): String {
  assertIs<TestClass>(TestClass.ScopedContribution.provideTestClass())
  return "OK"
}
