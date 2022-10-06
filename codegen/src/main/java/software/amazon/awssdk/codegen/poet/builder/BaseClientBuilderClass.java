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

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.utils.BearerAuthUtils;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.endpointdiscovery.providers.DefaultEndpointDiscoveryProviderChain;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.protocols.query.interceptor.QueryParametersToBodyInterceptor;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

public class BaseClientBuilderClass implements ClassSpec {
    private final IntermediateModel model;
    private final ClassName builderInterfaceName;
    private final ClassName builderClassName;
    private final String basePackage;

    public BaseClientBuilderClass(IntermediateModel model) {
        this.model = model;
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.builderInterfaceName = ClassName.get(basePackage, model.getMetadata().getBaseBuilderInterface());
        this.builderClassName = ClassName.get(basePackage, model.getMetadata().getBaseBuilder());
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder =
                PoetUtils.createClassBuilder(builderClassName)
                         .addModifiers(Modifier.ABSTRACT)
                         .addAnnotation(SdkInternalApi.class)
                         .addTypeVariable(PoetUtils.createBoundedTypeVariableName("B", builderInterfaceName, "B", "C"))
                         .addTypeVariable(TypeVariableName.get("C"))
                         .superclass(PoetUtils.createParameterizedTypeName(AwsDefaultClientBuilder.class, "B", "C"))
                         .addJavadoc("Internal base class for {@link $T} and {@link $T}.",
                                     ClassName.get(basePackage, model.getMetadata().getSyncBuilder()),
                                     ClassName.get(basePackage, model.getMetadata().getAsyncBuilder()));

        // Only services that require endpoint discovery for at least one of their operations get a default value of
        // 'true'
        if (model.getEndpointOperation().isPresent()) {
            builder.addField(FieldSpec.builder(boolean.class, "endpointDiscoveryEnabled")
                                      .addModifiers(PROTECTED)
                                      .initializer(resolveDefaultEndpointDiscovery() ? "true" : "false")
                                      .build());
        }

        builder.addMethod(serviceEndpointPrefixMethod());
        builder.addMethod(serviceNameMethod());
        builder.addMethod(mergeServiceDefaultsMethod());

        mergeInternalDefaultsMethod().ifPresent(builder::addMethod);

        builder.addMethod(finalizeServiceConfigurationMethod());
        defaultAwsAuthSignerMethod().ifPresent(builder::addMethod);
        builder.addMethod(signingNameMethod());

        if (model.getCustomizationConfig().getServiceConfig().getClassName() != null) {
            builder.addMethod(setServiceConfigurationMethod())
                   .addMethod(beanStyleSetServiceConfigurationMethod());
        }

        if (BearerAuthUtils.usesBearerAuth(model)) {
            builder.addMethod(defaultBearerTokenProviderMethod());
            builder.addMethod(defaultTokenAuthSignerMethod());
        }

        addServiceHttpConfigIfNeeded(builder, model);


        return builder.build();
    }

    private boolean resolveDefaultEndpointDiscovery() {
        return model.getEndpointOperation()
                    .map(OperationModel::isEndpointCacheRequired)
                    .orElse(false);
    }

    private MethodSpec signingNameMethod() {
        return MethodSpec.methodBuilder("signingName")
                         .addAnnotation(Override.class)
                         .addModifiers(PROTECTED, FINAL)
                         .returns(String.class)
                         .addCode("return $S;", model.getMetadata().getSigningName())
                         .build();
    }

    private Optional<MethodSpec> defaultAwsAuthSignerMethod() {
        return awsAuthSignerDefinitionMethodBody().map(body -> MethodSpec.methodBuilder("defaultSigner")
                                                                         .returns(Signer.class)
                                                                         .addModifiers(PRIVATE)
                                                                         .addCode(body)
                                                                         .build());

    }

    private MethodSpec serviceEndpointPrefixMethod() {
        return MethodSpec.methodBuilder("serviceEndpointPrefix")
                         .addAnnotation(Override.class)
                         .addModifiers(PROTECTED, FINAL)
                         .returns(String.class)
                         .addCode("return $S;", model.getMetadata().getEndpointPrefix())
                         .build();
    }

