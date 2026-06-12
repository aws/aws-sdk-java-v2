/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.poet.model;

import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.internal.Utils.capitalize;
import static software.amazon.awssdk.codegen.poet.model.DeprecationUtils.checkDeprecated;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.docs.DocumentationBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils;
import software.amazon.awssdk.codegen.poet.model.TypeProvider.TypeNameOptions;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Provides the Poet specs for AWS Service models.
 */
public class AwsServiceModel implements ClassSpec {

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final PoetExtension poetExtensions;
    private final TypeProvider typeProvider;
    private final ShapeModelSpec shapeModelSpec;
    private final ModelBuilderSpecs modelBuilderSpecs;
    private final ServiceModelCopiers serviceModelCopiers;
    private final ModelMethodOverrides modelMethodOverrides;

    public AwsServiceModel(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.poetExtensions = new PoetExtension(intermediateModel);
        this.typeProvider = new TypeProvider(intermediateModel);
        this.shapeModelSpec = new ShapeModelSpec(this.shapeModel,
                                                 typeProvider,
                                                 poetExtensions,
                                                 intermediateModel);
        this.modelBuilderSpecs = resolveBuilderSpecs();
        this.serviceModelCopiers = new ServiceModelCopiers(this.intermediateModel);
        this.modelMethodOverrides = new ModelMethodOverrides(className(), this.poetExtensions);
    }

    @Override
    public TypeSpec poetSpec() {
        if (shapeModel.isEventStream()) {
            return eventStreamInterfaceSpec();
        }
        List<FieldSpec> fields = shapeModelSpec.fields();

        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(className())
                                               .addModifiers(PUBLIC)
                                               .addAnnotation(PoetUtils.generatedAnnotation())
                                               .addSuperinterfaces(modelSuperInterfaces())
                                               .superclass(modelSuperClass())
                                               .addMethods(modelClassMethods())
                                               .addFields(fields)
                                               .addFields(shapeModelSpec.staticFields())
                                               .addMethod(addModifier(sdkFieldsMethod(), FINAL))
                                               .addMethod(addModifier(sdkFieldNameToFieldMethod(), FINAL))
                                               .addTypes(nestedModelClassTypes());

        shapeModelSpec.additionalMethods().forEach(specBuilder::addMethod);

        if (shapeModel.isUnion()) {
            specBuilder.addField(unionTypeField());
        }

        if (!isEvent()) {
            specBuilder.addModifiers(Modifier.FINAL);
        }

        // Add serializable version UID for model and exceptions.
        if (shapeModel.getShapeType() == ShapeType.Model || shapeModel.getShapeType() == ShapeType.Exception) {
            specBuilder.addField(FieldSpec.builder(long.class, "serialVersionUID",
                                                   Modifier.PRIVATE, STATIC, Modifier.FINAL)
                                          .initializer("1L")
                                          .build());
        }

        if (!fields.isEmpty()) {
            specBuilder
                .addMethod(getterCreator())
                .addMethod(setterCreator());
        }

        if (this.shapeModel.isEvent()) {
            addEventSupport(specBuilder);
        }

        if (this.shapeModel.getDocumentation() != null) {
            specBuilder.addJavadoc("$L", this.shapeModel.getDocumentation());
        }

        return specBuilder.build();
    }

    private ModelBuilderSpecs resolveBuilderSpecs() {
        return new ModelBuilderSpecs(intermediateModel, shapeModel, typeProvider);
    }

