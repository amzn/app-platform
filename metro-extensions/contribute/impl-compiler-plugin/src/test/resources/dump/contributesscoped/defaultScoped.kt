// RUN_PIPELINE_TILL: BACKEND
package com.test

import software.amazon.app.platform.inject.metro.ContributesScoped
import software.amazon.app.platform.scope.Scoped

interface SuperType

@Inject
@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class TestClass : SuperType, Scoped