    private MethodSpec serviceNameMethod() {
        return MethodSpec.methodBuilder("serviceName")
                         .addAnnotation(Override.class)
                         .addModifiers(PROTECTED, FINAL)
                         .returns(String.class)
                         .addCode("return $S;", model.getMetadata().getServiceName())
                         .build();
    }

    private MethodSpec mergeServiceDefaultsMethod() {
        boolean crc32FromCompressedDataEnabled = model.getCustomizationConfig().isCalculateCrc32FromCompressedData();

        MethodSpec.Builder builder = MethodSpec.methodBuilder("mergeServiceDefaults")
                                               .addAnnotation(Override.class)
                                               .addModifiers(PROTECTED, FINAL)
                                               .returns(SdkClientConfiguration.class)
                                               .addParameter(SdkClientConfiguration.class, "config")
                                               .addCode("return config.merge(c -> c");

        if (defaultAwsAuthSignerMethod().isPresent()) {
            builder.addCode(".option($T.SIGNER, defaultSigner())\n", SdkAdvancedClientOption.class);
        }
        builder.addCode(".option($T.CRC32_FROM_COMPRESSED_DATA_ENABLED, $L)\n",
                        SdkClientOption.class, crc32FromCompressedDataEnabled);

        String clientConfigClassName = model.getCustomizationConfig().getServiceConfig().getClassName();
        if (StringUtils.isNotBlank(clientConfigClassName)) {
            builder.addCode(".option($T.SERVICE_CONFIGURATION, $T.builder().build())\n",
                            SdkClientOption.class, ClassName.bestGuess(clientConfigClassName));
        }

        if (BearerAuthUtils.usesBearerAuth(model)) {
            builder.addCode(".option($T.TOKEN_PROVIDER, defaultTokenProvider())\n", AwsClientOption.class);
            builder.addCode(".option($T.TOKEN_SIGNER, defaultTokenSigner())", SdkAdvancedClientOption.class);
        }

        builder.addCode(");");
        return builder.build();
    }

