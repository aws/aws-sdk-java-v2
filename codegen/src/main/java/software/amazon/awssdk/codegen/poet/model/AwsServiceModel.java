/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.core.runtime.TypeConverter;

/**
 * Provides the Poet specs for AWS Service models.
 */
public class AwsServiceModel implements ClassSpec {

    private final ShapeModel shapeModel;
    private final PoetExtensions poetExtensions;
    private final TypeProvider typeProvider;
    private final ShapeModelSpec shapeModelSpec;
    private final ShapeInterfaceProvider interfaceProvider;
    private final ModelMethodOverrides modelMethodOverrides;
    private final ModelBuilderSpecs modelBuilderSpecs;

    public AwsServiceModel(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.shapeModel = shapeModel;
        this.poetExtensions = new PoetExtensions(intermediateModel);
        this.typeProvider = new TypeProvider(intermediateModel);
        this.shapeModelSpec = new ShapeModelSpec(this.shapeModel, typeProvider, poetExtensions);
        this.interfaceProvider = new AwsShapePublicInterfaceProvider(intermediateModel, this.shapeModel);
        this.modelMethodOverrides = new ModelMethodOverrides(this.poetExtensions);
        this.modelBuilderSpecs = new ModelBuilderSpecs(intermediateModel, this.shapeModel, this.shapeModelSpec,
                                                       this.typeProvider);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(shapeModel.getShapeName())
                                               .addModifiers(Modifier.PUBLIC)
                                               .addAnnotation(PoetUtils.GENERATED)
                                               .addSuperinterfaces(modelSuperInterfaces())
                                               .superclass(modelSuperClass())
                                               .addMethods(modelClassMethods())
                                               .addFields(shapeModelSpec.fields())
                                               .addTypes(nestedModelClassTypes());

        if (shapeModel.getDocumentation() != null) {
            specBuilder.addJavadoc("$L", shapeModel.getDocumentation());
        }

        return specBuilder.build();
    }

    @Override
    public ClassName className() {
        return shapeModelSpec.className();
    }

    private List<TypeName> modelSuperInterfaces() {
        return interfaceProvider.interfacesToImplement().stream()
                                .sorted(Comparator.comparing(TypeName::toString))
                                .collect(Collectors.toList());
    }

    private TypeName modelSuperClass() {
        return interfaceProvider.baseClassToExtend();
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
                break;
            default:
                methodSpecs.addAll(memberGetters());
                methodSpecs.add(constructor());
                methodSpecs.add(toBuilderMethod());
                methodSpecs.add(builderMethod());
                methodSpecs.add(serializableBuilderClass());
                methodSpecs.add(modelMethodOverrides.hashCodeMethod(shapeModel));
                methodSpecs.add(modelMethodOverrides.equalsMethod(shapeModel));
                methodSpecs.add(modelMethodOverrides.toStringMethod(shapeModel));
                methodSpecs.add(getValueForField());
                break;
        }

        if (implementStructuredPojoInterface()) {
            methodSpecs.add(structuredPojoMarshallMethod(shapeModel));
        }

