package software.amazon.app.platform.metro.compiler.renderer

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.TypeResolutionConfiguration
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.typeResolver
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeConflictingProjection
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirStarProjection
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance
import software.amazon.app.platform.metro.compiler.ClassIds
import software.amazon.app.platform.metro.compiler.fir.findAnnotation
import software.amazon.app.platform.metro.compiler.fir.findClassLikeSymbolInContainingFile
import software.amazon.app.platform.metro.compiler.fir.findClassLikeSymbolInPackageFiles
import software.amazon.app.platform.metro.compiler.fir.hasAnnotation
import software.amazon.app.platform.metro.compiler.fir.resolveClassIdArgument
import software.amazon.app.platform.metro.compiler.fir.resolveClassReferenceArgument
import software.amazon.app.platform.metro.compiler.fir.unwrapArgumentExpression

internal data class ResolvedModelClass(
  val classId: ClassId,
  val classSymbol: FirRegularClassSymbol?,
)

internal data class RendererContributionMetadata(
  val hasInjectAnnotation: Boolean,
  val modelClasses: List<ResolvedModelClass>,
)

internal sealed interface RendererModelTypeResolution {
  data class Success(val modelClass: ResolvedModelClass) : RendererModelTypeResolution

  data class Error(val message: String) : RendererModelTypeResolution
}

internal fun rendererContributionMetadata(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): RendererContributionMetadata? {
  val modelType =
    resolveRendererModelType(classSymbol, session) as? RendererModelTypeResolution.Success
      ?: return null
  val includeSealedSubtypes = contributesRendererIncludeSealedSubtypes(classSymbol, session)

  return RendererContributionMetadata(
    hasInjectAnnotation = hasAnnotation(classSymbol, ClassIds.INJECT, session),
    modelClasses =
      if (includeSealedSubtypes) {
        collectModelClasses(modelType.modelClass, session)
      } else {
        listOf(modelType.modelClass)
      },
  )
}

internal fun resolveRendererModelType(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): RendererModelTypeResolution {
  explicitRendererModelType(classSymbol, session)?.let {
    return RendererModelTypeResolution.Success(it)
  }

  val implicitModelTypes = implicitRendererModelTypes(classSymbol, session)
  return if (implicitModelTypes.size == 1) {
    RendererModelTypeResolution.Success(implicitModelTypes.single())
  } else {
    RendererModelTypeResolution.Error(
      buildString {
        append(
          "Couldn't find BaseModel type for ${classSymbol.name.asString()}. Consider adding " +
            "an explicit parameter."
        )
        if (implicitModelTypes.size > 1) {
          append("Found: ")
          append(implicitModelTypes.joinToString { it.classId.asSingleFqName().asString() })
        }
      }
    )
  }
}

