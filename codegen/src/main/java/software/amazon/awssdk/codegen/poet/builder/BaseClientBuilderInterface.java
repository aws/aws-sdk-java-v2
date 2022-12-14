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
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

public class BaseClientBuilderInterface implements ClassSpec {
    private final IntermediateModel model;
    private final String basePackage;
    private final ClassName builderInterfaceName;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public BaseClientBuilderInterface(IntermediateModel model) {
        this.model = model;
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getBaseBuilderInterface());
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
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

        if (model.getCustomizationConfig().getServiceConfig().getClassName() != null) {
            builder.addMethod(serviceConfigurationMethod());
            builder.addMethod(serviceConfigurationConsumerBuilderMethod());
        }

        builder.addMethod(endpointProviderMethod());

        if (hasClientContextParams()) {
            model.getClientContextParams().forEach((n, m) -> {
                builder.addMethod(clientContextParamSetter(n, m));
            });
        }

        if (generateTokenProviderMethod()) {
            builder.addMethod(tokenProviderMethod());
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
                                                        model.getCustomizationConfig().getServiceConfig().getClassName());
        return MethodSpec.methodBuilder("serviceConfiguration")
                         .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                         .returns(TypeVariableName.get("B"))
                         .addParameter(serviceConfiguration, "serviceConfiguration")
                         .build();
    }

    private MethodSpec serviceConfigurationConsumerBuilderMethod() {
        ClassName serviceConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceConfig().getClassName());
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

    private MethodSpec endpointProviderMethod() {
        return MethodSpec.methodBuilder("endpointProvider")
                         .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                         .addParameter(endpointRulesSpecUtils.providerInterfaceName(), "endpointProvider")
                         .addJavadoc("Set the {@link $T} implementation that will be used by the client to determine "
                                     + "the endpoint for each request. This is optional; if none is provided a "
                                     + "default implementation will be used the SDK.",
                                     endpointRulesSpecUtils.providerInterfaceName())
                         .returns(TypeVariableName.get("B"))
                         .addStatement("throw new $T()", UnsupportedOperationException.class)
                         .build();
    }

    private MethodSpec clientContextParamSetter(String name, ClientContextParam param) {
        String setterName = Utils.unCapitalize(CodegenNamingUtils.pascalCase(name));
        TypeName type = endpointRulesSpecUtils.toJavaType(param.getType());

        MethodSpec.Builder b = MethodSpec.methodBuilder(setterName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(type, setterName)
            .returns(TypeVariableName.get("B"));

        PoetUtils.addJavadoc(b::addJavadoc, param.getDocumentation());

        return b.build();
    }

    private boolean generateTokenProviderMethod() {
        return AuthUtils.usesBearerAuth(model);
    }

    private MethodSpec tokenProviderMethod() {
        return MethodSpec.methodBuilder("tokenProvider")
                         .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                         .returns(TypeVariableName.get("B"))
                         .addParameter(SdkTokenProvider.class, "tokenProvider")
                         .addJavadoc("Set the token provider to use for bearer token authorization. This is optional, if none "
                                     + "is provided, the SDK will use {@link $T}.\n"
                                     + "<p>\n"
                                     + "If the service, or any of its operations require Bearer Token Authorization, then the "
                                     + "SDK will default to this token provider to retrieve the token to use for authorization.\n"
                                     + "<p>\n"
                                     + "This provider works in conjunction with the {@code $T.TOKEN_SIGNER} set on the client. "
                                     + "By default it is {@link $T}.",
                                     DefaultAwsTokenProvider.class,
                                     SdkAdvancedClientOption.class,
                                     BearerTokenSigner.class)
                         .build();
    }

    @Override
    public ClassName className() {
        return builderInterfaceName;
    }

    private boolean hasClientContextParams() {
        return model.getClientContextParams() != null && !model.getClientContextParams().isEmpty();
    }
}
