package software.amazon.app.platform.inject.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.inject.APP_PLATFORM_LOOKUP_PACKAGE
import software.amazon.app.platform.inject.ContextAware
import software.amazon.app.platform.inject.addOriginAnnotation
import software.amazon.app.platform.inject.decapitalize
import software.amazon.app.platform.inject.robot.ContributesRobot
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/**
 * Generates the necessary code in order to support [ContributesRobot].
 *
 * If you use `@ContributesRobot(AbcScope::class)`, then this code generator will generate a
 * component interface, which gets contributed to this scope.
 *
 * ```
 * package app.platform.inject.software.amazon.test
 *
 * @ContributesTo(scope = AbcScope::class)
 * public interface AbcRobotComponent {
 *     @Provide
 *     fun provideAbcRobot(): AbcRobot = AbcRobot()
 *
 *     @Provides
 *     @IntoMap
 *     fun provideAbcRobotIntoMap(
 *         robot: () -> AbcRobot,
 *     ): Pair<KClass<out Robot>, () -> Robot> = AbcRobot::class to robot
 * }
 * ```
 */
@OptIn(KspExperimental::class)
internal class ContributesRobotProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, ContextAware {

  private val robotClassName = ClassName("software.amazon.app.platform.robot", "Robot")
  private val robotFqName = robotClassName.canonicalName

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(ContributesRobot::class)
      .filterIsInstance<KSClassDeclaration>()
      .onEach {
        checkIsPublic(it)
        checkHasInjectAnnotation(it)
        checkNotSingleton(it)
        checkSuperType(it)
        checkAppScope(it)
      }
      .forEach { generateComponentInterface(it) }

    return emptyList()
  }

  private fun generateComponentInterface(clazz: KSClassDeclaration) {
    val packageName = "${APP_PLATFORM_LOOKUP_PACKAGE}.${clazz.packageName.asString()}"
    val componentClassName = ClassName(packageName, "${clazz.innerClassNames()}Component")

    val fileSpec =
      FileSpec.builder(componentClassName)
        .addType(
          TypeSpec.interfaceBuilder(componentClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addOriginAnnotation(clazz)
            .addAnnotation(
              AnnotationSpec.builder(ContributesTo::class)
                .addMember("%T::class", clazz.scope().type.toClassName())
                .build()
            )
            .apply {
              if (!clazz.isAnnotationPresent(Inject::class)) {
                addFunction(
                  FunSpec.builder("provide${clazz.innerClassNames()}")
                    .addAnnotation(Provides::class)
                    .returns(clazz.toClassName())
                    .addStatement("return %T()", clazz.toClassName())
                    .build()
                )
              }
            }
            .addFunction(
              FunSpec.builder("provide${clazz.innerClassNames()}IntoMap")
                .addAnnotation(Provides::class)
                .addAnnotation(IntoMap::class)
                .addParameter(
                  name = "robot",
                  type = LambdaTypeName.get(returnType = clazz.toClassName()),
                )
                .returns(
                  Pair::class.asClassName()
                    .parameterizedBy(
                      listOf(
                        KClass::class.asClassName()
                          .parameterizedBy(WildcardTypeName.producerOf(robotClassName)),
                        LambdaTypeName.get(returnType = robotClassName),
                      )
                    )
                )
                .addStatement("return %T::class·to·robot", clazz.toClassName())
                .build()
            )
            .addProperty(name = clazz.innerClassNames().decapitalize(), type = clazz.toClassName())
            .build()
        )
        .build()

    fileSpec.writeTo(codeGenerator, aggregating = false)
  }

  private fun checkHasInjectAnnotation(clazz: KSClassDeclaration) {
    if (clazz.primaryConstructor?.parameters?.isNotEmpty() == true) {
      check(clazz.annotations.any { it.isAnnotation(injectFqName) }, clazz) {
        "${clazz.simpleName.asString()} must be annotated with @Inject when " +
          "injecting arguments into a robot."
      }
    }
  }

  private fun checkNotSingleton(clazz: KSClassDeclaration) {
    check(clazz.annotations.none { it.isKotlinInjectScopeAnnotation() }, clazz) {
      "It's not allowed allowed for a robot to be a singleton, because the lifetime " +
        "of the robot is scoped to the robot() factory function. Remove the @" +
        clazz.annotations.first { it.isKotlinInjectScopeAnnotation() }.shortName.asString() +
        " annotation."
    }
  }

  private fun checkSuperType(clazz: KSClassDeclaration) {
    val extendsRobot =
      clazz.getAllSuperTypes().any { it.declaration.requireQualifiedName() == robotFqName }

    check(extendsRobot, clazz) {
      "In order to use @ContributesRobot, ${clazz.simpleName.asString()} must " +
        "implement $robotFqName."
    }
  }

  private fun checkAppScope(clazz: KSClassDeclaration) {
    val scope = clazz.scope().type.declaration.requireQualifiedName()
    check(scope == AppScope::class.requireQualifiedName(), clazz) {
      "Robots can only be contributed to the AppScope for now. Scope $scope is unsupported."
    }
  }
}
