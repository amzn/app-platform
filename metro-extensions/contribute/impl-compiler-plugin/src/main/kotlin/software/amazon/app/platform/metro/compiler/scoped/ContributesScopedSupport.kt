package software.amazon.app.platform.metro.compiler.scoped

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import software.amazon.app.platform.metro.compiler.ClassIds
import software.amazon.app.platform.metro.compiler.fir.resolveDeclaredSuperTypes

internal data class ResolvedScopedSuperType(val classId: ClassId, val coneType: ConeKotlinType)

internal data class ScopedContributionMetadata(val otherSuperType: ResolvedScopedSuperType?)

internal fun contributesScopedMetadata(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): ScopedContributionMetadata? {
  if (!implementsScoped(classSymbol, session)) return null

  val otherSuperTypes = directOtherSupertypes(classSymbol, session)
  if (otherSuperTypes.size > 1) return null

  return ScopedContributionMetadata(otherSuperType = otherSuperTypes.singleOrNull())
}

internal fun directOtherSupertypes(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<ResolvedScopedSuperType> {
  return resolveDeclaredSuperTypes(classSymbol, session).mapNotNull { superType ->
    val classId = superType.classId ?: return@mapNotNull null
    if (classId == ClassIds.SCOPED || classId == StandardClassIds.Any) return@mapNotNull null
    ResolvedScopedSuperType(classId = classId, coneType = superType)
  }
}

internal fun implementsScoped(classSymbol: FirRegularClassSymbol, session: FirSession): Boolean {
  return isScopedType(classSymbol.defaultType(), session)
}

private fun isScopedType(
  type: ConeKotlinType,
  session: FirSession,
  visited: MutableSet<ConeKotlinType> = mutableSetOf(),
): Boolean {
  val expandedType = type.fullyExpandedType(session)
  if (!visited.add(expandedType)) return false
  if (expandedType.classId == ClassIds.SCOPED) return true

  val classSymbol = expandedType.toRegularClassSymbol(session) ?: return false
  return resolveDeclaredSuperTypes(classSymbol, session, actualType = expandedType).any {
    isScopedType(it, session, visited)
  }
}
