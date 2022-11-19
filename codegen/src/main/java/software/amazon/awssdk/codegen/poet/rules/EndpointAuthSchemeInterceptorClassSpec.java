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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.function.Supplier;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.SignerLoader;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.util.SignerOverrideUtils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.endpoints.Endpoint;

/**
 * Generates the Endpoint Interceptor responsible for applying the {@link AwsEndpointAttribute#AUTH_SCHEMES} property on the
 * endpoint if they exist. Auth schemes describe auth related requirements for the endpoint, such as signing name, signing
 * region, and the name of the auth scheme to use, such as SigV4.
 */
public class EndpointAuthSchemeInterceptorClassSpec implements ClassSpec {
    private static final String SIGV4_NAME = "sigv4";
    private static final String SIGV4A_NAME = "sigv4a";

    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public EndpointAuthSchemeInterceptorClassSpec(IntermediateModel intermediateModel) {
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                      .addAnnotation(SdkInternalApi.class)
                                      .addSuperinterface(ExecutionInterceptor.class);

        b.addMethod(modifyRequestMethod());
        b.addMethod(signerProviderMethod());

        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.authSchemesInterceptorName();
    }

    private MethodSpec modifyRequestMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("modifyRequest")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(ClassName.get(Context.ModifyRequest.class), "context")
            .addParameter(ExecutionAttributes.class, "executionAttributes")
            .returns(SdkRequest.class);

        builder.addStatement("$T resolvedEndpoint = executionAttributes.getAttribute($T.RESOLVED_ENDPOINT)", Endpoint.class,
                             SdkInternalExecutionAttribute.class);

        builder.addStatement("$1T request = ($1T) context.request()", AwsRequest.class);

        builder.beginControlFlow("if (resolvedEndpoint.headers() != null)")
               .addStatement("request = $T.addHeaders(request, resolvedEndpoint.headers())",
                             endpointRulesSpecUtils.rulesRuntimeClassName("AwsEndpointProviderUtils"));
        builder.endControlFlow();

        builder.addStatement("$T authSchemes = resolvedEndpoint.attribute($T.AUTH_SCHEMES)",
                             ParameterizedTypeName.get(List.class, EndpointAuthScheme.class), AwsEndpointAttribute.class);

        builder.beginControlFlow("if (authSchemes == null)")
               .addStatement("return request")
               .endControlFlow();

        // find the scheme to use
        builder.addStatement("$T chosenAuthScheme = $T.chooseAuthScheme(authSchemes)", EndpointAuthScheme.class,
                             endpointRulesSpecUtils.rulesRuntimeClassName("AuthSchemeUtils"));

        // Create a signer provider
        builder.addStatement("$T signerProvider = signerProvider(chosenAuthScheme)", ParameterizedTypeName.get(Supplier.class,
                                                                                                               Signer.class));

        // Set signing attributes
        builder.addStatement("$T.setSigningParams(executionAttributes, chosenAuthScheme)",
                             endpointRulesSpecUtils.rulesRuntimeClassName("AuthSchemeUtils"));

        // Override signer
        builder.addStatement("return $T.overrideSignerIfNotOverridden(request, executionAttributes, signerProvider)",
                             SignerOverrideUtils.class);


        return builder.build();
    }

    private MethodSpec signerProviderMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("signerProvider")
            .addModifiers(Modifier.PRIVATE)
            .addParameter(EndpointAuthScheme.class, "authScheme")
            .returns(ParameterizedTypeName.get(Supplier.class, Signer.class));

        builder.beginControlFlow("switch (authScheme.name())");
        builder.addCode("case $S:", SIGV4_NAME);
        if (endpointRulesSpecUtils.isS3() || endpointRulesSpecUtils.isS3Control()) {
            builder.addStatement("return $T::create", AwsS3V4Signer.class);
        } else {
            builder.addStatement("return $T::create", Aws4Signer.class);
        }

        builder.addCode("case $S:", SIGV4A_NAME);
        if (endpointRulesSpecUtils.isS3() || endpointRulesSpecUtils.isS3Control()) {
            builder.addStatement("return $T::getS3SigV4aSigner", SignerLoader.class);
        } else {
            builder.addStatement("return $T::getSigV4aSigner", SignerLoader.class);
        }

        builder.addCode("default:");
        builder.addStatement("break");
        builder.endControlFlow();

        builder.addStatement("throw $T.create($S + authScheme.name())",
                             SdkClientException.class,
                             "Don't know how to create signer for auth scheme: ");

        return builder.build();
    }
}