    private TypeSpec eventStreamInterfaceSpec() {
        Collection<OperationModel> opModels = EventStreamUtils.findOperationsWithEventStream(intermediateModel,
                shapeModel);

        Collection<OperationModel> outputOperations = findOutputEventStreamOperations(opModels, shapeModel);

        EventStreamSpecHelper helper = new EventStreamSpecHelper(shapeModel, intermediateModel);

        ClassName modelClass = poetExtensions.getModelClassFromShape(shapeModel);

        TypeSpec.Builder builder =
                PoetUtils.createInterfaceBuilder(modelClass)
                         .addAnnotation(SdkPublicApi.class)
                         .addMethods(eventStreamInterfaceEventBuilderMethods())
                         .addType(helper.eventTypeEnumSpec());


        ClassName eventTypeEnum = helper.eventTypeEnumClassName();
        builder.addMethod(MethodSpec.methodBuilder("sdkEventType")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(eventTypeEnum)
                .addJavadoc("The type of this event. Corresponds to the {@code :event-type} header on the Message.")
                .addStatement("return $T.UNKNOWN_TO_SDK_VERSION", eventTypeEnum)
                .build());

        if (!outputOperations.isEmpty()) {
            CodeBlock unknownInitializer = buildUnknownEventStreamInitializer(outputOperations,
                    modelClass);

            builder.addSuperinterface(ClassName.get(SdkPojo.class))
                   .addJavadoc("Base interface for all event types in $L.", shapeModel.getShapeName())
                    .addField(FieldSpec.builder(modelClass, "UNKNOWN")
                                    .addModifiers(PUBLIC, STATIC, Modifier.FINAL)
                                    .initializer(unknownInitializer)
                                    .addJavadoc("Special type of {@link $T} for unknown types of events that this "
                                            + "version of the SDK does not know about", modelClass)
                                    .build());

            for (OperationModel opModel : outputOperations) {
                ClassName responseHandlerClass = poetExtensions.eventStreamResponseHandlerType(opModel);
                builder.addMethod(acceptMethodSpec(modelClass, responseHandlerClass)
                        .addModifiers(Modifier.ABSTRACT)
                        .build());
            }

            return builder.build();

        } else if (hasInputStreamOperations(opModels, shapeModel)) {
            return builder.addJavadoc("Base interface for all event types in $L.", shapeModel.getShapeName())
                          .build();
        }

        throw new IllegalArgumentException(shapeModel.getShapeName() + " event stream shape is not found "
                + "in any request or response shape");
    }

    private void addEventSupport(TypeSpec.Builder specBuilder) {
        EventStreamUtils.getBaseEventStreamShapes(intermediateModel, shapeModel)
                .forEach(eventStream -> addEventSupport(specBuilder, eventStream));
    }

    private void addEventSupport(TypeSpec.Builder specBuilder, ShapeModel eventStream) {
        ClassName eventStreamClassName = poetExtensions.getModelClassFromShape(eventStream);
        Collection<OperationModel> opModels = EventStreamUtils.findOperationsWithEventStream(intermediateModel,
                                                                                             eventStream);

        Collection<OperationModel> outputOperations = findOutputEventStreamOperations(opModels, eventStream);

        boolean onOutput = !outputOperations.isEmpty();
        boolean onInput = hasInputStreamOperations(opModels, eventStream);

        if (!onOutput && !onInput) {
            throw new IllegalArgumentException(shapeModel.getC2jName() + " event shape is not a member in any "
                    + "request or response event shape");
        }

        EventStreamSpecHelper helper = new EventStreamSpecHelper(eventStream, intermediateModel);

        specBuilder.addSuperinterface(eventStreamClassName);

        boolean usesLegacyScheme = useLegacyEventGenerationScheme(eventStream);
        Optional<MemberModel> legacyEvent = findLegacyGenerationEventWithShape(eventStream);

        if (usesLegacyScheme && legacyEvent.isPresent()) {
            NamingStrategy namingStrategy = intermediateModel.getNamingStrategy();
            ClassName eventTypeEnum = helper.eventTypeEnumClassName();
            specBuilder.addMethod(MethodSpec.methodBuilder("sdkEventType")
                       .addAnnotation(Override.class)
                       .addModifiers(PUBLIC)
                       .returns(eventTypeEnum)
                       .addStatement("return $T.$N",
                                     eventTypeEnum,
                                     namingStrategy.getEnumValueName(legacyEvent.get().getName()))
                       .build());
        }

        if (onOutput) {
            ClassName modelClass = poetExtensions.getModelClass(shapeModel.getShapeName());
            for (OperationModel opModel : outputOperations) {
                ClassName responseHandlerClass = poetExtensions.eventStreamResponseHandlerType(opModel);

                MethodSpec.Builder acceptMethodSpec = acceptMethodSpec(modelClass, responseHandlerClass)
                        .addAnnotation(Override.class);

                if (usesLegacyScheme) {
                    acceptMethodSpec.addStatement("visitor.visit(this)");
                } else {
                    // The class that represents the event type will be
                    // responsible for implementing this
                    acceptMethodSpec.addStatement("throw new $T()", UnsupportedOperationException.class);
                }

                specBuilder.addMethod(acceptMethodSpec.build());
            }
        }
    }

    private boolean hasInputStreamOperations(Collection<OperationModel> opModels, ShapeModel eventStream) {
        return opModels.stream()
                       .anyMatch(op -> EventStreamUtils.doesShapeContainsEventStream(op.getInputShape(), eventStream));
    }

    private List<OperationModel> findOutputEventStreamOperations(Collection<OperationModel> opModels,
                                                                 ShapeModel eventStream) {
        return opModels
            .stream()
            .filter(opModel -> EventStreamUtils.doesShapeContainsEventStream(opModel.getOutputShape(), eventStream))
            .collect(Collectors.toList());
    }

