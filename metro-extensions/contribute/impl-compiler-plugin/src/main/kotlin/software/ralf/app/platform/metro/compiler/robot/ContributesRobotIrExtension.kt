package software.ralf.app.platform.metro.compiler.robot

import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.Keys

/** Generates metadata-visible bindings and constructor providers for `@ContributesRobot`. */
@Suppress("DEPRECATION")
internal class ContributesRobotIrExtension(private val compatContext: CompatContext) :
  IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    moduleFragment.transformChildrenVoid(Transformer(pluginContext, compatContext))
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private class Transformer(
    private val pluginContext: IrPluginContext,
    private val compatContext: CompatContext,
  ) : IrElementTransformerVoid(), CompatContext by compatContext {
    private val metadataRegistrar =
      compatContext.createIrGeneratedDeclarationsRegistrar(pluginContext)
    private val builtinsFinder = pluginContext.finderForBuiltinsCompat()

    override fun visitClass(declaration: IrClass): IrStatement {
      val origin = declaration.origin
      if (
        origin is IrDeclarationOrigin.GeneratedByPlugin &&
          origin.pluginKey == Keys.ContributesRobotGeneratorKey &&
          declaration.name == ContributesRobotIds.NESTED_INTERFACE_NAME
      ) {
        val robotClass = declaration.parent as IrClass
        generateBinding(contributionClass = declaration, robotClass = robotClass)
        declaration.declarations
          .filterIsInstance<IrClass>()
          .singleOrNull { it.isCompanion }
          ?.let { generateProvider(companionClass = it, robotClass = robotClass) }
      }
      return super.visitClass(declaration)
    }

    private fun generateBinding(contributionClass: IrClass, robotClass: IrClass) {
      val function = contributionClass.addFunction {
        name = Name.identifier("provide${generatedClassNamePrefix(robotClass)}IntoMap")
        returnType = requireNotNull(builtinsFinder.findClass(ClassIds.ROBOT)).defaultType
        visibility = DescriptorVisibilities.PUBLIC
        modality = Modality.ABSTRACT
        origin = contributionClass.origin
        startOffset = robotClass.startOffset
        endOffset = robotClass.endOffset
      }
      function.apply {
        parameters += requireNotNull(contributionClass.thisReceiver).copyTo(this)
        addValueParameter(name = "robot", type = robotClass.defaultType)
        addAnnotationCompat(buildAnnotation(this, ClassIds.BINDS))
        addAnnotationCompat(buildAnnotation(this, ClassIds.INTO_MAP))
        addAnnotationCompat(buildAnnotation(this, ClassIds.ROBOT_KEY, robotClass.symbol))
      }
      metadataRegistrar.registerFunctionAsMetadataVisible(function)
    }

    private fun generateProvider(companionClass: IrClass, robotClass: IrClass) {
      val constructor =
        robotClass.primaryConstructor ?: robotClass.constructors.firstOrNull() ?: return
      val function = companionClass.addFunction {
        name = Name.identifier("provide${generatedClassNamePrefix(robotClass)}")
        returnType = robotClass.defaultType
        visibility = DescriptorVisibilities.PUBLIC
        modality = Modality.OPEN
        origin = companionClass.origin
        startOffset = robotClass.startOffset
        endOffset = robotClass.endOffset
      }
      function.apply {
        parameters += requireNotNull(companionClass.thisReceiver).copyTo(this)
        for (constructorParameter in constructor.parameters) {
          if (constructorParameter.kind != IrParameterKind.Regular) {
            continue
          }
          parameters += constructorParameter.copyTo(this).apply { defaultValue = null }
        }
        addAnnotationCompat(buildAnnotation(this, ClassIds.PROVIDES))
        body =
          irBuilderFor(this).irBlockBody {
            val constructorCall = irCallConstructor(constructor.symbol, emptyList())
            parameters
              .filter { it.kind == IrParameterKind.Regular }
              .forEachIndexed { index, parameter ->
                constructorCall.arguments[index] = irGet(parameter)
              }
            +irReturn(constructorCall)
          }
      }
      metadataRegistrar.registerFunctionAsMetadataVisible(function)
    }

    private fun generatedClassNamePrefix(robotClass: IrClass): String =
      ContributesRobotIds.generatedClassNamePrefix(requireNotNull(robotClass.classId))

    private fun buildAnnotation(
      function: IrSimpleFunction,
      annotationClassId: ClassId,
      classArgument: IrClassSymbol? = null,
    ): IrConstructorCall {
      val constructor =
        requireNotNull(builtinsFinder.findClass(annotationClassId)).constructors.first()
      return irBuilderFor(function)
        .irAnnotationCompat(callee = constructor, typeArguments = emptyList())
        .apply {
          if (classArgument != null) {
            arguments[0] =
              IrClassReferenceImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = pluginContext.irBuiltIns.kClassClass.typeWith(classArgument.defaultType),
                symbol = classArgument,
                classType = classArgument.defaultType,
              )
          }
        }
    }

    private fun irBuilderFor(function: IrSimpleFunction) =
      DeclarationIrBuilder(
        generatorContext = pluginContext,
        symbol = function.symbol,
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
      )
  }
}
