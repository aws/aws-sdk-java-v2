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

package software.amazon.awssdk.codegen.poet.rules;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;

public class EndpointProviderInterfaceSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public EndpointProviderInterfaceSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createInterfaceBuilder(className())
                        .addSuperinterface(EndpointProvider.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(SdkPublicApi.class)
                        .addJavadoc(interfaceJavadoc())
                        .addMethod(resolveEndpointMethod())
                        .addMethod(resolveEndpointConsumerBuilderMethod())
                        .addMethod(defaultProviderMethod())
                        .build();
    }

    private MethodSpec resolveEndpointMethod() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("resolveEndpoint");
        b.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        b.addParameter(endpointRulesSpecUtils.parametersClassName(), "endpointParams");
        b.returns(endpointRulesSpecUtils.resolverReturnType());
        b.addJavadoc(resolveMethodJavadoc());
        return b.build();
    }

    private MethodSpec resolveEndpointConsumerBuilderMethod() {
        ClassName parametersClass = endpointRulesSpecUtils.parametersClassName();
        ClassName parametersBuilderClass = parametersClass.nestedClass("Builder");
        TypeName consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), parametersBuilderClass);

        MethodSpec.Builder b = MethodSpec.methodBuilder("resolveEndpoint");
        b.addModifiers(Modifier.PUBLIC, Modifier.DEFAULT);
        b.addParameter(consumerType, "endpointParamsConsumer");
        b.returns(endpointRulesSpecUtils.resolverReturnType());
        b.addJavadoc(resolveMethodJavadoc());

        b.addStatement("$T paramsBuilder = $T.builder()", parametersBuilderClass, parametersClass);
        b.addStatement("endpointParamsConsumer.accept(paramsBuilder)");
        b.addStatement("return resolveEndpoint(paramsBuilder.build())");

        return b.build();
    }

    private MethodSpec defaultProviderMethod() {
        return MethodSpec.methodBuilder("defaultProvider")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(className())
            .addStatement("return new $T()", endpointRulesSpecUtils.providerDefaultImplName())
                         .build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.providerInterfaceName();
    }

    private CodeBlock interfaceJavadoc() {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("An endpoint provider for $N. The endpoint provider takes a set of parameters using {@link $T}, and resolves an "
              + "{@link $T} base on the given parameters.",
              intermediateModel.getMetadata().getServiceName(),
              endpointRulesSpecUtils.parametersClassName(),
              Endpoint.class);

        return b.build();
    }

    private CodeBlock resolveMethodJavadoc() {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("Compute the endpoint based on the given set of parameters.");

        return b.build();
    }
}
