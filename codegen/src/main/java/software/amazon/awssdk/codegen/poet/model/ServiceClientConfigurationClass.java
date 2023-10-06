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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.utils.Validate;

public class ServiceClientConfigurationClass implements ClassSpec {
    private final ClassName defaultClientMetadataClassName;
    private final ServiceClientConfigurationUtils utils;

    public ServiceClientConfigurationClass(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullClientPackageName();
        String serviceId = model.getMetadata().getServiceName();
        this.defaultClientMetadataClassName = ClassName.get(basePackage, serviceId + "ServiceClientConfiguration");
        this.utils = new ServiceClientConfigurationUtils(model);
    }

    @Override
    public ClassName className() {
        return utils.serviceClientConfigurationClassName();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(defaultClientMetadataClassName)
                                            .addModifiers(PUBLIC, FINAL)
                                            .addAnnotation(SdkPublicApi.class)
                                            .superclass(AwsServiceClientConfiguration.class)
                                            .addJavadoc("Class to expose the service client settings to the user. "
                                                        + "Implementation of {@link $T}",
                                                        AwsServiceClientConfiguration.class);

        builder.addMethod(constructor());
        for (Field field : utils.serviceClientConfigurationFields()) {
            addLocalFieldForDataIfNeeded(field, builder);
            if (field.isLocalField()) {
                builder.addMethod(getterForDataField(field));
            }
        }

        return builder.addMethod(builderMethod())
                      .addType(builderInterfaceSpec())
                      .build();
    }

    private MethodSpec constructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                                               .addModifiers(PUBLIC)
                                               .addParameter(className().nestedClass("Builder"), "builder");
        builder.addStatement("super(builder)");
        for (Field field : utils.serviceClientConfigurationFields()) {
            if (field.isLocalField()) {
                builder.addStatement("this.$L = builder.$L()", field.name(), field.name());
            }
        }
        return builder.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(PUBLIC, STATIC)
                         .addStatement("return $T.builder()",
                                       utils.serviceClientConfigurationBuilderClassName())
                         .returns(className().nestedClass("Builder"))
                         .build();
    }

    private TypeSpec builderInterfaceSpec() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                                           .addModifiers(PUBLIC)
                                           .addSuperinterface(ClassName.get(AwsServiceClientConfiguration.class).nestedClass(
                                               "Builder"))
                                           .addJavadoc("A builder for creating a {@link $T}", className());
        for (Field field : utils.serviceClientConfigurationFields()) {
            builder.addMethod(baseSetterForField(field)
                                  .addModifiers(ABSTRACT)
                                  .build());
            builder.addMethod(baseGetterForField(field)
                                  .addModifiers(ABSTRACT)
                                  .build());
        }
        builder.addMethod(MethodSpec.methodBuilder("build")
                                    .addAnnotation(Override.class)
                                    .addModifiers(PUBLIC, ABSTRACT)
                                    .returns(className())
                                    .build());
        return builder.build();
    }

    private void addLocalFieldForDataIfNeeded(Field field, TypeSpec.Builder builder) {
        if (field.isLocalField()) {
            builder.addField(field.type(), field.name(), PRIVATE, FINAL);
        }
    }

    private MethodSpec.Builder baseSetterForField(Field field) {
        MethodSpec fieldBuilderSetter = field.builderSetter();
        if (fieldBuilderSetter != null) {
            return fieldBuilderSetter.toBuilder()
                                     .returns(className().nestedClass("Builder"));
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder(field.name())
                                               .addModifiers(PUBLIC)
                                               .addParameter(field.type(), field.name())
                                               .addJavadoc("Sets the value for " + field.doc())
                                               .returns(className().nestedClass("Builder"));
        if (!field.isLocalField()) {
            builder.addAnnotation(Override.class);
        }
        return builder;
    }

    private MethodSpec getterForDataField(Field field) {
        return getterForField(field, "config", true);
    }

    private MethodSpec getterForField(Field field, String fieldName, boolean forDataGetter) {
        MethodSpec fieldBuilderGetter = field.builderGetterImpl();
        if (fieldBuilderGetter != null) {
            return fieldBuilderGetter.toBuilder()
                                     .returns(field.type())
                                     .build();
        }

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
