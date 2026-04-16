package software.amazon.app.platform.metro.compiler.robot

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import software.amazon.app.platform.metro.compiler.ClassIds
import software.amazon.app.platform.metro.compiler.fir.AppPlatformMetroExtensionsDiagnostics
import software.amazon.app.platform.metro.compiler.fir.extractScopeClassId
import software.amazon.app.platform.metro.compiler.fir.hasAnnotation
import software.amazon.app.platform.metro.compiler.fir.hasTransitiveSupertype

internal object ContributesRobotChecker : FirClassChecker(MppCheckerKind.Common) {

  context(context: CheckerContext, reporter: DiagnosticReporter)
  override fun check(declaration: FirClass) {
    declaration.source ?: return
    val session = context.session

    val annotation =
      declaration.annotations.firstOrNull { candidate ->
        candidate.toAnnotationClassId(session) == ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID
      } ?: return

    val classSymbol = declaration.symbol as? FirRegularClassSymbol ?: return

    if (declaration.classKind != ClassKind.CLASS) {
      reporter.reportOn(
        annotation.source ?: declaration.source,
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_ROBOT_ERROR,
        "@ContributesRobot can only be applied to classes, not " +
          "${declaration.classKind.name.lowercase().replace('_', ' ')}s.",
      )
      return
    }

    if (!implementsRobot(declaration, session)) {
      reporter.reportOn(
        annotation.source ?: declaration.source,
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_ROBOT_ERROR,
        "In order to use @ContributesRobot, ${classSymbol.name.asString()} must implement " +
          "${ClassIds.ROBOT.asSingleFqName()}.",
      )
    }

    if (
      requiresInjectAnnotation(declaration) && !hasAnnotation(classSymbol, ClassIds.INJECT, session)
    ) {
      reporter.reportOn(
        annotation.source ?: declaration.source,
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_ROBOT_ERROR,
        "${classSymbol.name.asString()} must be annotated with @Inject when injecting arguments " +
          "into a robot.",
      )
    }

    val singletonAnnotation =
      declaration.annotations.firstOrNull { candidate ->
        isMetroScopeAnnotation(candidate.toAnnotationClassIdSafe(session), session)
      }
    if (singletonAnnotation != null) {
      reporter.reportOn(
        annotation.source ?: declaration.source,
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_ROBOT_ERROR,
        "It's not allowed for a robot to be a singleton, because the lifetime of the " +
          "robot is scoped to the robot() factory function. Remove the @" +
          singletonAnnotation.toAnnotationClassIdSafe(session)?.shortClassName?.asString() +
          " annotation.",
      )
    }

    val scopeClassId =
      extractScopeClassId(classSymbol, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
    if (scopeClassId != null && scopeClassId != ClassIds.APP_SCOPE) {
      reporter.reportOn(
        annotation.source ?: declaration.source,
        AppPlatformMetroExtensionsDiagnostics.CONTRIBUTES_ROBOT_ERROR,
        "Robots can only be contributed to the AppScope for now. Scope " +
          "${scopeClassId.asSingleFqName()} is unsupported.",
      )
    }
  }

  private fun implementsRobot(declaration: FirClass, session: FirSession): Boolean {
    return declaration.superTypeRefs.any { superTypeRef ->
      val coneType = superTypeRef.coneType.fullyExpandedType(session)
      hasTransitiveSupertype(coneType, session, ClassIds.ROBOT_FQ_NAMES)
    }
  }

  @OptIn(DirectDeclarationsAccess::class)
  private fun requiresInjectAnnotation(declaration: FirClass): Boolean {
    val constructor =
      declaration.declarations.filterIsInstance<FirConstructor>().firstOrNull { it.isPrimary }
        ?: declaration.declarations.filterIsInstance<FirConstructor>().firstOrNull()
    return constructor?.valueParameters?.isNotEmpty() == true
  }

  private fun isMetroScopeAnnotation(annotationClassId: ClassId?, session: FirSession): Boolean {
    val resolvedClassId = annotationClassId ?: return false
    val annotationSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(resolvedClassId) as? FirClassSymbol<*>
        ?: return false
    return annotationSymbol.resolvedCompilerAnnotationsWithClassIds.any {
      it.toAnnotationClassIdSafe(session) == ClassIds.SCOPE
    }
  }
}
