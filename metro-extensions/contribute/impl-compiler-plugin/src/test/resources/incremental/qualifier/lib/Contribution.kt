package com.test

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Named
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot

@ContributesRobot(AppScope::class) class TestRobot(@Named("before") val value: String) : Robot
