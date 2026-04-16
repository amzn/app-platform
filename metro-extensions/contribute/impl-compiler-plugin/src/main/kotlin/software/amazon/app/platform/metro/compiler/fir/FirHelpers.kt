package software.amazon.app.platform.metro.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildGetClassCall
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun hasAnnotation(
  classSymbol: FirClassSymbol<*>,
  annotationClassId: ClassId,
  session: FirSession,
): Boolean {
  return classSymbol.resolvedCompilerAnnotationsWithClassIds.any {
    it.toAnnotationClassIdSafe(session) == annotationClassId
  }
}

internal fun findAnnotation(
  classSymbol: FirClassSymbol<*>,
  annotationClassId: ClassId,
  session: FirSession,
): FirAnnotation? {
  return classSymbol.resolvedAnnotationsWithArguments.firstOrNull { annotation ->
    annotation.toAnnotationClassIdSafe(session) == annotationClassId
  }
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun buildAnnotationCallWithArgument(
  classId: ClassId,
  argName: Name,
  argument: FirExpression,
  containingSymbol: FirBasedSymbol<*>,
  session: FirSession,
): FirAnnotationCall {
  val annotationType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(classId),
      emptyArray(),
      isMarkedNullable = false,
    )
  val annotationClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId)!!
  val constructorSymbol =
    (annotationClassSymbol as FirClassSymbol<*>)
      .declarationSymbols
      .filterIsInstance<FirConstructorSymbol>()
      .first()
  val argumentParameter = constructorSymbol.fir.valueParameters.first { it.name == argName }

  return org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall {
    annotationTypeRef = annotationType.toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping { mapping[argName] = argument }
    argumentList =
      buildResolvedArgumentList(
        original = null,
        mapping = linkedMapOf(argument to argumentParameter),
      )
    calleeReference = buildResolvedNamedReference {
      name = classId.shortClassName
      resolvedSymbol = constructorSymbol
    }
    containingDeclarationSymbol = containingSymbol
    annotationResolvePhase = FirAnnotationResolvePhase.Types
  }
}

@OptIn(DirectDeclarationsAccess::class)
internal fun buildSimpleAnnotationCall(
  classId: ClassId,
  containingSymbol: FirBasedSymbol<*>,
  session: FirSession,
): FirAnnotationCall {
  val annotationType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(classId),
      emptyArray(),
      isMarkedNullable = false,
    )
  val annotationClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId)!!
  val constructorSymbol =
    (annotationClassSymbol as FirClassSymbol<*>)
      .declarationSymbols
      .filterIsInstance<FirConstructorSymbol>()
      .first()

  return org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall {
    annotationTypeRef = annotationType.toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping()
    calleeReference = buildResolvedNamedReference {
      name = classId.shortClassName
      resolvedSymbol = constructorSymbol
    }
    containingDeclarationSymbol = containingSymbol
    annotationResolvePhase = FirAnnotationResolvePhase.Types
  }
}

internal fun extractScopeArgument(
  classSymbol: FirClassSymbol<*>,
  annotationClassId: ClassId,
  session: FirSession,
): FirExpression? {
  val annotation = findAnnotation(classSymbol, annotationClassId, session) ?: return null
  val annotationCall = annotation as? FirAnnotationCall ?: return null
  val firstArgument = annotationCall.argumentList.arguments.firstOrNull() ?: return null
  return if (firstArgument is FirNamedArgumentExpression) {
    firstArgument.expression
  } else {
    firstArgument
  }
}

