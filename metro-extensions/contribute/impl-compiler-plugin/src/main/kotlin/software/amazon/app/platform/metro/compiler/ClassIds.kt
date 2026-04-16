package software.amazon.app.platform.metro.compiler

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object ClassIds {
  val APP_SCOPE = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("AppScope"))

  val BINDS = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Binds"))

  val CONTRIBUTES_ROBOT =
    ClassId(
      FqName("software.amazon.app.platform.inject.robot"),
      Name.identifier("ContributesRobot"),
    )

  val CONTRIBUTES_TO = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("ContributesTo"))

  val DEPENDENCY_GRAPH = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("DependencyGraph"))

  val INJECT = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Inject"))

  val INTO_MAP = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("IntoMap"))

  val ORIGIN = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Origin"))

  val PROVIDER = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Provider"))

  val PROVIDES = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Provides"))

  val ROBOT = ClassId(FqName("software.amazon.app.platform.robot"), Name.identifier("Robot"))

  val ROBOT_KEY =
    ClassId(FqName("software.amazon.app.platform.renderer.metro"), Name.identifier("RobotKey"))

  val ROBOT_FQ_NAMES: Set<ClassId> = setOf(ROBOT)

  val SCOPE = ClassId(FqName("dev.zacsweers.metro"), Name.identifier("Scope"))
}
