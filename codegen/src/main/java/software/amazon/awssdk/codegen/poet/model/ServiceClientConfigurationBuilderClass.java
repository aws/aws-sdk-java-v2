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

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.poet.model.ServiceClientConfigurationUtils.Field;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.utils.Validate;

public class ServiceClientConfigurationBuilderClass implements ClassSpec {
    private final ServiceClientConfigurationUtils utils;
    private final ClassName builderInterface;

    public ServiceClientConfigurationBuilderClass(IntermediateModel model) {
        this.utils = new ServiceClientConfigurationUtils(model);
        this.builderInterface = utils.serviceClientConfigurationClassName().nestedClass("Builder");
    }

    @Override
    public ClassName className() {
        return utils.serviceClientConfigurationBuilderClassName();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(PUBLIC)
                                            .addAnnotation(SdkInternalApi.class);

        return builder.addMethod(builderMethod())
                      .addMethod(builderFromSdkClientConfiguration())
                      .addType(builderInternalSpec())
                      .addType(builderImplSpec())
                      .build();
    }


    private MethodSpec builderFromSdkClientConfiguration() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(PUBLIC, STATIC)
                         .addParameter(SdkClientConfiguration.Builder.class, "builder")
                         .returns(className().nestedClass("BuilderInternal"))
                         .addStatement("return new BuilderImpl(builder)")
                         .build();
    }


    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(PUBLIC, STATIC)
                         .returns(builderInterface)
                         .addStatement("return new BuilderImpl()")
                         .build();
    }

    private TypeSpec builderInternalSpec() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("BuilderInternal")
                                           .addModifiers(PUBLIC)
                                           .addSuperinterface(builderInterface);
        builder.addMethod(MethodSpec.methodBuilder("buildSdkClientConfiguration")
                                    .addModifiers(PUBLIC, ABSTRACT)
                                    .returns(SdkClientConfiguration.class)
                                    .build());
        return builder.build();
    }

    private TypeSpec builderImplSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("BuilderImpl")
                                           .addModifiers(PUBLIC, STATIC)
                                           .addSuperinterface(className().nestedClass("BuilderInternal"));

        builder.addField(SdkClientConfiguration.Builder.class, "builder", PRIVATE, FINAL);

        builder.addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(PRIVATE)
                                    .addStatement("this.builder = $T.builder()", SdkClientConfiguration.class)
                                    .build());
        builder.addMethod(constructorFromSdkClientConfiguration());

        for (Field field : utils.serviceClientConfigurationFields()) {
            addLocalFieldForBuilderIfNeeded(field, builder);
            builder.addMethod(setterForField(field));
            builder.addMethod(getterForBuilderField(field));
        }

        builder.addMethod(MethodSpec.methodBuilder("build")
                                    .addAnnotation(Override.class)
                                    .addModifiers(PUBLIC)
                                    .returns(utils.serviceClientConfigurationClassName())
                                    .addStatement("return new $T(this)", utils.serviceClientConfigurationClassName())
                                    .build());

        builder.addMethod(buildSdkClientConfigurationMethod());

        return builder.build();
    }

    private MethodSpec buildSdkClientConfigurationMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("buildSdkClientConfiguration")
                                               .addModifiers(PUBLIC)
                                               .addAnnotation(Override.class)
                                               .returns(SdkClientConfiguration.class);

        for (Field field : utils.serviceClientConfigurationFields()) {
            if (field.optionClass() == null) {
                CodeBlock block = field.copyToConfiguration();
                if (block != null) {
                    builder.addCode(block);
                }
            }
        }
        return builder
            .addStatement("return builder.build()")
            .build();
    }

    private MethodSpec constructorFromSdkClientConfiguration() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                                               .addModifiers(PRIVATE)
                                               .addParameter(SdkClientConfiguration.Builder.class, "builder")
                                               .addStatement("this.builder = builder");

        for (Field field : utils.serviceClientConfigurationFields()) {
            if (field.optionClass() == null) {
                CodeBlock block = field.constructFromConfiguration();
                if (block != null) {
                    builder.addCode(block);
                }
            }
        }
        return builder.build();
    }


    private void addLocalFieldForBuilderIfNeeded(Field field, TypeSpec.Builder builder) {
        if (field.optionClass() == null) {
            builder.addField(field.type(), field.name(), PRIVATE);
        }
    }

    private MethodSpec setterForField(Field field) {
        MethodSpec.Builder builder = baseSetterForField(field);
        if (field.isLocalField()) {
            builder.addAnnotation(Override.class);
        }
        if (field.optionClass() == null) {
            return builder.addStatement("this.$1L = $1L", field.name())
                          .addStatement("return this")
                          .build();

        }
        return builder.addStatement("builder.option($T.$L, $L)",
                                    field.optionClass(), field.optionName(), field.name())
                      .addStatement("return this")
                      .build();
    }

    private MethodSpec.Builder baseSetterForField(Field field) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(field.name())
                                               .addModifiers(PUBLIC)
                                               .addParameter(field.type(), field.name())
                                               .addJavadoc("Sets the value for " + field.doc())
                                               .returns(builderInterface);
        if (!field.isLocalField()) {
            builder.addAnnotation(Override.class);
        }
        return builder;
    }

    private MethodSpec getterForBuilderField(Field field) {
        return getterForField(field, "builder", false);
    }


    private MethodSpec getterForField(Field field, String fieldName, boolean forDataGetter) {
        MethodSpec.Builder builder = baseGetterForField(field);
        if (!forDataGetter && field.isLocalField()) {
            builder.addAnnotation(Override.class);
        }
        if (forDataGetter && field.isLocalField()) {
            return builder.addStatement("return $L", field.name())
                          .build();
        }
        if (field.optionClass() == null) {
            return builder.addStatement("return $L", field.name())
                          .build();
        }
        if (field.baseType() != null) {
            return builder.addStatement("$T result = $L.option($T.$L)",
                                        field.baseType(), fieldName, field.optionClass(), field.optionName())
                          .beginControlFlow("if (result == null)")
                          .addStatement("return null")
                          .endControlFlow()
                          .addStatement("return $T.isInstanceOf($T.class, result, $S + $T.class.getSimpleName())",
                                        Validate.class, field.type(),
                                        "Expected an instance of ", field.type())
                          .build();
        }
        return builder.addStatement("return $L.option($T.$L)", fieldName, field.optionClass(), field.optionName())
                      .build();
    }

    private MethodSpec.Builder baseGetterForField(Field field) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(field.name())
                                               .addModifiers(PUBLIC)
                                               .addJavadoc("Gets the value for " + field.doc())
                                               .returns(field.type());
        if (!field.isLocalField()) {
            builder.addAnnotation(Override.class);
        }
        return builder;
    }
}
