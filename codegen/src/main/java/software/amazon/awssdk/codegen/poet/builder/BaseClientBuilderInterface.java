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

package software.amazon.awssdk.codegen.poet.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;


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
                        .addSuperinterface(PoetUtils.createParameterizedTypeName(AwsClientBuilder.class, "B", "C"))
                        .addJavadoc(getJavadoc());

        if (model.getEndpointOperation().isPresent()) {
            if (model.getCustomizationConfig().isEnableEndpointDiscoveryMethodRequired()) {
                builder.addMethod(enableEndpointDiscovery());
            }

            builder.addMethod(endpointDiscovery());
        }

        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            builder.addMethod(serviceConfigurationMethod());
            builder.addMethod(serviceConfigurationConsumerBuilderMethod());
        }

        return builder.build();
    }

    private CodeBlock getJavadoc() {
        return CodeBlock.of("This includes configuration specific to $L that is supported by both {@link $T} and {@link $T}.",
                            model.getMetadata().getDescriptiveServiceName(),
                            ClassName.get(basePackage, model.getMetadata().getSyncBuilderInterface()),
                            ClassName.get(basePackage, model.getMetadata().getAsyncBuilderInterface()));
    }

    private MethodSpec enableEndpointDiscovery() {
        return MethodSpec.methodBuilder("enableEndpointDiscovery")
                         .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                         .returns(TypeVariableName.get("B"))
                         .addAnnotation(Deprecated.class)
                         .addJavadoc("@deprecated Use {@link #endpointDiscoveryEnabled($T)} instead.", boolean.class)
                         .build();
    }

    private MethodSpec endpointDiscovery() {
        return MethodSpec.methodBuilder("endpointDiscoveryEnabled")
                         .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                         .returns(TypeVariableName.get("B"))
                         .addParameter(boolean.class, "endpointDiscovery")
                         .build();
    }

    private MethodSpec serviceConfigurationMethod() {
        ClassName serviceConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        return MethodSpec.methodBuilder("serviceConfiguration")
                         .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                         .returns(TypeVariableName.get("B"))
                         .addParameter(serviceConfiguration, "serviceConfiguration")
                         .build();
    }

    private MethodSpec serviceConfigurationConsumerBuilderMethod() {
        ClassName serviceConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        TypeName consumerBuilder = ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                                             serviceConfiguration.nestedClass("Builder"));
        return MethodSpec.methodBuilder("serviceConfiguration")
                         .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                         .returns(TypeVariableName.get("B"))
                         .addParameter(consumerBuilder, "serviceConfiguration")
                         .addStatement("return serviceConfiguration($T.builder().applyMutation(serviceConfiguration).build())",
                                       serviceConfiguration)
                         .build();
    }

    @Override
    public ClassName className() {
        return builderInterfaceName;
    }
}