    private Optional<MethodSpec> mergeInternalDefaultsMethod() {
        String userAgent = model.getCustomizationConfig().getUserAgent();
        RetryMode defaultRetryMode = model.getCustomizationConfig().getDefaultRetryMode();
        
        // If none of the options are customized, then we do not need to bother overriding the method
        if (userAgent == null && defaultRetryMode == null) {
            return Optional.empty();
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder("mergeInternalDefaults")
                                               .addAnnotation(Override.class)
                                               .addModifiers(PROTECTED, FINAL)
                                               .returns(SdkClientConfiguration.class)
                                               .addParameter(SdkClientConfiguration.class, "config")
                                               .addCode("return config.merge(c -> {\n");
        if (userAgent != null) {
            builder.addCode("c.option($T.INTERNAL_USER_AGENT, $S);\n",
                            SdkClientOption.class, userAgent);
        }
        if (defaultRetryMode != null) {
            builder.addCode("c.option($T.DEFAULT_RETRY_MODE, $T.$L);\n",
                            SdkClientOption.class, RetryMode.class, defaultRetryMode.name());
        }
        builder.addCode("});\n");
        return Optional.of(builder.build());
    }

    private MethodSpec finalizeServiceConfigurationMethod() {
        String requestHandlerDirectory = Utils.packageToDirectory(model.getMetadata().getFullClientPackageName());
        String requestHandlerPath = String.format("%s/execution.interceptors", requestHandlerDirectory);

        MethodSpec.Builder builder = MethodSpec.methodBuilder("finalizeServiceConfiguration")
                         .addAnnotation(Override.class)
                         .addModifiers(PROTECTED, FINAL)
                         .returns(SdkClientConfiguration.class)
                         .addParameter(SdkClientConfiguration.class, "config");

        // Initialize configuration values

        builder.addCode("$1T interceptorFactory = new $1T();\n", ClasspathInterceptorChainFactory.class)
               .addCode("$T<$T> interceptors = interceptorFactory.getInterceptors($S);\n",
                        List.class, ExecutionInterceptor.class, requestHandlerPath)
               .addCode("interceptors = $T.mergeLists(interceptors, config.option($T.EXECUTION_INTERCEPTORS));\n",
                        CollectionUtils.class, SdkClientOption.class);

        if (model.getMetadata().isQueryProtocol()) {
            TypeName listType = ParameterizedTypeName.get(List.class, ExecutionInterceptor.class);
            builder.addStatement("$T protocolInterceptors = $T.singletonList(new $T())",
                                 listType,
                                 Collections.class,
                                 QueryParametersToBodyInterceptor.class);
            builder.addStatement("interceptors = $T.mergeLists(interceptors, protocolInterceptors)",
                                 CollectionUtils.class);
        }

        if (model.getEndpointOperation().isPresent()) {
            builder.beginControlFlow("if (!endpointDiscoveryEnabled)")
                   .addStatement("$1T chain = new $1T(config)", DefaultEndpointDiscoveryProviderChain.class)
                   .addStatement("endpointDiscoveryEnabled = chain.resolveEndpointDiscovery()")
                   .endControlFlow();
        }

        String clientConfigClassName = model.getCustomizationConfig().getServiceConfig().getClassName();
        if (StringUtils.isNotBlank(clientConfigClassName)) {
            ClassName clientConfigClass = ClassName.bestGuess(clientConfigClassName);
            builder.addCode("$1T.Builder c = (($1T) config.option($2T.SERVICE_CONFIGURATION)).toBuilder();" +
                            "c.profileFile(c.profileFile() != null ? c.profileFile() : config.option($2T.PROFILE_FILE));" +
                            "c.profileName(c.profileName() != null ? c.profileName() : config.option($2T.PROFILE_NAME));",
                            clientConfigClass, SdkClientOption.class);

            if (model.getCustomizationConfig().getServiceConfig().hasDualstackProperty()) {
                builder.addCode("if (c.dualstackEnabled() != null) {")
                       .addCode("    $T.validState(config.option($T.DUALSTACK_ENDPOINT_ENABLED) == null, \"Dualstack has been "
                                + "configured on both $L and the client/global level. Please limit dualstack configuration to "
                                + "one location.\");",
                                Validate.class, AwsClientOption.class, clientConfigClassName)
                       .addCode("} else {")
                       .addCode("    c.dualstackEnabled(config.option($T.DUALSTACK_ENDPOINT_ENABLED));", AwsClientOption.class)
                       .addCode("}");
            }

            if (model.getCustomizationConfig().getServiceConfig().hasFipsProperty()) {
                builder.addCode("if (c.fipsModeEnabled() != null) {")
                       .addCode("    $T.validState(config.option($T.FIPS_ENDPOINT_ENABLED) == null, \"Fips has been "
                                + "configured on both $L and the client/global level. Please limit fips configuration to "
                                + "one location.\");",
                                Validate.class, AwsClientOption.class, clientConfigClassName)
                       .addCode("} else {")
                       .addCode("    c.fipsModeEnabled(config.option($T.FIPS_ENDPOINT_ENABLED));", AwsClientOption.class)
                       .addCode("}");
            }
        }

        // Update configuration

        builder.addCode("return config.toBuilder()\n");

        if (model.getCustomizationConfig().getServiceConfig().hasDualstackProperty()) {
            builder.addCode(".option($T.DUALSTACK_ENDPOINT_ENABLED, c.dualstackEnabled())", AwsClientOption.class);
        }

        if (model.getCustomizationConfig().getServiceConfig().hasFipsProperty()) {
            builder.addCode(".option($T.FIPS_ENDPOINT_ENABLED, c.fipsModeEnabled())", AwsClientOption.class);
        }

        if (model.getEndpointOperation().isPresent()) {
            builder.addCode(".option($T.ENDPOINT_DISCOVERY_ENABLED, endpointDiscoveryEnabled)\n",
                            SdkClientOption.class);
        }

        builder.addCode(".option($1T.EXECUTION_INTERCEPTORS, interceptors)", SdkClientOption.class);

        if (StringUtils.isNotBlank(model.getCustomizationConfig().getCustomRetryPolicy())) {
            builder.addCode(".option($1T.RETRY_POLICY, $2T.resolveRetryPolicy(config))",
                            SdkClientOption.class,
                            PoetUtils.classNameFromFqcn(model.getCustomizationConfig().getCustomRetryPolicy()));
        }

        if (StringUtils.isNotBlank(clientConfigClassName)) {
            builder.addCode(".option($T.SERVICE_CONFIGURATION, c.build())", SdkClientOption.class);
        }

        builder.addCode(".build();");

        return builder.build();
    }

    private MethodSpec setServiceConfigurationMethod() {
        ClassName serviceConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceConfig().getClassName());
        return MethodSpec.methodBuilder("serviceConfiguration")
                         .addModifiers(Modifier.PUBLIC)
                         .returns(TypeVariableName.get("B"))
                         .addParameter(serviceConfiguration, "serviceConfiguration")
                         .addStatement("clientConfiguration.option($T.SERVICE_CONFIGURATION, serviceConfiguration)",
                                       SdkClientOption.class)
                         .addStatement("return thisBuilder()")
                         .build();
    }

