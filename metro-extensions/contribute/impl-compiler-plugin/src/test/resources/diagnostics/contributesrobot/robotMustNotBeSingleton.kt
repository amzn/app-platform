// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.amazon.app.platform.inject.robot.ContributesRobot
import software.amazon.app.platform.robot.Robot

@SingleIn(AppScope::class)
<!CONTRIBUTES_ROBOT_ERROR!>@ContributesRobot(AppScope::class)<!>
class TestRobot : Robot
