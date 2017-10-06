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

package software.amazon.awssdk.codegen.poet.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.client.builder.ClientBuilder;


public class BaseClientBuilderInterface implements ClassSpec {
    private final IntermediateModel model;
    private final String basePackage;
    private final ClassName builderInterfaceName;

    public BaseClientBuilderInterface(IntermediateModel model) {
        this.model = model;
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getBaseBuilderInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createInterfaceBuilder(builderInterfaceName)
                        .addTypeVariable(PoetUtils.createBoundedTypeVariableName("B", builderInterfaceName, "B", "C"))
                        .addTypeVariable(TypeVariableName.get("C"))
                        .addSuperinterface(PoetUtils.createParameterizedTypeName(ClientBuilder.class, "B", "C"))
                        .addJavadoc(getJavadoc());

        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            builder.addMethod(advancedConfigurationMethod());
        }

        return builder.build();
    }

    private CodeBlock getJavadoc() {
        return CodeBlock.of("This includes configuration specific to $L that is supported by both {@link $T} and {@link $T}.",
                            model.getMetadata().getServiceAbbreviation(),
                            ClassName.get(basePackage, model.getMetadata().getSyncBuilderInterface()),
                            ClassName.get(basePackage, model.getMetadata().getAsyncBuilderInterface()));
    }

    private MethodSpec advancedConfigurationMethod() {
        ClassName advancedConfiguration = ClassName.get(basePackage,
                model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        return MethodSpec.methodBuilder("advancedConfiguration")
                         .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                         .returns(TypeVariableName.get("B"))
                         .addParameter(advancedConfiguration, "advancedConfiguration")
                         .build();
    }

    @Override
    public ClassName className() {
        return builderInterfaceName;
    }
}