internal fun isSingleInRendererScope(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): Boolean {
  val annotation =
    findAnnotation(classSymbol, ClassIds.SINGLE_IN, session) as? FirAnnotationCall ?: return false
  val rawScopeArgument =
    annotation.argumentMapping.mapping[Name.identifier("scope")]
      ?: annotation.argumentList.arguments.firstOrNull()
      ?: return false
  return resolveClassIdArgument(rawScopeArgument, classSymbol, session) == ClassIds.RENDERER_SCOPE
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal fun constructorParameterCount(classSymbol: FirRegularClassSymbol): Int {
  val constructorSymbol =
    classSymbol.declarationSymbols.filterIsInstance<FirConstructorSymbol>().firstOrNull {
      it.isPrimary
    } ?: classSymbol.declarationSymbols.filterIsInstance<FirConstructorSymbol>().firstOrNull()
  return constructorSymbol?.fir?.valueParameters?.size ?: 0
}

private fun explicitRendererModelType(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): ResolvedModelClass? {
  val annotation =
    findAnnotation(classSymbol, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session)
      as? FirAnnotationCall ?: return null
  val rawModelTypeArgument =
    annotation.argumentMapping.mapping[Name.identifier("modelType")]
      ?: annotation.argumentList.arguments.firstOrNull()
      ?: return null

  return resolveClassReferenceArgument(rawModelTypeArgument, classSymbol, session)
    ?.takeIf { it.classId != ClassIds.UNIT }
    ?.let {
      val resolvedClassSymbol =
        it.classSymbol
          ?: (session.symbolProvider.getClassLikeSymbolByClassId(it.classId)
            as? FirRegularClassSymbol)
          ?: findClassLikeSymbolInContainingFile(classSymbol, it.classId, session)
          ?: findClassLikeSymbolInPackageFiles(
            classSymbol.classId.packageFqName,
            it.classId,
            session,
          )
      ResolvedModelClass(classId = it.classId, classSymbol = resolvedClassSymbol)
    }
}

private fun contributesRendererIncludeSealedSubtypes(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): Boolean {
  val annotation =
    findAnnotation(classSymbol, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session)
      as? FirAnnotationCall ?: return true
  val rawArgument =
    annotation.argumentMapping.mapping[Name.identifier("includeSealedSubtypes")]
      ?: annotation.argumentList.arguments.firstOrNull { argument ->
        (argument as? FirNamedArgumentExpression)?.name == Name.identifier("includeSealedSubtypes")
      }
  val expression = rawArgument?.let(::unwrapArgumentExpression) ?: return true
  return (expression as? FirLiteralExpression)?.value as? Boolean ?: true
}

private fun implicitRendererModelTypes(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<ResolvedModelClass> {
  val collected = linkedMapOf<ClassId, ResolvedModelClass>()
  val visited = mutableSetOf<ConeKotlinType>()
  val queue = ArrayDeque<ConeKotlinType>()

  queue += resolveDeclaredSuperTypes(classSymbol, session)

  while (queue.isNotEmpty()) {
    val type = queue.removeFirst().fullyExpandedType(session)
    if (!visited.add(type)) continue

    collectImplicitModelTypes(type, session).forEach { modelClass ->
      collected.putIfAbsent(modelClass.classId, modelClass)
    }

    val typeSymbol = type.toRegularClassSymbol(session) ?: continue
    queue += resolveDeclaredSuperTypes(typeSymbol, session, actualType = type)
  }

  return collected.values.toList()
}

private fun collectImplicitModelTypes(
  type: ConeKotlinType,
  session: FirSession,
): List<ResolvedModelClass> {
  return type.typeArguments
    .asSequence()
    .mapNotNull { projection ->
      val candidateType =
        (projection as? ConeKotlinTypeProjection)?.type?.fullyExpandedType(session)
          ?: return@mapNotNull null
      val candidateSymbol = candidateType.toRegularClassSymbol(session)
      if (candidateSymbol == null || candidateSymbol.classId == ClassIds.BASE_MODEL) {
        return@mapNotNull null
      }
      if (!isBaseModelSubtype(candidateType, session)) return@mapNotNull null
      ResolvedModelClass(candidateSymbol.classId, candidateSymbol)
    }
    .toList()
}

private fun isBaseModelSubtype(
  type: ConeKotlinType,
  session: FirSession,
  visited: MutableSet<ConeKotlinType> = mutableSetOf(),
): Boolean {
  val expandedType = type.fullyExpandedType(session)
  if (!visited.add(expandedType)) return false

  val classSymbol = expandedType.toRegularClassSymbol(session) ?: return false
  if (classSymbol.classId == ClassIds.BASE_MODEL) return true

  return resolveDeclaredSuperTypes(classSymbol, session, actualType = expandedType).any {
    isBaseModelSubtype(it, session, visited)
  }
}

@OptIn(SymbolInternals::class)
private fun resolveDeclaredSuperTypes(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
  actualType: ConeKotlinType? = null,
): List<ConeKotlinType> {
  val resolutionSession = classSymbol.fir.moduleData.session
  val substitution =
    actualType
      ?.let { actual ->
        classSymbol.typeParameterSymbols.zip(actual.typeArguments).mapNotNull { (parameter, arg) ->
          (arg as? ConeKotlinTypeProjection)?.type?.let { parameter to it }
        }
      }
      .orEmpty()
  val substitutor =
    substitution.takeIf { it.isNotEmpty() }?.let { substitutorByMap(it.toMap(), session) }

  return classSymbol.fir.superTypeRefs.mapNotNull { superTypeRef ->
    val resolvedType =
      resolveSuperTypeRef(superTypeRef, classSymbol, resolutionSession) ?: return@mapNotNull null
    val substitutedType = substitutor?.substituteOrNull(resolvedType) ?: resolvedType
    val expandedType = substitutedType.fullyExpandedType(session)
    expandedType.takeUnless { it.toRegularClassSymbol(session)?.classId == StandardClassIds.Any }
  }
}

private fun resolveSuperTypeRef(
  typeRef: FirTypeRef,
  owner: FirRegularClassSymbol,
  session: FirSession,
): ConeKotlinType? {
  return when (typeRef) {
    is FirResolvedTypeRef -> typeRef.coneType
    is FirUserTypeRef -> resolveUserType(typeRef, owner, session)
    else -> typeRef.coneTypeOrNull
  }?.fullyExpandedType(session)
}

private fun resolveUserType(
  typeRef: FirUserTypeRef,
  owner: FirRegularClassSymbol,
  session: FirSession,
): ConeKotlinType? {
  val manualType = resolveUserTypeManually(typeRef, owner, session)
  val file = findContainingFile(owner, session) ?: return typeRef.coneTypeOrNull ?: manualType
  val scopes = createImportingScopes(file, session, ScopeSession())
  val configuration = TypeResolutionConfiguration(scopes, emptyList(), useSiteFile = file)
  val resolvedType =
    runCatching {
        session.typeResolver
          .resolveType(
            typeRef = typeRef,
            configuration = configuration,
            areBareTypesAllowed = true,
            isOperandOfIsOperator = false,
            resolveDeprecations = false,
            supertypeSupplier = SupertypeSupplier.Default,
            expandTypeAliases = false,
          )
          .type
      }
      .getOrElse {
        return manualType ?: throw it
      }

  if (resolvedType.classId?.shortClassName?.asString() != "<error>") {
    return resolvedType
  }

  return manualType ?: resolvedType
}

private fun resolveUserTypeManually(
  typeRef: FirUserTypeRef,
  owner: FirRegularClassSymbol,
  session: FirSession,
): ConeKotlinType? {
  val fallbackClassId = resolveUserTypeClassId(typeRef, owner, session) ?: return null
  val typeArguments =
    typeRef.qualifier.lastOrNull()?.typeArgumentList?.typeArguments?.map { argument ->
      resolveTypeProjection(argument, owner, session)
    }
  return ConeClassLikeTypeImpl(
    ConeClassLikeLookupTagImpl(fallbackClassId),
    typeArguments.orEmpty().toTypedArray(),
    isMarkedNullable = typeRef.isMarkedNullable,
  )
}

private fun resolveTypeProjection(
  projection: FirTypeProjection,
  owner: FirRegularClassSymbol,
  session: FirSession,
) =
  when (projection) {
    is FirStarProjection -> ConeStarProjection
    is FirTypeProjectionWithVariance -> {
      val resolvedType =
        resolveSuperTypeRef(projection.typeRef, owner, session) ?: projection.typeRef.coneTypeOrNull
      when {
        resolvedType == null -> ConeStarProjection
        projection.variance == Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(resolvedType)
        projection.variance == Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(resolvedType)
        else -> ConeKotlinTypeConflictingProjection(resolvedType)
      }
    }
    else -> ConeStarProjection
  }

private fun findContainingFile(classSymbol: FirClassLikeSymbol<*>, session: FirSession): FirFile? {
  return allSessions(session).firstNotNullOfOrNull { candidate ->
    candidate.firProvider.getFirClassifierContainerFileIfAny(classSymbol)
  }
}

private fun allSessions(session: FirSession): List<FirSession> {
  val visitedModules = linkedSetOf<FirModuleData>()
  val visited = linkedSetOf<FirSession>()

  fun visit(moduleData: FirModuleData) {
    visited.add(moduleData.session)
    if (!visitedModules.add(moduleData)) return

    moduleData.dependencies.forEach(::visit)
    moduleData.friendDependencies.forEach(::visit)
    moduleData.dependsOnDependencies.forEach(::visit)
  }

  visit(session.moduleData)
  return visited.toList()
}

private fun resolveUserTypeClassId(
  typeRef: FirUserTypeRef,
  owner: FirRegularClassSymbol,
  session: FirSession,
): ClassId? {
  val qualifierNames = typeRef.qualifier.map { it.name }
  if (qualifierNames.isEmpty()) return null

  val importedClassId =
    findContainingFile(owner, session)?.let { file ->
      val scopes = createImportingScopes(file, session, ScopeSession())
      scopes.firstNotNullOfOrNull { scope ->
        val importedSymbol =
          scope.getSingleClassifier(qualifierNames.first()) as? FirClassLikeSymbol<*>
            ?: return@firstNotNullOfOrNull null
        qualifierNames.drop(1).fold(importedSymbol.classId) { classId, name ->
          classId.createNestedClassId(name)
        }
      }
    }
  if (importedClassId != null) return importedClassId

  return userTypeClassIdCandidates(owner, qualifierNames).firstOrNull { classId ->
    session.symbolProvider.getClassLikeSymbolByClassId(classId) != null ||
      findClassLikeSymbolInContainingFile(owner, classId, session) != null ||
      findClassLikeSymbolInPackageFiles(owner.classId.packageFqName, classId, session) != null
  }
}

private fun userTypeClassIdCandidates(
  owner: FirRegularClassSymbol,
  qualifierNames: List<Name>,
): Sequence<ClassId> {
  fun nestedClassId(base: ClassId, nestedNames: List<Name>): ClassId {
    return nestedNames.fold(base) { classId, name -> classId.createNestedClassId(name) }
  }

  val topLevelCandidate =
    nestedClassId(
      ClassId.topLevel(owner.classId.packageFqName.child(qualifierNames.first())),
      qualifierNames.drop(1),
    )

  return sequence {
    yield(topLevelCandidate)

    var parentClassId = owner.classId.parentClassId
    while (parentClassId != null) {
      yield(nestedClassId(parentClassId, qualifierNames))
      parentClassId = parentClassId.parentClassId
    }
  }
}

@OptIn(SymbolInternals::class)
private fun collectModelClasses(
  rootModelClass: ResolvedModelClass,
  session: FirSession,
): List<ResolvedModelClass> {
  val collected = linkedMapOf<ClassId, ResolvedModelClass>()
  val queue = ArrayDeque<ResolvedModelClass>()
  queue += rootModelClass

  while (queue.isNotEmpty()) {
    val modelClass = queue.removeFirst()
    if (collected.putIfAbsent(modelClass.classId, modelClass) != null) continue

    val classSymbol =
      modelClass.classSymbol
        ?: (session.symbolProvider.getClassLikeSymbolByClassId(modelClass.classId)
          as? FirRegularClassSymbol)
        ?: continue
    val sealedInheritors = findDirectSealedInheritors(classSymbol, session)
    if (sealedInheritors.isEmpty()) continue

    for ((classId, symbol) in sealedInheritors) {
      queue += ResolvedModelClass(classId = classId, classSymbol = symbol)
    }
  }

  return collected.values.toList()
}

@OptIn(SymbolInternals::class)
private fun findDirectSealedInheritors(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<Pair<ClassId, FirRegularClassSymbol?>> {
  if (!classSymbol.fir.isSealed) return emptyList()

  val collected = linkedMapOf<ClassId, FirRegularClassSymbol?>()

  findDirectSealedInheritorsFromMetadata(classSymbol, session).forEach { (classId, symbol) ->
    collected.putIfAbsent(classId, symbol)
  }
  findDirectSealedInheritorsInSource(classSymbol, session).forEach { symbol ->
    collected[symbol.classId] = collected[symbol.classId] ?: symbol
  }

  return collected.map { (classId, symbol) -> classId to symbol }
}

@OptIn(SymbolInternals::class)
private fun findDirectSealedInheritorsFromMetadata(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<Pair<ClassId, FirRegularClassSymbol?>> {
  val ownerSession = classSymbol.fir.moduleData.session
  return classSymbol.fir.getSealedClassInheritors(ownerSession).distinct().map { classId ->
    classId to
      ((ownerSession.symbolProvider.getClassLikeSymbolByClassId(classId)
        ?: session.symbolProvider.getClassLikeSymbolByClassId(classId))
        as? FirRegularClassSymbol)
  }
}

@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
private fun findDirectSealedInheritorsInSource(
  classSymbol: FirRegularClassSymbol,
  session: FirSession,
): List<FirRegularClassSymbol> {
  val visitedFiles = linkedSetOf<FirFile>()
  val candidates = linkedMapOf<ClassId, FirRegularClassSymbol>()

  fun visitFile(file: FirFile) {
    if (!visitedFiles.add(file)) return

    collectRegularClasses(file.declarations).forEach { candidate ->
      if (candidate.classId == classSymbol.classId) return@forEach
      val hasDirectSupertype =
        resolveDeclaredSuperTypes(candidate, session).any { superType ->
          superType.toRegularClassSymbol(session)?.classId == classSymbol.classId
        }
      if (hasDirectSupertype) {
        candidates.putIfAbsent(candidate.classId, candidate)
      }
    }
  }

  findContainingFile(classSymbol, session)?.let(::visitFile)
  allSessions(session).forEach { candidateSession ->
    candidateSession.firProvider
      .getFirFilesByPackage(classSymbol.classId.packageFqName)
      .forEach(::visitFile)
  }

  return candidates.values.toList()
}

@OptIn(DirectDeclarationsAccess::class)
private fun collectRegularClasses(
  declarations: List<FirDeclaration>
): Sequence<FirRegularClassSymbol> {
  return declarations.asSequence().flatMap { declaration ->
    val regularClass = declaration as? FirRegularClass ?: return@flatMap emptySequence()
    sequenceOf(regularClass.symbol) + collectRegularClasses(regularClass.declarations)
  }
}
