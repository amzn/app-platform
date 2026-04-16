// RUN_PIPELINE_TILL: BACKEND
// DUMP_KT_IR
package com.test

import software.amazon.app.platform.inject.robot.ContributesRobot
import software.amazon.app.platform.robot.Robot

@ContributesRobot(AppScope::class)
class TestRobot : Robot