    private CodeBlock buildUnknownEventStreamInitializer(Collection<OperationModel> outputOperations,
                                                         ClassName eventStreamModelClass) {
        CodeBlock.Builder builder = CodeBlock.builder()
                                             .add("new $T() {\n"
                                                  + "        @Override\n"
                                                  + "        public $T<$T<?>> sdkFields() {\n"
                                                  + "            return $T.emptyList();\n"
                                                  + "        }\n",
                                                  eventStreamModelClass, List.class, SdkField.class,
                                                  Collections.class
                                             );

        for (OperationModel opModel : outputOperations) {
            ClassName responseHandlerClass = poetExtensions.eventStreamResponseHandlerType(opModel);
            builder.add("        @Override\n"
                      + "        public void accept($T.Visitor visitor) {"
                      + "            \nvisitor.visitDefault(this);\n"
                      + "        }\n", responseHandlerClass);
        }

        builder.add("    }\n");

        return builder.build();
    }

    private MethodSpec sdkFieldsMethod() {
        ParameterizedTypeName sdkFieldType = ParameterizedTypeName.get(ClassName.get(SdkField.class),
                                                                       WildcardTypeName.subtypeOf(ClassName.get(Object.class)));
        return MethodSpec.methodBuilder("sdkFields")
                         .addModifiers(PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(ParameterizedTypeName.get(ClassName.get(List.class), sdkFieldType))
                         .addCode("return SDK_FIELDS;")
                         .build();
    }

    private MethodSpec sdkFieldNameToFieldMethod() {
        ParameterizedTypeName sdkFieldType = ParameterizedTypeName.get(ClassName.get(SdkField.class),
                                                                       WildcardTypeName.subtypeOf(ClassName.get(Object.class)));
        return MethodSpec.methodBuilder("sdkFieldNameToField")
                         .addModifiers(PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), sdkFieldType))
                         .addCode("return SDK_NAME_TO_FIELD;")
                         .build();
    }

    private MethodSpec getterCreator() {
        TypeVariableName t = TypeVariableName.get("T");
        return MethodSpec.methodBuilder("getter")
                         .addTypeVariable(t)
                         .addModifiers(Modifier.PRIVATE, STATIC)
                         .addParameter(ParameterizedTypeName.get(ClassName.get(Function.class),
                                                                 className(), t),
                                       "g")
                         .returns(ParameterizedTypeName.get(ClassName.get(Function.class),
                                                            ClassName.get(Object.class), t))
                         .addStatement("return obj -> g.apply(($T) obj)", className())
                         .build();
    }

    private MethodSpec setterCreator() {
        TypeVariableName t = TypeVariableName.get("T");
        return MethodSpec.methodBuilder("setter")
                         .addTypeVariable(t)
                         .addModifiers(Modifier.PRIVATE, STATIC)
                         .addParameter(ParameterizedTypeName.get(ClassName.get(BiConsumer.class),
                                                                 builderClassName(),
                                                                 t),
                                       "s")
                         .returns(ParameterizedTypeName.get(ClassName.get(BiConsumer.class),
                                                            ClassName.get(Object.class), t))
                         .addStatement("return (obj, val) -> s.accept(($T) obj, val)", builderClassName())
                         .build();
    }

    private MethodSpec.Builder acceptMethodSpec(ClassName modelClass, ClassName responseHandlerClass) {
        return MethodSpec.methodBuilder("accept")
                         .addModifiers(PUBLIC)
                         .addJavadoc(new DocumentationBuilder()
                                         .description("Calls the appropriate visit method depending on "
                                                      + "the subtype of {@link $T}.")
                                         .param("visitor", "Visitor to invoke.")
                                         .build(), modelClass)
                         .addParameter(responseHandlerClass
                                           .nestedClass("Visitor"), "visitor");
    }

    @Override
    public ClassName className() {
        return shapeModelSpec.className();
    }

    private ClassName builderClassName() {
        return className().nestedClass("Builder");
    }

    private ClassName unionTypeClassName() {
        return className().nestedClass("Type");
    }

    private List<TypeName> modelSuperInterfaces() {
        List<TypeName> interfaces = new ArrayList<>();


        switch (shapeModel.getShapeType()) {
            case Model:
                interfaces.add(ClassName.get(SdkPojo.class));
                interfaces.add(ClassName.get(Serializable.class));
                interfaces.add(toCopyableBuilderInterface());
                break;
            case Exception:
            case Request:
            case Response:
                interfaces.add(toCopyableBuilderInterface());
                break;
            default:
                break;
        }

        return interfaces;
    }

    private TypeName modelSuperClass() {
        switch (shapeModel.getShapeType()) {
            case Request:
                return requestBaseClass();
            case Response:
                return responseBaseClass();
            case Exception:
                return exceptionBaseClass();
            default:
                return ClassName.OBJECT;
        }
    }

    private TypeName requestBaseClass() {
        return new AwsServiceBaseRequestSpec(intermediateModel).className();
    }

    private TypeName responseBaseClass() {
        return new AwsServiceBaseResponseSpec(intermediateModel).className();
    }

    private ClassName exceptionBaseClass() {
        String customExceptionBase = intermediateModel.getCustomizationConfig()
                                                      .getSdkModeledExceptionBaseClassName();
        if (customExceptionBase != null) {
            return poetExtensions.getModelClass(customExceptionBase);
        }
        return poetExtensions.getModelClass(intermediateModel.getSdkModeledExceptionBaseClassName());
    }

    private TypeName toCopyableBuilderInterface() {
        return ParameterizedTypeName.get(ClassName.get(ToCopyableBuilder.class),
                                         className().nestedClass("Builder"),
                                         className());
    }

    private List<MethodSpec> modelClassMethods() {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        switch (shapeModel.getShapeType()) {
            case Exception:
                methodSpecs.add(exceptionConstructor());
                methodSpecs.add(toBuilderMethod());
                methodSpecs.add(builderMethod());
                methodSpecs.add(serializableBuilderClass());
                methodSpecs.addAll(memberGetters());
                methodSpecs.addAll(retryableOverrides());
                break;
            default:
                methodSpecs.addAll(addModifier(memberGetters(), FINAL));
                methodSpecs.add(constructor());
                methodSpecs.add(toBuilderMethod());
                methodSpecs.add(builderMethod());
                methodSpecs.add(serializableBuilderClass());
                methodSpecs.add(addModifier(modelMethodOverrides.hashCodeMethod(shapeModel), FINAL));
                methodSpecs.add(addModifier(modelMethodOverrides.equalsMethod(shapeModel), FINAL));
                methodSpecs.add(addModifier(modelMethodOverrides.equalsBySdkFieldsMethod(shapeModel), FINAL));
                methodSpecs.add(addModifier(modelMethodOverrides.toStringMethod(shapeModel), FINAL));
                methodSpecs.add(getValueForField());
                methodSpecs.addAll(unionMembers());
                break;
        }

        if (isEvent()) {
            methodSpecs.add(copyMethod());
        }

        return methodSpecs;
    }

    private MethodSpec getValueForField() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getValueForField")
                                                     .addModifiers(PUBLIC, FINAL)
                                                     .addTypeVariable(TypeVariableName.get("T"))
                                                     .returns(ParameterizedTypeName.get(ClassName.get(Optional.class),
                                                                                        TypeVariableName.get("T")))
                                                     .addParameter(String.class, "fieldName")
                                                     .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class),
                                                                                             TypeVariableName.get("T")),
                                                                   "clazz");

        if (shapeModel.getNonStreamingMembers().isEmpty()) {
            methodBuilder.addStatement("return $T.empty()", Optional.class);
            return methodBuilder.build();
        }


        methodBuilder.beginControlFlow("switch ($L)", "fieldName");

        shapeModel.getNonStreamingMembers().forEach(m -> addCasesForMember(methodBuilder, m));

        methodBuilder.addCode("default:");
        methodBuilder.addStatement("return $T.empty()", Optional.class);
        methodBuilder.endControlFlow();

        return methodBuilder.build();
    }

    private FieldSpec unionTypeField() {
        return FieldSpec.builder(unionTypeClassName(), "type", PRIVATE, FINAL).build();
    }

    private Collection<MethodSpec> unionMembers() {
        if (!shapeModel.isUnion()) {
            return emptyList();
        }

        Validate.isFalse(shapeModel.isEvent(), "Event shape %s must not be a union", shapeModel.getShapeName());
        Validate.isFalse(shapeModel.isEventStream(), "Event stream shape %s must not be a union", shapeModel.getShapeName());
        Validate.isFalse(shapeModel.isDocument(), "Document shape %s must not be a union", shapeModel.getShapeName());
        Validate.isFalse(isRequest() && isResponse(), "Input or output shape %s must not be a union", shapeModel.getShapeName());

        List<MethodSpec> unionMembers = new ArrayList<>();
        unionMembers.addAll(unionConstructors());
        if (intermediateModel.getCustomizationConfig().getGenerateFastUnionConstructors()) {
            unionMembers.addAll(fastUnionConstructors());
        }
        unionMembers.add(unionTypeMethod());
        unionMembers.addAll(unionAcceptMethods());
        return unionMembers;
    }

    /**
     * Fast direct-construction factories that bypass the builder for union shapes. Each produces an object
     * identical to builder().member(v).build() (same fields, same union type, collection members defensively
     * copied), but with one allocation and no union-type resolution. Gated by generateFastUnionConstructors.
     */
    private Collection<MethodSpec> fastUnionConstructors() {
        List<MemberModel> members = shapeModel.getMembers();
        List<MethodSpec> methods = new ArrayList<>();

        // One all-args constructor; its full signature avoids the collisions a per-member constructor
        // would hit where members share an erased type (S/N, SS/NS, BOOL/NUL).
        MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                                            .addModifiers(PRIVATE)
                                            .addParameter(unionTypeClassName(), "type");
        ctor.addStatement("this.type = type");
        for (MemberModel m : members) {
            String fName = m.getVariable().getVariableName();
            ctor.addParameter(typeProvider.typeName(m, new TypeNameOptions().useEnumTypes(false)), fName);
            ctor.addStatement("this.$N = $N", fName, fName);
        }
        methods.add(ctor.build());

        // Per-member factory: one allocation, builder-identical. A null (or sentinel) value yields type()
        // UNKNOWN_TO_SDK_VERSION, matching the builder; list/map values are defensively copied.
        for (MemberModel member : members) {
            String memberName = member.getVariable().getVariableName();
            String factoryName = "fast" + capitalize(member.getFluentSetterMethodName());
            TypeName paramType = typeProvider.typeName(member, new TypeNameOptions().useEnumTypes(false));
            boolean isCollection = member.isList() || member.isMap();

            MethodSpec.Builder factory = MethodSpec.methodBuilder(factoryName)
                                                   .addModifiers(PUBLIC, STATIC)
                                                   .returns(className())
                                                   .addParameter(paramType, memberName);

            String slotValue = memberName;
            if (isCollection) {
                Optional<ClassName> copier = serviceModelCopiers.copierClassFor(member);
                if (copier.isPresent()) {
                    factory.addStatement("$T copied = $T.$N($N)", paramType, copier.get(),
                                         serviceModelCopiers.copyMethodName(), memberName);
                    slotValue = "copied";
                }
                ClassName autoConstruct = member.isMap()
                    ? ClassName.get("software.amazon.awssdk.core.util", "SdkAutoConstructMap")
                    : ClassName.get("software.amazon.awssdk.core.util", "SdkAutoConstructList");
                factory.beginControlFlow("if ($N instanceof $T)", slotValue, autoConstruct);
            } else {
                factory.beginControlFlow("if ($N == null)", memberName);
            }

            CodeBlock notSet = fastCtorCall(members, member, slotValue, true);
            factory.addStatement("$L", notSet);
            factory.endControlFlow();
            factory.addStatement("$L", fastCtorCall(members, member, slotValue, false));

            methods.add(factory.build());
        }
        return methods;
    }

    /**
     * Builds the {@code return new <Class>(type, slot...)} statement for a fast union factory. {@code notSet}
     * selects UNKNOWN_TO_SDK_VERSION (mirrors the builder for a null/sentinel value) vs the member's own type.
     * {@code slotValue} is the expression placed in the target member's slot (the copied local for collections).
     */
    private CodeBlock fastCtorCall(List<MemberModel> members, MemberModel target, String slotValue, boolean notSet) {
        CodeBlock.Builder args = CodeBlock.builder();
        if (notSet) {
            args.add("$T.UNKNOWN_TO_SDK_VERSION", unionTypeClassName());
        } else {
            args.add("$T.$N", unionTypeClassName(), target.getUnionEnumTypeName());
        }
        for (MemberModel m : members) {
            String fName = m.getVariable().getVariableName();
            if (fName.equals(target.getVariable().getVariableName())) {
                args.add(", $N", slotValue);
            } else if (m.isList() || m.isMap()) {
                ClassName sentinel = m.isMap()
                    ? ClassName.get("software.amazon.awssdk.core.util", "DefaultSdkAutoConstructMap")
                    : ClassName.get("software.amazon.awssdk.core.util", "DefaultSdkAutoConstructList");
                args.add(", $T.getInstance()", sentinel);
            } else {
                args.add(", null");
            }
        }
        return CodeBlock.of("return new $T($L)", className(), args.build());
    }

    private Collection<MethodSpec> unionConstructors() {
        return shapeModel.getMembers().stream()
                         .flatMap(this::unionConstructors)
                         .collect(Collectors.toList());
    }

    private Stream<MethodSpec> unionConstructors(MemberModel member) {
        List<MethodSpec> unionConstructors = new ArrayList<>();

        String memberName = member.getFluentSetterMethodName();
        String methodName = "from" + capitalize(memberName);

        unionConstructors.add(MethodSpec.methodBuilder(methodName)
                                        .addJavadoc("$L", member.getUnionConstructorDocumentation())
                                        .addModifiers(PUBLIC, STATIC)
                                        .returns(className())
                                        .addParameter(typeProvider.typeName(member, new TypeNameOptions().useEnumTypes(false)),
                                                      memberName)
                                        .addCode(CodeBlock.of("return builder()." + memberName + "(" + memberName + ").build();"))
                                        .build());

        if (member.getFluentEnumSetterMethodName() != null) {
            unionConstructors.add(MethodSpec.methodBuilder("from" + capitalize(member.getFluentEnumSetterMethodName()))
                                            .addJavadoc("$L", member.getUnionConstructorDocumentation())
                                            .addModifiers(PUBLIC, STATIC)
                                            .returns(className())
                                            .addParameter(typeProvider.typeName(member, new TypeNameOptions().useEnumTypes(true)),
                                                          memberName)
                                            .addCode(CodeBlock.of("return builder()." + member.getFluentEnumSetterMethodName() +
                                                                  "(" + memberName + ").build();"))
                                            .build());
        }

        // Include a consumer-builder if the inner types are structures
        if (!member.isSimple() && !member.isList() && !member.isMap()) {
            TypeName memberType = typeProvider.typeName(member);
            ClassName memberClass = Validate.isInstanceOf(ClassName.class, memberType,
                                                          "Non-simple TypeName was not represented as a ClassName: %s",
                                                          memberType);
            ClassName memberClassBuilder = memberClass.nestedClass("Builder");
            unionConstructors.add(MethodSpec.methodBuilder(methodName)
                                            .addJavadoc("$L", member.getUnionConstructorDocumentation())
                                            .addModifiers(PUBLIC, STATIC)
                                            .returns(className())
                                            .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                                                                    memberClassBuilder),
                                                          memberName)
                                            .addCode(CodeBlock.builder()
                                                              .add("$T builder = $T.builder();", memberClassBuilder, memberClass)
                                                              .add("$L.accept(builder);", memberName)
                                                              .add("return $L(builder.build());", methodName)
                                                              .build())
                                            .build());
        }

        return unionConstructors.stream();
    }

    private MethodSpec unionTypeMethod() {
        return MethodSpec.methodBuilder("type")
                         .addJavadoc("$L", shapeModel.getUnionTypeGetterDocumentation())
                         .addModifiers(PUBLIC)
                         .returns(unionTypeClassName())
                         .addCode("return type;")
                         .build();
    }

    private Collection<MethodSpec> unionAcceptMethods() {
        return emptyList();
    }

    private void addCasesForMember(MethodSpec.Builder methodBuilder, MemberModel member) {
        methodBuilder.addCode("case $S:", member.getC2jName())
                     .addStatement("return $T.ofNullable(clazz.cast($L()))",
                                   Optional.class,
                                   member.getFluentGetterMethodName());

        if (shouldGenerateDeprecatedNameGetter(member)) {
            methodBuilder.addCode("case $S:", member.getDeprecatedName())
                         .addStatement("return $T.ofNullable(clazz.cast($L()))",
                                       Optional.class,
                                       member.getFluentGetterMethodName());
        }
    }

    private List<MethodSpec> memberGetters() {
        return shapeModel.getNonStreamingMembers().stream()
                         .filter(m -> !m.getHttp().getIsStreaming())
                         .flatMap(this::memberGetters)
                         .collect(Collectors.toList());
    }

    private Stream<MethodSpec> memberGetters(MemberModel member) {
        List<MethodSpec> result = new ArrayList<>();

        if (shouldGenerateEnumGetter(member)) {
            result.add(enumMemberGetter(member));
        }

        member.getAutoConstructClassIfExists()
              .ifPresent(autoConstructClass -> result.add(existenceCheckGetter(member, autoConstructClass)));

        if (shouldGenerateDeprecatedNameGetter(member)) {
            result.add(deprecatedMemberGetter(member));
        }

        result.add(memberGetter(member));

        return checkDeprecated(member, result).stream();
    }

    private boolean shouldGenerateDeprecatedNameGetter(MemberModel member) {
        return StringUtils.isNotBlank(member.getDeprecatedName());
    }

    private boolean shouldGenerateEnumGetter(MemberModel member) {
        return member.getEnumType() != null || MemberCopierSpec.isEnumCopyAvailable(member);
    }

    private MethodSpec enumMemberGetter(MemberModel member) {
        return MethodSpec.methodBuilder(member.getFluentEnumGetterMethodName())
                         .addJavadoc("$L", member.getGetterDocumentation())
                         .addModifiers(PUBLIC)
                         .returns(typeProvider.enumReturnType(member))
                         .addCode(enumGetterStatement(member))
                         .build();
    }

    private MethodSpec memberGetter(MemberModel member) {
        return MethodSpec.methodBuilder(member.getFluentGetterMethodName())
                         .addJavadoc("$L", member.getGetterDocumentation())
                         .addModifiers(PUBLIC)
                         .returns(typeProvider.returnType(member))
                         .addCode(getterStatement(member))
                         .build();
    }

    private MethodSpec existenceCheckGetter(MemberModel member, ClassName autoConstructClass) {
        return MethodSpec.methodBuilder(member.getExistenceCheckMethodName())
                         .addJavadoc("$L", member.getExistenceCheckDocumentation())
                         .addModifiers(PUBLIC)
                         .returns(TypeName.BOOLEAN)
                         .addCode(existenceCheckStatement(member, autoConstructClass))
                         .build();
    }

    private CodeBlock existenceCheckStatement(MemberModel member, ClassName autoConstructClass) {
        String variableName = member.getVariable().getVariableName();
        return CodeBlock.of("return $N != null && !($N instanceof $T);", variableName, variableName, autoConstructClass);
    }

    private MethodSpec deprecatedMemberGetter(MemberModel member) {
        return MethodSpec.methodBuilder(member.getDeprecatedFluentGetterMethodName())
                         .addJavadoc("$L", member.getDeprecatedGetterDocumentation())
                         .addModifiers(PUBLIC)
                         .addAnnotation(Deprecated.class)
                         .returns(typeProvider.returnType(member))
                         .addCode(getterStatement(member))
                         .build();
    }

    private CodeBlock enumGetterStatement(MemberModel member) {
        String fieldName = member.getVariable().getVariableName();
        if (member.isList() || member.isMap()) {
            Optional<ClassName> copier = serviceModelCopiers.copierClassFor(member);
            if (!copier.isPresent()) {
                throw new IllegalStateException("Don't know how to copy " + fieldName + " with enum elements!");
            }
            return CodeBlock.of("return $T.$N($N);", copier.get(), serviceModelCopiers.stringToEnumCopyMethodName(), fieldName);
        } else {
            ClassName enumClass = poetExtensions.getModelClass(member.getEnumType());
            return CodeBlock.of("return $T.fromValue($N);", enumClass, fieldName);
        }
    }

    private CodeBlock getterStatement(MemberModel model) {
        VariableModel modelVariable = model.getVariable();
        return CodeBlock.of("return $N;", modelVariable.getVariableName());
    }

    private List<MethodSpec> retryableOverrides() {
        if (shapeModel.isRetryable()) {
            MethodSpec isRetryable = MethodSpec.methodBuilder("isRetryableException")
                                               .addAnnotation(Override.class)
                                               .addModifiers(PUBLIC)
                                               .returns(TypeName.BOOLEAN)
                                               .addStatement("return true")
                                               .build();
            if (shapeModel.isThrottling()) {
                MethodSpec isThrottling = MethodSpec.methodBuilder("isThrottlingException")
                                                   .addAnnotation(Override.class)
                                                   .addModifiers(PUBLIC)
                                                   .returns(TypeName.BOOLEAN)
                                                   .addStatement("return true")
                                                   .build();
                return Arrays.asList(isRetryable, isThrottling);
            }
            return Arrays.asList(isRetryable);
        }
        return emptyList();
    }

    private List<TypeSpec> nestedModelClassTypes() {
        List<TypeSpec> nestedClasses = new ArrayList<>();
        switch (shapeModel.getShapeType()) {
            case Model:
            case Request:
            case Response:
            case Exception:
                nestedClasses.add(modelBuilderSpecs.builderInterface());
                nestedClasses.add(modelBuilderSpecs.beanStyleBuilder());
                break;
            default:
                break;
        }

        if (shapeModel.isUnion()) {
            nestedClasses.add(modelBuilderSpecs.unionTypeClass());
        }

        return nestedClasses;
    }

    private MethodSpec constructor() {
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                                                   .addParameter(modelBuilderSpecs.builderImplName(), "builder");

        if (shapeModel.isEvent()) {
            ctorBuilder.addModifiers(Modifier.PROTECTED);
        } else {
            ctorBuilder.addModifiers(PRIVATE);
        }

        if (isRequest() || isResponse()) {
            ctorBuilder.addStatement("super(builder)");
        }

        shapeModelSpec.fields().forEach(f -> ctorBuilder.addStatement("this.$N = builder.$N", f, f));

        if (shapeModel.isUnion()) {
            ctorBuilder.addStatement("this.type = builder.type");
        }

        return ctorBuilder.build();
    }

    private MethodSpec exceptionConstructor() {
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                                                   .addModifiers(Modifier.PRIVATE)
                                                   .addParameter(modelBuilderSpecs.builderImplName(), "builder");

        ctorBuilder.addStatement("super(builder)");

        shapeModelSpec.fields().forEach(f -> ctorBuilder.addStatement("this.$N = builder.$N", f, f));

        return ctorBuilder.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(PUBLIC, STATIC)
                         .returns(modelBuilderSpecs.builderInterfaceName())
                         .addStatement("return new $T()", modelBuilderSpecs.builderImplName())
                         .build();
    }

    private MethodSpec toBuilderMethod() {
        return MethodSpec.methodBuilder("toBuilder")
                         .addModifiers(PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(modelBuilderSpecs.builderInterfaceName())
                         .addStatement("return new $T(this)", modelBuilderSpecs.builderImplName())
                         .build();
    }

    private MethodSpec serializableBuilderClass() {
        return MethodSpec.methodBuilder("serializableBuilderClass")
                         .addModifiers(PUBLIC, STATIC)
                         .returns(ParameterizedTypeName.get(ClassName.get(Class.class),
                                                            WildcardTypeName.subtypeOf(modelBuilderSpecs.builderInterfaceName())))
                         .addStatement("return $T.class", modelBuilderSpecs.builderImplName())
                         .build();
    }

    private MethodSpec copyMethod() {
        ParameterizedTypeName consumerParam = ParameterizedTypeName.get(ClassName.get(Consumer.class),
                WildcardTypeName.supertypeOf(modelBuilderSpecs.builderInterfaceName()));

        return MethodSpec.methodBuilder("copy")
                .addModifiers(PUBLIC, FINAL)
                .addAnnotation(Override.class)
                .addParameter(consumerParam, "modifier")
                .addStatement("return $T.super.copy(modifier)", ToCopyableBuilder.class)
                .returns(className())
                .build();
    }

    private boolean isResponse() {
        return shapeModel.getShapeType() == ShapeType.Response;
    }

    private boolean isRequest() {
        return shapeModel.getShapeType() == ShapeType.Request;
    }

    private boolean isEvent() {
        return shapeModel.isEvent();
    }

    private List<MethodSpec> eventStreamInterfaceEventBuilderMethods() {
        return shapeModel.getMembers().stream()
                .filter(m -> m.getShape().isEvent())
                .map(this::eventBuilderMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec eventBuilderMethod(MemberModel event) {
        EventStreamSpecHelper specHelper = new EventStreamSpecHelper(shapeModel, intermediateModel);
        ClassName eventClassName = specHelper.eventClassName(event);

        ClassName returnType;

        if (specHelper.useLegacyGenerationScheme(event)) {
            returnType = eventClassName.nestedClass("Builder");
        } else {
            ClassName baseClass = poetExtensions.getModelClass(event.getShape().getShapeName());
            returnType = baseClass.nestedClass("Builder");
        }

        String methodName = specHelper.eventBuilderMethodName(event);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(PUBLIC, STATIC)
                .returns(returnType)
                .addJavadoc("Create a builder for the {@code $L} event type for this stream.", event.getC2jName())
                .addStatement("return $T.builder()", eventClassName)
                .build();
    }

    private List<MethodSpec> addModifier(List<MethodSpec> specs, Modifier modifier) {
        return specs.stream()
                .map(spec -> addModifier(spec, modifier))
                .collect(Collectors.toList());
    }

    private MethodSpec addModifier(MethodSpec spec, Modifier modifier) {
        return spec.toBuilder().addModifiers(modifier).build();
    }

    private boolean useLegacyEventGenerationScheme(ShapeModel eventStream) {
        EventStreamSpecHelper helper = new EventStreamSpecHelper(eventStream, intermediateModel);
        // This is hacky, but there's no better solution without
        // extensive refactoring. We basically need to know if any of
        // the *event types* within this even stream have this
        // customization enabled, which requires knowing the
        // MemberModel that has this given shape.
        for (MemberModel member : eventStream.getMembers()) {
            if (member.getShape().equals(shapeModel) && helper.useLegacyGenerationScheme(member)) {
                return true;
            }
        }
        return false;
    }

    private Optional<MemberModel> findLegacyGenerationEventWithShape(ShapeModel eventStream) {
        EventStreamSpecHelper helper = new EventStreamSpecHelper(eventStream, intermediateModel);
        for (MemberModel member : eventStream.getMembers()) {
            if (member.getShape().equals(shapeModel) && helper.useLegacyGenerationScheme(member)) {
                return Optional.ofNullable(member);
            }
        }
        return Optional.empty();
    }
}