    private MethodSpec beanStyleSetServiceConfigurationMethod() {
        ClassName serviceConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceConfig().getClassName());
        return MethodSpec.methodBuilder("setServiceConfiguration")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(serviceConfiguration, "serviceConfiguration")
                         .addStatement("serviceConfiguration(serviceConfiguration)")
                         .build();
    }

    private void addServiceHttpConfigIfNeeded(TypeSpec.Builder builder, IntermediateModel model) {
        String serviceDefaultFqcn = model.getCustomizationConfig().getServiceSpecificHttpConfig();
        boolean supportsH2 = model.getMetadata().supportsH2();

        if (serviceDefaultFqcn != null || supportsH2) {
            builder.addMethod(serviceSpecificHttpConfigMethod(serviceDefaultFqcn, supportsH2));
        }
    }

    private MethodSpec serviceSpecificHttpConfigMethod(String serviceDefaultFqcn, boolean supportsH2) {
        return MethodSpec.methodBuilder("serviceHttpConfig")
                         .addAnnotation(Override.class)
                         .addModifiers(PROTECTED, FINAL)
                         .returns(AttributeMap.class)
                         .addCode(serviceSpecificHttpConfigMethodBody(serviceDefaultFqcn, supportsH2))
                         .build();
    }

    private CodeBlock serviceSpecificHttpConfigMethodBody(String serviceDefaultFqcn, boolean supportsH2) {
        CodeBlock.Builder builder =  CodeBlock.builder();

        if (serviceDefaultFqcn != null) {
            builder.addStatement("$T result = $T.defaultHttpConfig()",
                                 AttributeMap.class,
                                 PoetUtils.classNameFromFqcn(model.getCustomizationConfig().getServiceSpecificHttpConfig()));
        } else {
            builder.addStatement("$1T result = $1T.empty()", AttributeMap.class);
        }

        if (supportsH2) {
            builder.addStatement("return result.merge(AttributeMap.builder()"
                                 + ".put($T.PROTOCOL, $T.HTTP2)"
                                 + ".build())",
                                 SdkHttpConfigurationOption.class, Protocol.class);
        } else {
            builder.addStatement("return result");
        }

        return builder.build();
    }

    private Optional<CodeBlock> awsAuthSignerDefinitionMethodBody() {
        AuthType authType = model.getMetadata().getAuthType();
        switch (authType) {
            case V4:
                return Optional.of(v4SignerDefinitionMethodBody());
            case S3:
            case S3V4:
                return Optional.of(s3SignerDefinitionMethodBody());
            case BEARER:
                return Optional.empty();
            default:
                throw new UnsupportedOperationException("Unsupported signer type: " + authType);
        }
    }

    private CodeBlock v4SignerDefinitionMethodBody() {
        return CodeBlock.of("return $T.create();", Aws4Signer.class);
    }


    private CodeBlock s3SignerDefinitionMethodBody() {
        return CodeBlock.of("return $T.create();\n",
                            ClassName.get("software.amazon.awssdk.auth.signer", "AwsS3V4Signer"));
    }

    private MethodSpec defaultBearerTokenProviderMethod() {
        return MethodSpec.methodBuilder("defaultTokenProvider")
                         .returns(SdkTokenProvider.class)
                         .addModifiers(PRIVATE)
                         .addStatement("return $T.create()", DefaultAwsTokenProvider.class)
                         .build();
    }

    private MethodSpec defaultTokenAuthSignerMethod() {
        return MethodSpec.methodBuilder("defaultTokenSigner")
                         .returns(Signer.class)
                         .addModifiers(PRIVATE)
                         .addStatement("return $T.create()", BearerTokenSigner.class)
                         .build();
    }

    @Override
    public ClassName className() {
        return builderClassName;
    }
}
