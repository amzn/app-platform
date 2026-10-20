package software.ralf.app.platform.metro.compiler.scoped

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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.Keys

@Suppress("DEPRECATION")
internal class ContributesScopedIrExtension(private val compatContext: CompatContext) :
  IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    moduleFragment.transformChildrenVoid(
      ContributesScopedIrTransformer(pluginContext, compatContext),
    )
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private class ContributesScopedIrTransformer(
    private val pluginContext: IrPluginContext,
    private val compatContext: CompatContext,
  ) : IrElementTransformerVoid() {
    private val metadataRegistrar =
      compatContext.createIrGeneratedDeclarationsRegistrar(pluginContext)
    private val origin = IrDeclarationOrigin.GeneratedByPlugin(Keys.ContributesScopedGeneratorKey)

    override fun visitClass(declaration: IrClass): IrStatement {
      if (
        declaration.origin == origin &&
          declaration.name == ContributesScopedIds.NESTED_INTERFACE_NAME
      ) {
        generateFunctions(declaration)
      }
      return super.visitClass(declaration)
    }

    private fun generateFunctions(contribution: IrClass) {
      val owner = contribution.parentAsClass
      val ownerName = ContributesScopedIds.generatedOwnerName(requireNotNull(owner.classId))
      owner.superTypes
        .filter { type ->
          val classId = type.classOrNull?.owner?.classId
          classId != ClassIds.SCOPED && classId != StandardClassIds.Any
        }
        .singleOrNull()
        ?.let { superType ->
          val boundClassId = requireNotNull(superType.classOrNull?.owner?.classId)
          generateBinding(
            contribution = contribution,
            owner = owner,
            name = "bind${ContributesScopedIds.generatedTypeName(boundClassId)}",
            returnType = superType,
          )
        }
      val scopeArgument =
        with(compatContext) {
          owner
            .annotationsCompat()
            .single { it.symbol.owner.parentAsClass.classId == ClassIds.CONTRIBUTES_SCOPED }
            .getAnnotationArgumentCompat(Name.identifier("scope"))
        }
      generateBinding(
        contribution = contribution,
        owner = owner,
        name = "bind${ownerName}Scoped",
        returnType = requireNotNull(pluginContext.referenceClass(ClassIds.SCOPED)).defaultType,
        scopeArgument = requireNotNull(scopeArgument),
      )
      contribution.declarations
        .filterIsInstance<IrClass>()
        .singleOrNull { it.isCompanion }
        ?.let { companion ->
          generateProvider(companion = companion, owner = owner, name = "provide$ownerName")
        }
    }

    private fun generateBinding(
      contribution: IrClass,
      owner: IrClass,
      name: String,
      returnType: IrType,
      scopeArgument: IrExpression? = null,
    ) {
      val function = contribution.addFunction {
        this.name = Name.identifier(name)
        this.returnType = returnType
        this.origin = this@ContributesScopedIrTransformer.origin
        visibility = DescriptorVisibilities.PUBLIC
        modality = Modality.ABSTRACT
        startOffset = owner.startOffset
        endOffset = owner.endOffset
      }
      function.parameters = listOf(requireNotNull(contribution.thisReceiver).copyTo(function))
      function.addValueParameter(name = "instance", type = owner.symbol.defaultType)
      with(compatContext) {
        function.addAnnotationCompat(annotation(function, ClassIds.BINDS))
        if (scopeArgument != null) {
          function.addAnnotationCompat(annotation(function, ClassIds.INTO_SET))
          function.addAnnotationCompat(
            annotation(function, ClassIds.FOR_SCOPE, scopeArgument.deepCopyWithSymbols()),
          )
        }
      }
      metadataRegistrar.registerFunctionAsMetadataVisible(function)
    }

    private fun generateProvider(companion: IrClass, owner: IrClass, name: String) {
      val constructor = owner.primaryConstructor ?: owner.constructors.first()
      val function = companion.addFunction {
        this.name = Name.identifier(name)
        returnType = owner.symbol.defaultType
        origin = this@ContributesScopedIrTransformer.origin
        visibility = DescriptorVisibilities.PUBLIC
        modality = Modality.OPEN
        startOffset = owner.startOffset
        endOffset = owner.endOffset
      }
      function.parameters = listOf(requireNotNull(companion.thisReceiver).copyTo(function))
      val parameters =
        constructor.parameters
          .filter { it.kind == IrParameterKind.Regular }
          .map { parameter ->
            function
              .addValueParameter {
                this.name = parameter.name
                type = parameter.type
                varargElementType = parameter.varargElementType
                isCrossInline = parameter.isCrossinline
                isNoinline = parameter.isNoinline
                origin = this@ContributesScopedIrTransformer.origin
              }
              .also { generated ->
                with(compatContext) {
                  generated.addAnnotationsCompat(
                    parameter.annotationsCompat().map { it.deepCopyWithSymbols() },
                  )
                }
              }
          }
      with(compatContext) {
        function.addAnnotationCompat(annotation(function, ClassIds.PROVIDES))
        owner
          .annotationsCompat()
          .firstOrNull { it.symbol.owner.parentAsClass.classId == ClassIds.SINGLE_IN }
          ?.let {
            function.addAnnotationCompat(it.deepCopyWithSymbols())
          }
      }
      function.body =
        irBuilderFor(function).irBlockBody {
          val call = irCallConstructor(constructor.symbol, emptyList())
          parameters.forEachIndexed { index, parameter -> call.arguments[index] = irGet(parameter) }
          +irReturn(call)
        }
      metadataRegistrar.registerFunctionAsMetadataVisible(function)
    }

    private fun annotation(
      function: IrSimpleFunction,
      classId: ClassId,
      argument: IrExpression? = null,
    ): IrConstructorCall {
      val constructor = requireNotNull(pluginContext.referenceClass(classId)).constructors.single()
      return with(compatContext) {
          irBuilderFor(function)
            .irAnnotationCompat(callee = constructor, typeArguments = emptyList())
        }
        .apply {
          if (argument != null) {
            arguments[0] = argument
          }
        }
    }

    private fun irBuilderFor(function: IrSimpleFunction) =
      DeclarationIrBuilder(pluginContext, function.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
  }
}
