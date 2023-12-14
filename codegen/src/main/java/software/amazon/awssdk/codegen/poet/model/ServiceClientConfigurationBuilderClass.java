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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.model.ServiceClientConfigurationUtils.Field;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;

public class ServiceClientConfigurationBuilderClass implements ClassSpec {
    private final ServiceClientConfigurationUtils utils;

    public ServiceClientConfigurationBuilderClass(IntermediateModel model) {
        this.utils = new ServiceClientConfigurationUtils(model);
    }

    @Override
    public ClassName className() {
        return utils.serviceClientConfigurationBuilderClassName();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addSuperinterface(utils.serviceClientConfigurationClassName().nestedClass("Builder"))
                                            .addModifiers(PUBLIC)
                                            .addAnnotation(SdkInternalApi.class);

        builder.addField(SdkClientConfiguration.Builder.class, "config", PRIVATE, FINAL);

        builder.addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(PUBLIC)
                                    .addStatement("this($T.builder())", SdkClientConfiguration.class)
                                    .build());

        builder.addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(PUBLIC)
                                    .addParameter(SdkClientConfiguration.Builder.class, "config")
                                    .addStatement("this.config = config", SdkClientConfiguration.Builder.class)
                                    .build());

        for (Field field : utils.serviceClientConfigurationFields()) {
            builder.addMethod(setterForField(field));
            builder.addMethod(getterForField(field));
        }

        builder.addMethod(MethodSpec.methodBuilder("build")
                                    .addAnnotation(Override.class)
                                    .addModifiers(PUBLIC)
                                    .returns(utils.serviceClientConfigurationClassName())
                                    .addStatement("return new $T(this)", utils.serviceClientConfigurationClassName())
                                    .build());

        return builder.build();
    }

    private MethodSpec setterForField(Field field) {
        return field.configSetter()
                    .toBuilder()
                    .addAnnotation(Override.class)
                    .build();
    }

    private MethodSpec getterForField(Field field) {
        return field.configGetter()
                    .toBuilder()
                    .addAnnotation(Override.class)
                    .build();
    }
}
