package software.amazon.app.platform.metro.processor

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
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import software.amazon.app.platform.inject.robot.ContributesRobot
import software.amazon.app.platform.ksp.decapitalize
import software.amazon.app.platform.metro.METRO_LOOKUP_PACKAGE
import software.amazon.app.platform.metro.MetroContextAware
import software.amazon.app.platform.metro.addMetroOriginAnnotation
import software.amazon.app.platform.renderer.metro.RobotKey

/**
 * Generates the necessary code in order to support [ContributesRobot].
 *
 * If you use `@ContributesRobot(AbcScope::class)`, then this processor will generate a graph
 * interface, which gets contributed to this scope.
 *
 * ```
 * package app.platform.inject.metro.software.amazon.test
 *
 * @ContributesTo(scope = AbcScope::class)
 * public interface AbcRobotGraph {
 *     @Provide
 *     fun provideAbcRobot(): AbcRobot = AbcRobot()
 *
 *     @Provides
 *     @IntoMap
 *     @RobotKey(AbcRobot::class)
 *     fun provideAbcRobotIntoMap(
 *         robot: () -> AbcRobot,
 *     ): Robot = robot()
 * }
 * ```
 */
@OptIn(KspExperimental::class)
internal class ContributesRobotProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, MetroContextAware {

  private val robotClassName = ClassName("software.amazon.app.platform.robot", "Robot")
  private val robotFqName = robotClassName.canonicalName

  private val robotKey = RobotKey::class.asClassName()

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
      .forEach { generateGraph(it) }

    return emptyList()
  }

  private fun generateGraph(clazz: KSClassDeclaration) {
    val packageName = "${METRO_LOOKUP_PACKAGE}.${clazz.packageName.asString()}"
    val graphClassName = ClassName(packageName, "${clazz.innerClassNames()}Graph")

    val fileSpec =
      FileSpec.builder(graphClassName)
        .addType(
          TypeSpec.interfaceBuilder(graphClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addMetroOriginAnnotation(clazz)
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
                .addAnnotation(
                  AnnotationSpec.builder(robotKey)
                    .addMember("%T::class", clazz.toClassName())
                    .build()
                )
                .addParameter(
                  name = "robot",
                  type = LambdaTypeName.get(returnType = clazz.toClassName()),
                )
                .returns(robotClassName)
                .addStatement("return robot()")
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
    check(clazz.annotations.none { it.isMetroScopeAnnotation() }, clazz) {
      "It's not allowed allowed for a robot to be a singleton, because the lifetime " +
        "of the robot is scoped to the robot() factory function. Remove the @" +
        clazz.annotations.first { it.isMetroScopeAnnotation() }.shortName.asString() +
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