internal fun extractScopeClassId(
  classSymbol: FirRegularClassSymbol,
  annotationClassId: ClassId,
  session: FirSession,
): ClassId? {
  val annotation = findAnnotation(classSymbol, annotationClassId, session) ?: return null
  val annotationCall = annotation as? FirAnnotationCall ?: return null

  val rawScopeExpression =
    annotationCall.argumentMapping.mapping[Name.identifier("scope")]
      ?: annotationCall.argumentList.arguments.firstOrNull()
      ?: return null

  val scopeExpression =
    if (rawScopeExpression is FirNamedArgumentExpression) {
      rawScopeExpression.expression
    } else {
      rawScopeExpression
    }

  val getClassCall = scopeExpression as? FirGetClassCall ?: return null
  val innerArgument = getClassCall.argumentList.arguments.firstOrNull() ?: return null

  return when (innerArgument) {
    is FirResolvedQualifier -> innerArgument.classId
    is FirPropertyAccessExpression -> {
      val reference = innerArgument.calleeReference
      if (
        reference is FirResolvedNamedReference && reference.resolvedSymbol is FirClassLikeSymbol<*>
      ) {
        (reference.resolvedSymbol as FirClassLikeSymbol<*>).classId
      } else {
        val name = reference.name
        val file = session.firProvider.getFirClassifierContainerFileIfAny(classSymbol)
        file?.imports?.firstNotNullOfOrNull { import ->
          val importedFqName = import.importedFqName ?: return@firstNotNullOfOrNull null
          if (import.isAllUnder) {
            val classId = ClassId(importedFqName, name)
            session.symbolProvider.getClassLikeSymbolByClassId(classId)?.classId
          } else if (importedFqName.shortName() == name) {
            val classId = ClassId.topLevel(importedFqName)
            session.symbolProvider.getClassLikeSymbolByClassId(classId)?.classId
          } else {
            null
          }
        }
          ?: session.symbolProvider
            .getClassLikeSymbolByClassId(ClassId(FqName("kotlin"), name))
            ?.classId
          ?: session.symbolProvider
            .getClassLikeSymbolByClassId(ClassId(classSymbol.classId.packageFqName, name))
            ?.classId
      }
    }

    else -> null
  }
}

internal fun buildClassExpression(
  classSymbol: FirClassSymbol<*>,
  session: FirSession,
): FirExpression {
  val classId = classSymbol.classId
  val classType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(classId),
      emptyArray(),
      isMarkedNullable = false,
    )
  val kClassClassId = ClassId(FqName("kotlin.reflect"), Name.identifier("KClass"))
  val kClassType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(kClassClassId),
      arrayOf(classType),
      isMarkedNullable = false,
    )

  return buildGetClassCall {
    coneTypeOrNull = kClassType
    argumentList = buildArgumentList {
      arguments += buildResolvedQualifier {
        packageFqName = classId.packageFqName
        relativeClassFqName = classId.relativeClassName
        coneTypeOrNull = classType
        symbol = classSymbol
        resolvedToCompanionObject = false
        isFullyQualified = true
      }
    }
  }
}

internal fun buildClassExpression(classId: ClassId, session: FirSession): FirExpression {
  val classType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(classId),
      emptyArray(),
      isMarkedNullable = false,
    )
  val kClassClassId = ClassId(FqName("kotlin.reflect"), Name.identifier("KClass"))
  val kClassType =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(kClassClassId),
      arrayOf(classType),
      isMarkedNullable = false,
    )

  return buildGetClassCall {
    coneTypeOrNull = kClassType
    argumentList = buildArgumentList {
      arguments += buildResolvedQualifier {
        packageFqName = classId.packageFqName
        relativeClassFqName = classId.relativeClassName
        coneTypeOrNull = classType
        symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId)!!
        resolvedToCompanionObject = false
        isFullyQualified = true
      }
    }
  }
}

internal fun hasTransitiveSupertype(
  type: ConeKotlinType,
  session: FirSession,
  targetIds: Collection<ClassId>,
  visited: MutableSet<ClassId> = mutableSetOf(),
): Boolean {
  val classSymbol = type.toRegularClassSymbol(session) ?: return false
  val classId = classSymbol.classId
  if (!visited.add(classId)) return false
  if (classId in targetIds) return true

  return classSymbol.resolvedSuperTypeRefs.any { superTypeRef ->
    val superConeType = superTypeRef.coneType.fullyExpandedType(session)
    hasTransitiveSupertype(superConeType, session, targetIds, visited)
  }
}
