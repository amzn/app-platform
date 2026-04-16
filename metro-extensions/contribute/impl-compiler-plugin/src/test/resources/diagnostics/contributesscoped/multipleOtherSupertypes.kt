// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.amazon.app.platform.inject.metro.ContributesScoped
import software.amazon.app.platform.scope.Scoped

interface SuperType

interface SuperType2

@Inject
@SingleIn(AppScope::class)
<!CONTRIBUTES_SCOPED_ERROR!>@ContributesScoped(AppScope::class)<!>
class TestClass : SuperType, SuperType2, Scoped
