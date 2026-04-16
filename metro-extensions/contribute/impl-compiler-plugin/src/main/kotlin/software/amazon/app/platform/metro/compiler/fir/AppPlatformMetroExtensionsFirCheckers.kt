package software.amazon.app.platform.metro.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import software.amazon.app.platform.metro.compiler.robot.ContributesRobotChecker

internal class AppPlatformMetroExtensionsFirCheckers(session: FirSession) :
  FirAdditionalCheckersExtension(session) {
  override val declarationCheckers: DeclarationCheckers =
    object : DeclarationCheckers() {
      override val classCheckers: Set<FirClassChecker> = setOf(ContributesRobotChecker)
    }
}
