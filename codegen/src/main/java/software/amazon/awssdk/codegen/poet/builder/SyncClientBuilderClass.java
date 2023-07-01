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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.net.URI;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.endpoints.EndpointProvider;

public class SyncClientBuilderClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName clientInterfaceName;
    private final ClassName clientClassName;
    private final ClassName builderInterfaceName;
    private final ClassName builderClassName;
    private final ClassName builderBaseClassName;
    private final ClassName serviceConfigClassName;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public SyncClientBuilderClass(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullClientPackageName();
        this.model = model;
        this.clientInterfaceName = ClassName.get(basePackage, model.getMetadata().getSyncInterface());
        this.clientClassName = ClassName.get(basePackage, model.getMetadata().getSyncClient());
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getSyncBuilderInterface());
        this.builderClassName = ClassName.get(basePackage, model.getMetadata().getSyncBuilder());
        this.builderBaseClassName = ClassName.get(basePackage, model.getMetadata().getBaseBuilder());
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
        this.serviceConfigClassName = new PoetExtension(model).getServiceConfigClass();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder =
                PoetUtils.createClassBuilder(builderClassName)
                         .addAnnotation(SdkInternalApi.class)
                         .addModifiers(Modifier.FINAL)
                         .superclass(ParameterizedTypeName.get(builderBaseClassName, builderInterfaceName, clientInterfaceName))
                         .addSuperinterface(builderInterfaceName)
                         .addJavadoc("Internal implementation of {@link $T}.", builderInterfaceName);

        if (model.getEndpointOperation().isPresent()) {
            builder.addMethod(endpointDiscoveryEnabled());

            if (model.getCustomizationConfig().isEnableEndpointDiscoveryMethodRequired()) {
                builder.addMethod(enableEndpointDiscovery());
            }
        }

        builder.addMethod(endpointProviderMethod());

        if (AuthUtils.usesBearerAuth(model)) {
            builder.addMethod(tokenProviderMethodImpl());
        }

        builder.addMethod(buildClientMethod());
        builder.addMethod(initializeServiceClientConfigMethod());

        return builder.build();
    }

    private MethodSpec endpointDiscoveryEnabled() {
        return MethodSpec.methodBuilder("endpointDiscoveryEnabled")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(builderClassName)
                         .addParameter(boolean.class, "endpointDiscoveryEnabled")
                         .addStatement("this.endpointDiscoveryEnabled = endpointDiscoveryEnabled")
                         .addStatement("return this")
                         .build();
    }

    private MethodSpec enableEndpointDiscovery() {
        return MethodSpec.methodBuilder("enableEndpointDiscovery")
                         .addAnnotation(Override.class)
                         .addAnnotation(Deprecated.class)
                         .addJavadoc("@deprecated Use {@link #endpointDiscoveryEnabled($T)} instead.", boolean.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(builderClassName)
                         .addStatement("endpointDiscoveryEnabled = true")
                         .addStatement("return this")
                         .build();
    }

    private MethodSpec endpointProviderMethod() {
        return MethodSpec.methodBuilder("endpointProvider")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(className())
                         .addParameter(endpointRulesSpecUtils.providerInterfaceName(), "endpointProvider")
                         .addStatement("clientConfiguration.option($T.ENDPOINT_PROVIDER, endpointProvider)",
                                       SdkClientOption.class)
                         .addStatement("return this")
                         .build();
    }


    private MethodSpec buildClientMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("buildClient")
                                         .addAnnotation(Override.class)
                                         .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                                         .returns(clientInterfaceName)
                                         .addStatement("$T clientConfiguration = super.syncClientConfiguration()",
                                                       SdkClientConfiguration.class)
                                         .addStatement("this.validateClientOptions(clientConfiguration)")
                                         .addStatement("$T serviceClientConfiguration = initializeServiceClientConfig"
                                                       + "(clientConfiguration)",
                                                       serviceConfigClassName);

        builder.addStatement("$1T client = new $2T(serviceClientConfiguration, clientConfiguration)",
                             clientInterfaceName, clientClassName);
        if (model.syncClientDecoratorClassName().isPresent()) {
            builder.addStatement("return new $T().decorate(client, clientConfiguration, clientContextParams.copy().build())",
                                 PoetUtils.classNameFromFqcn(model.syncClientDecoratorClassName().get()));
        } else {
            builder.addStatement("return client");
        }
        return builder.build();
    }

    private MethodSpec tokenProviderMethodImpl() {
        return MethodSpec.methodBuilder("tokenProvider").addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .addParameter(SdkTokenProvider.class, "tokenProvider")
                         .returns(builderClassName)
                         .addStatement("clientConfiguration.option($T.TOKEN_PROVIDER, tokenProvider)",
                                       AwsClientOption.class)
                         .addStatement("return this")
                         .build();
    }

    private MethodSpec initializeServiceClientConfigMethod() {
        return MethodSpec.methodBuilder("initializeServiceClientConfig").addModifiers(Modifier.PRIVATE)
                         .addParameter(SdkClientConfiguration.class, "clientConfig")
                         .returns(serviceConfigClassName)
                         .addStatement("$T endpointOverride = null", URI.class)
                         .addStatement("$T endpointProvider = clientConfig.option($T.ENDPOINT_PROVIDER)",
                                       EndpointProvider.class,
                                       SdkClientOption.class)
                         .addCode("if (clientConfig.option($T.ENDPOINT_OVERRIDDEN) != null"
                                  + "&& $T.TRUE.equals(clientConfig.option($T.ENDPOINT_OVERRIDDEN))) {"
                                  + "endpointOverride = clientConfig.option($T.ENDPOINT);"
                                  + "}",
                                  SdkClientOption.class, Boolean.class, SdkClientOption.class, SdkClientOption.class)
                         .addStatement("return $T.builder()"
                                       + ".overrideConfiguration(overrideConfiguration())"
                                       + ".region(clientConfig.option($T.AWS_REGION))"
                                       + ".endpointOverride(endpointOverride)"
                                       + ".endpointProvider(endpointProvider)"
                                       + ".build()",
                                       serviceConfigClassName, AwsClientOption.class)
                         .build();
    }

    @Override
    public ClassName className() {
        return builderClassName;
    }
}
