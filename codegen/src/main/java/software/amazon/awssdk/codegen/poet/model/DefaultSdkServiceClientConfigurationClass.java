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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

public class DefaultSdkServiceClientConfigurationClass implements ClassSpec {

    private final ClassName defaultClientMetadataClassName;

    public DefaultSdkServiceClientConfigurationClass(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullInternalPackageName();
        this.defaultClientMetadataClassName = ClassName.get(basePackage, "DefaultSdkServiceClientConfiguration");
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(defaultClientMetadataClassName)
                        .addSuperinterface(SdkServiceClientConfiguration.class)
            .addMethod(constructor())
            .addMethod(clientOverrideConfigMethod())
            .addModifiers(PUBLIC)
            .addAnnotation(SdkInternalApi.class)
            .addField(clientOverrideConfigField())
            .build();
    }

    @Override
    public ClassName className() {
        return defaultClientMetadataClassName;
    }

    public MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(PUBLIC)
                         .addParameter(ClientOverrideConfiguration.class, "clientOverrideConfiguration")
                         .addStatement("this.overrideConfiguration = clientOverrideConfiguration")
                         .build();
    }

    public MethodSpec clientOverrideConfigMethod() {
        return MethodSpec.methodBuilder("overrideConfiguration")
            .addModifiers(PUBLIC)
            .returns(ClientOverrideConfiguration.class)
            .addStatement("return this.overrideConfiguration")
                         .build();
    }

    public FieldSpec clientOverrideConfigField() {
        return FieldSpec.builder(ClassName.get(ClientOverrideConfiguration.class), "overrideConfiguration")
                        .addModifiers(PRIVATE, FINAL)
                        .build();
    }
}
