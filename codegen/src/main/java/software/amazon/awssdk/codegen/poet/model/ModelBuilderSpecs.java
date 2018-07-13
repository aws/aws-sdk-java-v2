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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.utils.builder.CopyableBuilder;

/**
 * Provides the Poet specs for model class builders.
 */
class ModelBuilderSpecs {
    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;
    private final AccessorsFactory accessorsFactory;

    ModelBuilderSpecs(IntermediateModel intermediateModel,
                      ShapeModel shapeModel,
                      TypeProvider typeProvider) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.typeProvider = typeProvider;
        this.poetExtensions = new PoetExtensions(this.intermediateModel);
        this.accessorsFactory = new AccessorsFactory(this.shapeModel, this.intermediateModel, this.typeProvider, poetExtensions);
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
                .addModifiers(Modifier.PUBLIC);

        shapeModel.getNonStreamingMembers()
                  .forEach(m -> {
                      builder.addMethods(accessorsFactory.fluentSetterDeclarations(m, builderInterfaceName()));
                      builder.addMethods(accessorsFactory.convenienceSetterDeclarations(m, builderInterfaceName()));
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
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .build());

            builder.addMethod(MethodSpec.methodBuilder("overrideConfiguration")
                    .addAnnotation(Override.class)
                    .returns(builderInterfaceName())
                    .addParameter(ParameterizedTypeName.get(Consumer.class, AwsRequestOverrideConfiguration.Builder.class),
                            "builderConsumer")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .build());
        }

        return builder.build();
    }

    private ClassName parentExceptionBuilder() {
        final String customExceptionBase = intermediateModel.getCustomizationConfig()
                .getSdkModeledExceptionBaseClassName();
        if (customExceptionBase != null) {
            return poetExtensions.getModelClass(customExceptionBase);
        }
        return poetExtensions.getModelClass(intermediateModel.getSdkModeledExceptionBaseClassName());
    }

    public TypeSpec beanStyleBuilder() {
        TypeSpec.Builder builderClassBuilder = TypeSpec.classBuilder(builderImplName())
                .addSuperinterface(builderInterfaceName())
                // TODO: Uncomment this once property shadowing is fixed
                //.addSuperinterface(copyableBuilderSuperInterface())
                .superclass(builderImplSuperClass())
                .addModifiers(Modifier.STATIC, Modifier.FINAL);

        if (isException()) {
            builderClassBuilder.superclass(parentExceptionBuilder().nestedClass("BuilderImpl"));
        }

        builderClassBuilder.addFields(fields());
        builderClassBuilder.addMethod(noargConstructor());
        builderClassBuilder.addMethod(modelCopyConstructor());
        builderClassBuilder.addMethods(accessors());
        builderClassBuilder.addMethod(buildMethod());

        return builderClassBuilder.build();
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
        List<FieldSpec> fields = shapeModel.getNonStreamingMembers().stream()
                .map(m -> {
                    FieldSpec fieldSpec = typeProvider.asField(m, Modifier.PRIVATE);
                    if (m.isList() && typeProvider.useAutoConstructLists()) {
                        fieldSpec = fieldSpec.toBuilder()
                                .initializer("$T.getInstance()", DefaultSdkAutoConstructList.class)
                                .build();
                    } else if (m.isMap() && typeProvider.useAutoConstructMaps()) {
                        fieldSpec = fieldSpec.toBuilder()
                                .initializer("$T.getInstance()", DefaultSdkAutoConstructMap.class)
                                .build();
                    }
                    return fieldSpec;
                }).collect(Collectors.toList());

        return fields;
    }

    private MethodSpec noargConstructor() {
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);
        return ctorBuilder.build();
    }

    private MethodSpec modelCopyConstructor() {
        MethodSpec.Builder copyBuilderCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
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
        shapeModel.getNonStreamingMembers().stream()
                  .forEach(m -> {
                      accessors.add(accessorsFactory.beanStyleGetter(m));
                      accessors.addAll(accessorsFactory.fluentSetters(m, builderInterfaceName()));
                      accessors.add(accessorsFactory.beanStyleSetter(m));
                      accessors.addAll(accessorsFactory.convenienceSetters(m, builderInterfaceName()));
                  });

        if (isException()) {
            accessors.addAll(ExceptionProperties.builderImplMethods(builderImplName()));
        }

        if (isRequest()) {
            accessors.add(MethodSpec.methodBuilder("overrideConfiguration")
                    .addAnnotation(Override.class)
                    .returns(builderInterfaceName())
                    .addParameter(AwsRequestOverrideConfiguration.class, "overrideConfiguration")
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("super.overrideConfiguration(overrideConfiguration)")
                    .addStatement("return this")
                    .build());

            accessors.add(MethodSpec.methodBuilder("overrideConfiguration")
                    .addAnnotation(Override.class)
                    .returns(builderInterfaceName())
                    .addParameter(ParameterizedTypeName.get(Consumer.class, AwsRequestOverrideConfiguration.Builder.class),
                            "builderConsumer")
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("super.overrideConfiguration(builderConsumer)")
                    .addStatement("return this")
                    .build());
        }

        return accessors;
    }

    private MethodSpec buildMethod() {
        return MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(classToBuild())
                .addStatement("return new $T(this)", classToBuild())
                .build();
    }

    private ClassName classToBuild() {
        return poetExtensions.getModelClass(shapeModel.getShapeName());
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

    private List<TypeName> builderSuperInterfaces() {
        List<TypeName> superInterfaces = new ArrayList<>();
        if (isRequest()) {
            superInterfaces.add(new AwsServiceBaseRequestSpec(intermediateModel).className().nestedClass("Builder"));
        }
        if (isResponse()) {
            superInterfaces.add(new AwsServiceBaseResponseSpec(intermediateModel).className().nestedClass("Builder"));
        }
        superInterfaces.add(ParameterizedTypeName.get(ClassName.get(CopyableBuilder.class),
                classToBuild().nestedClass("Builder"), classToBuild()));
        return superInterfaces;
    }
}
