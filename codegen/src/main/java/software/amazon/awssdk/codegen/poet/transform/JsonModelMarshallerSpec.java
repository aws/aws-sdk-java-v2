/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.transform;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingInfo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Create ModelMarshaller for Json protocol
 */
public class JsonModelMarshallerSpec implements ClassSpec {

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final ClassName className;
    private final ClassName requestClassName;

    public JsonModelMarshallerSpec(IntermediateModel intermediateModel, ShapeModel shapeModel, String className) {
        PoetExtensions poetExtensions = new PoetExtensions(intermediateModel);
        String modelPackage = intermediateModel.getMetadata().getFullModelPackageName();
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.className = poetExtensions.getTransformClass(shapeModel.getShapeName() + className);
        this.requestClassName = ClassName.get(modelPackage, shapeModel.getShapeName());
    }

    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className)
                       .addModifiers(Modifier.PUBLIC)
                       .addAnnotation(PoetUtils.GENERATED)
                       .addAnnotation(SdkInternalApi.class)
                       .addJavadoc("{@link $T} Marshaller", requestClassName)
                       .addFields(memberVariables())
                       .addMethods(methods())
                       .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private List<MethodSpec> methods() {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        methodSpecs.add(constructor());
        methodSpecs.add(getInstanceMethod());
        methodSpecs.add(marshallMethod());
        return methodSpecs;
    }

    private List<FieldSpec> memberVariables() {
        List<FieldSpec> fields = new ArrayList<>();

        FieldSpec instance = FieldSpec.builder(className, "INSTANCE")
                                      .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                                      .initializer("new $T()", className)
                                      .build();

        for (MemberModel memberModel : shapeModel.getNonStreamingMembers()) {
            TypeName typeName = ParameterizedTypeName.get(ClassName.get(MarshallingInfo.class),
                                                          marshallingTargetClass(memberModel.getMarshallingTargetClass(),
                                                                                 memberModel.getVariable().getVariableType()));

            CodeBlock.Builder initializationCodeBlockBuilder = CodeBlock.builder()
                                                                        .add("$T.builder($T.$L).marshallLocation($T.$L)",
                                                                             MarshallingInfo.class,
                                                                             MarshallingType.class,
                                                                             memberModel.getMarshallingType(),
                                                                             ClassName.get(MarshallLocation.class),
                                                                             memberModel.getHttp().getMarshallLocation());

            if (memberModel.getHttp().getIsPayload()) {
                initializationCodeBlockBuilder.add(".isExplicitPayloadMember($L)", memberModel.getHttp().getIsPayload());
            } else {
                initializationCodeBlockBuilder.add(".marshallLocationName($S)", memberModel.getHttp().getMarshallLocationName());
            }

            initializationCodeBlockBuilder.add(".isBinary($L)", memberModel.getIsBinary());

            Optional.ofNullable(intermediateModel.getCustomizationConfig().getModelMarshallerDefaultValueSupplier())
                    .map(defaultValueSupplier -> defaultValueSupplier.get(memberModel.getName()))
                    .ifPresent(value -> initializationCodeBlockBuilder.add(".defaultValueSupplier($L)", value));

            if (memberModel.isIdempotencyToken()) {
                initializationCodeBlockBuilder.add(".defaultValueSupplier($T.getGenerator())", ClassName.get(IdempotentUtils
                                                                                                                 .class));
            }

            CodeBlock codeBlock = initializationCodeBlockBuilder.add(".build()").build();

            FieldSpec fieldSpec = FieldSpec.builder(typeName, memberModel.getMarshallerBindingFieldName())
                                           .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                                           .initializer(codeBlock)
                                           .build();
            fields.add(fieldSpec);
        }

        fields.add(instance);
        return fields;
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
    }

    private MethodSpec getInstanceMethod() {
        return MethodSpec.methodBuilder("getInstance").addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addStatement("return INSTANCE")
                         .returns(className)
                         .build();
    }

    private MethodSpec marshallMethod() {
        String variableName = shapeModel.getVariable().getVariableName();
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("marshall")
                                                         .addJavadoc("Marshall the given parameter object")
                                                         .addModifiers(Modifier.PUBLIC)
                                                         .addParameter(requestClassName, variableName)
                                                         .addParameter(ProtocolMarshaller.class, "protocolMarshaller");

        if (shapeModel.getNonStreamingMembers().isEmpty()) {
            return methodSpecBuilder.build();
        }

        methodSpecBuilder.addStatement("$T.paramNotNull($L, $S)", ClassName.get(Validate.class), variableName, variableName);
        methodSpecBuilder.addStatement("$T.paramNotNull($L, $S)", ClassName.get(Validate.class), "protocolMarshaller",
                                       "protocolMarshaller");

        methodSpecBuilder.beginControlFlow("try");
        shapeModel.getNonStreamingMembers().forEach(
            memberModel -> methodSpecBuilder.addStatement("protocolMarshaller.marshall($L.$L(), $L)",
                                                          variableName,
                                                          memberModel.getFluentGetterMethodName(),
                                                          memberModel.getMarshallerBindingFieldName()));

        methodSpecBuilder.endControlFlow();
        methodSpecBuilder.beginControlFlow("catch (Exception e)");
        methodSpecBuilder.addStatement("throw new $T(\"Unable to marshall request to JSON: \" + e.getMessage(), e)", ClassName
            .get(SdkClientException.class));
        methodSpecBuilder.endControlFlow();
        return methodSpecBuilder.build();
    }

    private ClassName marshallingTargetClass(String marshallerTargetClass, String variableType) {
        if ("List".equals(marshallerTargetClass)) {
            return ClassName.get(List.class);
        } else if ("String".equals(marshallerTargetClass)) {
            return ClassName.get(String.class);
        } else if ("Map".equals(marshallerTargetClass)) {
            return ClassName.get(Map.class);
        } else if ("StructuredPojo".equals(marshallerTargetClass)) {
            return ClassName.get(StructuredPojo.class);
        } else {
            return ClassName.bestGuess(variableType);
        }
    }
}
