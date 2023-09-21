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

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static software.amazon.awssdk.codegen.poet.model.DeprecationUtils.checkDeprecated;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;

/**
 * Provides the Poet specs for model class builders.
 */
class ModelBuilderSpecs {
    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final TypeProvider typeProvider;
    private final PoetExtension poetExtensions;
    private final AccessorsFactory accessorsFactory;
    private final ShapeModelSpec shapeModelSpec;

    ModelBuilderSpecs(IntermediateModel intermediateModel,
                      ShapeModel shapeModel,
                      TypeProvider typeProvider) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.typeProvider = typeProvider;
        this.poetExtensions = new PoetExtension(this.intermediateModel);
        this.accessorsFactory = new AccessorsFactory(this.shapeModel, this.intermediateModel, this.typeProvider, poetExtensions);
        this.shapeModelSpec = new ShapeModelSpec(shapeModel, typeProvider, poetExtensions, intermediateModel);
    }

    public ClassName builderInterfaceName() {
        return classToBuild().nestedClass("Builder");
    }

    public ClassName builderImplName() {
        return classToBuild().nestedClass("BuilderImpl");
    }

    public TypeSpec builderInterface() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(builderInterfaceName())
                .addSuperinterfaces(builderSuperInterfaces())
                .addModifiers(PUBLIC);

        shapeModel.getNonStreamingMembers()
                  .forEach(m -> {
                      builder.addMethods(
                          checkDeprecated(m, accessorsFactory.fluentSetterDeclarations(m, builderInterfaceName())));
                      builder.addMethods(
                          checkDeprecated(m, accessorsFactory.convenienceSetterDeclarations(m, builderInterfaceName())));
                  });

        if (isException()) {
            builder.addSuperinterface(parentExceptionBuilder().nestedClass("Builder"));
            builder.addMethods(ExceptionProperties.builderInterfaceMethods(builderInterfaceName()));
        }

        if (isRequest()) {
            builder.addMethod(MethodSpec.methodBuilder("overrideConfiguration")
                    .returns(builderInterfaceName())
                    .addAnnotation(Override.class)
                    .addParameter(AwsRequestOverrideConfiguration.class, "overrideConfiguration")
                    .addModifiers(PUBLIC, Modifier.ABSTRACT)
                    .build());

            builder.addMethod(MethodSpec.methodBuilder("overrideConfiguration")
                    .addAnnotation(Override.class)
                    .returns(builderInterfaceName())
                    .addParameter(ParameterizedTypeName.get(Consumer.class, AwsRequestOverrideConfiguration.Builder.class),
                            "builderConsumer")
                    .addModifiers(PUBLIC, Modifier.ABSTRACT)
                    .build());

            builder.addMethod(MethodSpec.methodBuilder("addPlugin")
                                        .addAnnotation(Override.class)
                                        .returns(builderInterfaceName())
                                        .addParameter(SdkPlugin.class , "plugin")
                                        .addModifiers(PUBLIC, Modifier.ABSTRACT)
                                        .build());

        }

        return builder.build();
    }

    public TypeSpec beanStyleBuilder() {
        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(builderImplName())
                .addSuperinterface(builderInterfaceName())
                // TODO: Uncomment this once property shadowing is fixed
                //.addSuperinterface(copyableBuilderSuperInterface())
                .superclass(builderImplSuperClass())
                .addModifiers(Modifier.STATIC);

        if (!isEvent()) {
            builderClassBuilder.addModifiers(Modifier.FINAL);
        } else {
            builderClassBuilder.addModifiers(Modifier.PROTECTED);
        }

        if (isException()) {
            builderClassBuilder.superclass(parentExceptionBuilder().nestedClass("BuilderImpl"));
        }

        builderClassBuilder.addFields(fields());
        builderClassBuilder.addMethod(noargConstructor());
        builderClassBuilder.addMethod(modelCopyConstructor());
        builderClassBuilder.addMethods(accessors());
        builderClassBuilder.addMethod(buildMethod());
        builderClassBuilder.addMethod(sdkFieldsMethod());

        if (shapeModel.isUnion()) {
            builderClassBuilder.addMethod(handleUnionValueChangeMethod());
        }

        return builderClassBuilder.build();
    }

    private ClassName parentExceptionBuilder() {
        String customExceptionBase = intermediateModel.getCustomizationConfig()
                .getSdkModeledExceptionBaseClassName();
        if (customExceptionBase != null) {
            return poetExtensions.getModelClass(customExceptionBase);
        }
        return poetExtensions.getModelClass(intermediateModel.getSdkModeledExceptionBaseClassName());
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

    private TypeName builderImplSuperClass() {
        if (isRequest()) {
            return new AwsServiceBaseRequestSpec(intermediateModel).className().nestedClass("BuilderImpl");
        }

        if (isResponse()) {
            return new AwsServiceBaseResponseSpec(intermediateModel).className().nestedClass("BuilderImpl");
        }

        return ClassName.OBJECT;
    }

    private List<FieldSpec> fields() {
        List<FieldSpec> fields = new ArrayList<>();

        for (MemberModel member : shapeModel.getNonStreamingMembers()) {
            FieldSpec fieldSpec = typeProvider.asField(member, Modifier.PRIVATE);
            if (member.isList()) {
                fieldSpec = fieldSpec.toBuilder()
                                     .initializer("$T.getInstance()", DefaultSdkAutoConstructList.class)
                                     .build();
            } else if (member.isMap()) {
                fieldSpec = fieldSpec.toBuilder()
                                     .initializer("$T.getInstance()", DefaultSdkAutoConstructMap.class)
                                     .build();
            }

            fields.add(fieldSpec);
        }

        if (shapeModel.isUnion()) {
            ClassName unionType = shapeModelSpec.className().nestedClass("Type");
            fields.add(FieldSpec.builder(unionType, "type", PRIVATE)
                                .initializer("$T.UNKNOWN_TO_SDK_VERSION", unionType)
                                .build());
            fields.add(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Set.class), unionType), "setTypes", PRIVATE)
                                .initializer("$T.noneOf($T.class)", EnumSet.class, unionType)
                                .build());
        }

        return fields;
    }

    private MethodSpec noargConstructor() {
        Modifier modifier = isEvent() ? Modifier.PROTECTED : Modifier.PRIVATE;
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(modifier);
        return ctorBuilder.build();
    }

    private MethodSpec modelCopyConstructor() {
        Modifier modifier = isEvent() ? Modifier.PROTECTED : Modifier.PRIVATE;
        MethodSpec.Builder copyBuilderCtor = MethodSpec.constructorBuilder()
                .addModifiers(modifier)
                .addParameter(classToBuild(), "model");

        if (isRequest() || isResponse() || isException()) {
            copyBuilderCtor.addCode("super(model);");
        }

        shapeModel.getNonStreamingMembers().forEach(m -> {
            String name = m.getVariable().getVariableName();
            copyBuilderCtor.addStatement("$N(model.$N)", m.getFluentSetterMethodName(), name);
        });

        return copyBuilderCtor.build();
    }

    private List<MethodSpec> accessors() {
        List<MethodSpec> accessors = new ArrayList<>();
        shapeModel.getNonStreamingMembers()
                  .forEach(m -> {
                      accessors.add(checkDeprecated(m, accessorsFactory.beanStyleGetter(m)));
                      accessors.addAll(checkDeprecated(m, accessorsFactory.beanStyleSetters(m)));
                      accessors.addAll(checkDeprecated(m, accessorsFactory.fluentSetters(m, builderInterfaceName())));
                      accessors.addAll(checkDeprecated(m, accessorsFactory.convenienceSetters(m, builderInterfaceName())));
                  });

        if (isException()) {
            accessors.addAll(ExceptionProperties.builderImplMethods(builderImplName()));
        }

        if (isRequest()) {
            accessors.add(MethodSpec.methodBuilder("overrideConfiguration")
                    .addAnnotation(Override.class)
                    .returns(builderInterfaceName())
                    .addParameter(AwsRequestOverrideConfiguration.class, "overrideConfiguration")
                    .addModifiers(PUBLIC)
                    .addStatement("super.overrideConfiguration(overrideConfiguration)")
                    .addStatement("return this")
                    .build());

            accessors.add(MethodSpec.methodBuilder("overrideConfiguration")
                    .addAnnotation(Override.class)
                    .returns(builderInterfaceName())
                    .addParameter(ParameterizedTypeName.get(Consumer.class, AwsRequestOverrideConfiguration.Builder.class),
                            "builderConsumer")
                    .addModifiers(PUBLIC)
                    .addStatement("super.overrideConfiguration(builderConsumer)")
                    .addStatement("return this")
                    .build());

            accessors.add(MethodSpec.methodBuilder("addPlugin")
                                    .addAnnotation(Override.class)
                                    .returns(builderInterfaceName())
                                    .addParameter(SdkPlugin.class, "plugin")
                                    .addModifiers(PUBLIC)
                                    .addStatement("super.addPlugin(plugin)")
                                    .addStatement("return this")
                                    .build());
        }

        return accessors;
    }

    private MethodSpec buildMethod() {
        return MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(classToBuild())
                .addStatement("return new $T(this)", classToBuild())
                .build();
    }

    private ClassName classToBuild() {
        return shapeModelSpec.className();
    }

    private boolean isException() {
        return shapeModel.getShapeType() == ShapeType.Exception;
    }

    private boolean isRequest() {
        return shapeModel.getShapeType() == ShapeType.Request;
    }

    private boolean isResponse() {
        return shapeModel.getShapeType() == ShapeType.Response;
    }

    private boolean isEvent() {
        return shapeModel.isEvent();
    }

    private List<TypeName> builderSuperInterfaces() {
        List<TypeName> superInterfaces = new ArrayList<>();
        if (isRequest()) {
            superInterfaces.add(new AwsServiceBaseRequestSpec(intermediateModel).className().nestedClass("Builder"));
        }
        if (isResponse()) {
            superInterfaces.add(new AwsServiceBaseResponseSpec(intermediateModel).className().nestedClass("Builder"));
        }
        superInterfaces.add(ClassName.get(SdkPojo.class));
        superInterfaces.add(ParameterizedTypeName.get(ClassName.get(CopyableBuilder.class),
                classToBuild().nestedClass("Builder"), classToBuild()));
        return superInterfaces;
    }

    public TypeSpec unionTypeClass() {
        Validate.isTrue(shapeModel.isUnion(), "%s was not a union.", shapeModel.getShapeName());

        TypeSpec.Builder type = TypeSpec.enumBuilder("Type")
                                        .addJavadoc("@see $L#type()", shapeModel.getShapeName())
                                        .addModifiers(PUBLIC);

        for (MemberModel member : shapeModel.getMembers()) {
            type.addEnumConstant(member.getUnionEnumTypeName());
        }

        type.addEnumConstant("UNKNOWN_TO_SDK_VERSION");

        return type.build();
    }

    private MethodSpec handleUnionValueChangeMethod() {
        CodeBlock body =
            CodeBlock.builder()
                     .beginControlFlow("if (this.type == type || oldValue == newValue)")
                     .addStatement("return")
                     .endControlFlow()
                     .beginControlFlow("if (newValue == null || newValue instanceof $T || newValue instanceof $T)",
                                       SdkAutoConstructList.class, SdkAutoConstructMap.class)
                     .addStatement("setTypes.remove(type)")
                     .nextControlFlow("else if (oldValue == null || oldValue instanceof $T || oldValue instanceof $T)",
                                      SdkAutoConstructList.class, SdkAutoConstructMap.class)
                     .addStatement("setTypes.add(type)")
                     .endControlFlow()
                     .beginControlFlow("if (setTypes.size() == 1)")
                     .addStatement("this.type = setTypes.iterator().next()")
                     .nextControlFlow("else if (setTypes.isEmpty())")
                     .addStatement("this.type = Type.UNKNOWN_TO_SDK_VERSION")
                     .nextControlFlow("else")
                     .addStatement("this.type = null")
                     .endControlFlow()
                     .build();

        return MethodSpec.methodBuilder("handleUnionValueChange")
                         .addModifiers(PRIVATE, FINAL)
                         .returns(void.class)
                         .addParameter(shapeModelSpec.className().nestedClass("Type"), "type")
                         .addParameter(Object.class, "oldValue")
                         .addParameter(Object.class, "newValue")
                         .addCode(body)
                         .build();
    }
}
