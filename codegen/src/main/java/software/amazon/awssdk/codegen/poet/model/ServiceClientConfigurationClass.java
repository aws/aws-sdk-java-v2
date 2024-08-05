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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.model.ServiceClientConfigurationUtils.Field;

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

        builder.addMethod(constructor())
               .addMethod(builderMethod());

        for (Field field : utils.serviceClientConfigurationFields()) {
            if (!field.isInherited()) {
                builder.addField(field.type(), field.name(), PRIVATE, FINAL);
                builder.addMethod(field.localGetter());
            }
        }

        return builder.addType(builderInterfaceSpec())
                      .build();
    }

    private MethodSpec constructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                                               .addModifiers(PUBLIC)
                                               .addParameter(className().nestedClass("Builder"), "builder");
        builder.addStatement("super(builder)");
        for (Field field : utils.serviceClientConfigurationFields()) {
            if (!field.isInherited()) {
                builder.addStatement("this.$L = builder.$L()", field.name(), field.name());
            }
        }
        return builder.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(PUBLIC, STATIC)
                         .addStatement("return new $T()",
                                       utils.serviceClientConfigurationBuilderClassName())
                         .returns(className().nestedClass("Builder"))
                         .build();
    }

    private TypeSpec builderInterfaceSpec() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                                           .addModifiers(PUBLIC)
                                           .addSuperinterface(ClassName.get(AwsServiceClientConfiguration.class)
                                                                       .nestedClass("Builder"))
                                           .addJavadoc("A builder for creating a {@link $T}", className());
        for (Field field : utils.serviceClientConfigurationFields()) {
            MethodSpec.Builder setterMethod = field.setterSpec().toBuilder().addModifiers(ABSTRACT);
            MethodSpec.Builder getterMethod = field.getterSpec().toBuilder().addModifiers(ABSTRACT);
            if (field.isInherited()) {
                setterMethod.addAnnotation(Override.class);
                getterMethod.addAnnotation(Override.class);
            }
            builder.addMethod(setterMethod.build());
            builder.addMethod(getterMethod.build());

        }

        builder.addMethod(MethodSpec.methodBuilder("build")
                                    .addAnnotation(Override.class)
                                    .addModifiers(PUBLIC, ABSTRACT)
                                    .returns(className())
                                    .build());
        return builder.build();
    }
}