        return methodSpecs;
    }

    private MethodSpec getValueForField() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getValueForField")
                                                     .addModifiers(Modifier.PUBLIC)
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

        shapeModel.getNonStreamingMembers().forEach(m -> methodBuilder.addCode("case $S:", m.getC2jName())
                                                                      .addStatement("return $T.of(clazz.cast($L()))",
                                                                                    Optional.class,
                                                                                    m.getFluentGetterMethodName()));

        methodBuilder.addCode("default:");
        methodBuilder.addStatement("return $T.empty()", Optional.class);
        methodBuilder.endControlFlow();

        return methodBuilder.build();
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

        result.add(memberGetter(member));

        return result.stream();
    }

    private boolean shouldGenerateEnumGetter(MemberModel member) {
        return Utils.isOrContainsEnum(member);
    }

    private MethodSpec enumMemberGetter(MemberModel member) {
        return MethodSpec.methodBuilder(member.getFluentEnumGetterMethodName())
                         .addJavadoc("$L", member.getGetterDocumentation())
                         .addModifiers(Modifier.PUBLIC)
                         .returns(typeProvider.enumReturnType(member))
                         .addCode(enumGetterStatement(member))
                         .build();
    }

    private MethodSpec memberGetter(MemberModel member) {
        return MethodSpec.methodBuilder(member.getFluentGetterMethodName())
                         .addJavadoc("$L", member.getGetterDocumentation())
                         .addModifiers(Modifier.PUBLIC)
                         .returns(typeProvider.returnType(member))
                         .addCode(getterStatement(member))
                         .build();
    }

    private CodeBlock enumGetterStatement(MemberModel member) {
        String fieldName = member.getVariable().getVariableName();

        if (member.isList()) {
            ClassName valueEnumClass = poetExtensions.getModelClass(member.getListModel().getListMemberModel().getEnumType());
            return CodeBlock.of("return $T.convert($N, $T::fromValue);", TypeConverter.class, fieldName, valueEnumClass);
        } else if (member.isMap()) {
            MapModel mapModel = member.getMapModel();
            String keyEnumType = mapModel.getKeyModel().getEnumType();
            String valueEnumType = mapModel.getValueModel().getEnumType();

            CodeBlock keyConverter = keyEnumType != null ? enumConverterFunction(poetExtensions.getModelClass(keyEnumType))
                                                         : identityFunction();
            CodeBlock valueConverter = valueEnumType != null ? enumConverterFunction(poetExtensions.getModelClass(valueEnumType))
                                                             : identityFunction();

            CodeBlock entryPredicate = mapEntryFilter(keyEnumType);

            return CodeBlock.builder()
                            .add("return $T.convert($N, ", TypeConverter.class, fieldName)
                            .add(keyConverter).add(", ")
                            .add(valueConverter).add(", ")
                            .add(entryPredicate).add(");")
                            .build();
        } else {
            ClassName enumClass = poetExtensions.getModelClass(member.getEnumType());
            return CodeBlock.of("return $T.fromValue($N);", enumClass, fieldName);
        }
    }

    private CodeBlock mapEntryFilter(String keyEnumType) {
        // Don't include UNKNOWN_TO_SDK_VERSION keys in the enum map. Customers should use the string version to get at that data.
        return keyEnumType != null ? CodeBlock.of("(k, v) -> !$T.equals(k, $T.UNKNOWN_TO_SDK_VERSION)",
                                                  Objects.class, poetExtensions.getModelClass(keyEnumType))
                                   : CodeBlock.of("(k, v) -> true");
    }

    private CodeBlock enumConverterFunction(ClassName enumClass) {
        return CodeBlock.of("$T::fromValue", enumClass);
    }

    private CodeBlock identityFunction() {
        return CodeBlock.of("$T.identity()", Function.class);
    }

    private CodeBlock getterStatement(MemberModel model) {
        VariableModel modelVariable = model.getVariable();

        if ("java.nio.ByteBuffer".equals(modelVariable.getVariableType())) {
            return CodeBlock.of("return $1N == null ? null : $1N.asReadOnlyBuffer();", modelVariable.getVariableName());
        }

        return CodeBlock.of("return $N;", modelVariable.getVariableName());
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
        return nestedClasses;
    }

    private boolean implementStructuredPojoInterface() {
        return interfaceProvider.shouldImplementInterface(StructuredPojo.class);
    }

    private MethodSpec structuredPojoMarshallMethod(ShapeModel shapeModel) {
        return MethodSpec.methodBuilder("marshall")
                         .addAnnotation(SdkInternalApi.class)
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ProtocolMarshaller.class, "protocolMarshaller")
                         .addStatement("$T.getInstance().marshall(this, $N)",
                                       poetExtensions.getTransformClass(shapeModel.getShapeName() + "Marshaller"),
                                       "protocolMarshaller")
                         .build();
    }

    private MethodSpec constructor() {
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                                                   .addModifiers(Modifier.PRIVATE)
                                                   .addParameter(modelBuilderSpecs.builderImplName(), "builder");

        shapeModelSpec.fields().forEach(f -> ctorBuilder.addStatement("this.$N = builder.$N", f, f));

        return ctorBuilder.build();
    }

    private MethodSpec exceptionConstructor() {
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                                                   .addModifiers(Modifier.PRIVATE)
                                                   .addParameter(modelBuilderSpecs.builderImplName(), "builder");

        ctorBuilder.addStatement("super(builder.message)");

        shapeModelSpec.fields().forEach(f -> ctorBuilder.addStatement("this.$N = builder.$N", f, f));

        return ctorBuilder.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(modelBuilderSpecs.builderInterfaceName())
                         .addStatement("return new $T()", modelBuilderSpecs.builderImplName())
                         .build();
    }

    private MethodSpec toBuilderMethod() {
        return MethodSpec.methodBuilder("toBuilder")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(modelBuilderSpecs.builderInterfaceName())
                         .addStatement("return new $T(this)", modelBuilderSpecs.builderImplName())
                         .build();
    }

    private MethodSpec serializableBuilderClass() {
        return MethodSpec.methodBuilder("serializableBuilderClass")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(ParameterizedTypeName.get(ClassName.get(Class.class),
                                                            WildcardTypeName.subtypeOf(modelBuilderSpecs.builderInterfaceName())))
                         .addStatement("return $T.class", modelBuilderSpecs.builderImplName())
                         .build();
    }
}
