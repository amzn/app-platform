package software.amazon.app.platform.metro.compiler.robot

import com.google.auto.service.AutoService
import dev.zacsweers.metro.compiler.MetroOptions
import dev.zacsweers.metro.compiler.api.fir.MetroFirDeclarationGenerationExtension
import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildNamedFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import software.amazon.app.platform.metro.compiler.ClassIds
import software.amazon.app.platform.metro.compiler.Keys
import software.amazon.app.platform.metro.compiler.fir.buildAnnotationCallWithArgument
import software.amazon.app.platform.metro.compiler.fir.buildClassExpression
import software.amazon.app.platform.metro.compiler.fir.buildSimpleAnnotationCall
import software.amazon.app.platform.metro.compiler.fir.extractScopeArgument
import software.amazon.app.platform.metro.compiler.fir.extractScopeClassId
import software.amazon.app.platform.metro.compiler.fir.hasAnnotation

/**
 * Generates the declaration shape for `@ContributesRobot` classes.
 *
 * Pseudo Kotlin:
 * ```kotlin
 * @ContributesRobot(AppScope::class)
 * class TestRobot : Robot {
 *
 *   @ContributesTo(AppScope::class)
 *   interface RobotContribution {
 *     @Provides
 *     fun provideTestRobot(): TestRobot
 *
 *     @Binds
 *     @IntoMap
 *     @RobotKey(TestRobot::class)
 *     fun provideTestRobotIntoMap(robot: TestRobot): Robot
 *   }
 * }
 * ```
 *
 * No top-level graph interface is generated. If the robot class is already `@Inject`-constructible,
 * the nested declaration omits `provideTestRobot()` and only synthesizes the map-binding method.
 */
public class ContributesRobotFir(session: FirSession) :
  MetroFirDeclarationGenerationExtension(session) {

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(ContributesRobotIds.PREDICATE)
  }

  override fun getContributionHints(): List<ContributionHint> {
    return annotatedRobotClasses().mapNotNull { classSymbol ->
      val scopeClassId =
        extractScopeClassId(classSymbol, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
          ?: return@mapNotNull null
      ContributionHint(
        contributingClassId =
          classSymbol.classId.createNestedClassId(ContributesRobotIds.NESTED_INTERFACE_NAME),
        scope = scopeClassId,
      )
    }
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
  ): Set<Name> {
    return if (
      hasAnnotation(classSymbol, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
    ) {
      setOf(ContributesRobotIds.NESTED_INTERFACE_NAME)
    } else {
      emptySet()
    }
  }

  override fun generateNestedClassLikeDeclaration(
    owner: FirClassSymbol<*>,
    name: Name,
    context: NestedClassGenerationContext,
  ): FirClassLikeSymbol<*>? {
    if (name != ContributesRobotIds.NESTED_INTERFACE_NAME) return null
    if (!hasAnnotation(owner, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)) return null

    val scopeArg =
      extractScopeArgument(owner, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
        ?: return null
    val nestedClassId = owner.classId.createNestedClassId(name)
    val classSymbol = FirRegularClassSymbol(nestedClassId)

    buildRegularClass {
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRobotGeneratorKey.origin
      source = owner.source
      classKind = ClassKind.INTERFACE
      scopeProvider = session.kotlinScopeProvider
      this.name = nestedClassId.shortClassName
      symbol = classSymbol
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.ABSTRACT,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      superTypeRefs += session.builtinTypes.anyType
      annotations +=
        buildAnnotationCallWithArgument(
          ClassIds.CONTRIBUTES_TO,
          Name.identifier("scope"),
          scopeArg,
          classSymbol,
          session,
        )
      annotations += buildOriginAnnotation(owner)
      for (function in buildProvidesFunctions(nestedClassId, owner)) {
        declarations += function
      }
    }

    return classSymbol
  }

  private fun annotatedRobotClasses(): List<FirRegularClassSymbol> {
    return session.predicateBasedProvider
      .getSymbolsByPredicate(ContributesRobotIds.PREDICATE)
      .filterIsInstance<FirRegularClassSymbol>()
      .toList()
  }

  private fun buildProvidesFunctions(
    graphClassId: ClassId,
    owner: FirClassSymbol<*>,
  ): List<FirFunction> {
    val functions = mutableListOf<FirFunction>()
    if (!hasAnnotation(owner, ClassIds.INJECT, session)) {
      functions += buildProvideRobotFunction(graphClassId, owner)
    }
    functions += buildProvideRobotIntoMapFunction(graphClassId, owner)
    return functions
  }

  private fun buildProvideRobotFunction(
    graphClassId: ClassId,
    owner: FirClassSymbol<*>,
  ): FirFunction {
    val functionName = "provide${ContributesRobotIds.generatedClassNamePrefix(owner.classId)}"
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRobotGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = owner.defaultType().toFirResolvedTypeRef()
      dispatchReceiverType = generatedGraphType(graphClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      annotations += buildSimpleAnnotationCall(ClassIds.PROVIDES, functionSymbol, session)
    }
  }

  private fun buildProvideRobotIntoMapFunction(
    graphClassId: ClassId,
    owner: FirClassSymbol<*>,
  ): FirFunction {
    val functionName =
      "provide${ContributesRobotIds.generatedClassNamePrefix(owner.classId)}IntoMap"
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)
    val ownerType = owner.defaultType()

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRobotGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = robotType().toFirResolvedTypeRef()
      dispatchReceiverType = generatedGraphType(graphClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      valueParameters += buildValueParameter {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        moduleData = session.moduleData
        origin = Keys.ContributesRobotGeneratorKey.origin
        source = owner.source
        returnTypeRef = ownerType.toFirResolvedTypeRef()
        name = Name.identifier("robot")
        symbol = FirValueParameterSymbol()
        containingDeclarationSymbol = functionSymbol
      }
      annotations += buildSimpleAnnotationCall(ClassIds.BINDS, functionSymbol, session)
      annotations += buildSimpleAnnotationCall(ClassIds.INTO_MAP, functionSymbol, session)
      annotations += buildRobotKeyAnnotation(owner.classId)
    }
  }

  private fun generatedGraphType(graphClassId: ClassId) =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(graphClassId),
      emptyArray(),
      isMarkedNullable = false,
    )

  private fun robotType() =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(ClassIds.ROBOT),
      emptyArray(),
      isMarkedNullable = false,
    )

  private fun buildOriginAnnotation(owner: FirClassSymbol<*>) = buildAnnotation {
    val originSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ClassIds.ORIGIN) as? FirRegularClassSymbol
        ?: error("Annotation class ${ClassIds.ORIGIN} not found on the classpath")
    annotationTypeRef = originSymbol.defaultType().toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping {
      mapping[Name.identifier("value")] = buildClassExpression(owner, session)
    }
  }

  private fun buildRobotKeyAnnotation(robotClassId: ClassId) = buildAnnotation {
    val robotKeySymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ClassIds.ROBOT_KEY)
        as? FirRegularClassSymbol
        ?: error("Annotation class ${ClassIds.ROBOT_KEY} not found on the classpath")
    annotationTypeRef = robotKeySymbol.defaultType().toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping {
      mapping[Name.identifier("value")] = buildClassExpression(robotClassId, session)
    }
  }

  @AutoService(MetroFirDeclarationGenerationExtension.Factory::class)
  public class Factory : MetroFirDeclarationGenerationExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroFirDeclarationGenerationExtension = ContributesRobotFir(session)
  }
}
