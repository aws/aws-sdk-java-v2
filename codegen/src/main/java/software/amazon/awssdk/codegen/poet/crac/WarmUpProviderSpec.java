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

package software.amazon.awssdk.codegen.poet.crac;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;

/**
 * Generates an {@link SdkWarmUpProvider} implementation per service.
 *
 * <p>The {@code warmUp()} body instantiates and closes a synthetic sync client (when the service generates one) and a
 * synthetic async client, each wired to an in-memory canned HTTP client, dummy credentials, a fixed region and a local
 * {@code endpointOverride}. Building the clients JIT-compiles the client construction and configuration-resolution path
 * before a CRaC checkpoint. The operation call that exercises the marshal/unmarshal pipeline is added in a later stage.
 */
public class WarmUpProviderSpec implements ClassSpec {

    private static final String CANNED_RESPONSE_FIELD = "CANNED_RESPONSE";

    private static final ClassName CANNED_RESPONSE_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.core.internal.crac", "CannedResponseHttpClient");
    private static final ClassName CANNED_RESPONSE_ASYNC_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.core.internal.crac", "CannedResponseAsyncHttpClient");
    private static final ClassName SDK_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.http", "SdkHttpClient");
    private static final ClassName SDK_ASYNC_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.http.async", "SdkAsyncHttpClient");
    private static final ClassName STATIC_CREDENTIALS_PROVIDER =
        ClassName.get("software.amazon.awssdk.auth.credentials", "StaticCredentialsProvider");
    private static final ClassName AWS_BASIC_CREDENTIALS =
        ClassName.get("software.amazon.awssdk.auth.credentials", "AwsBasicCredentials");
    private static final ClassName REGION =
        ClassName.get("software.amazon.awssdk.regions", "Region");

    private final IntermediateModel model;
    private final PoetExtension poetExtensions;

    public WarmUpProviderSpec(IntermediateModel model) {
        this.model = model;
        this.poetExtensions = new PoetExtension(model);
    }

    @Override
    public ClassName className() {
        return ClassName.get(model.getMetadata().getFullCracInternalPackageName(),
                             model.getMetadata().getServiceName() + "WarmUpProvider");
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(SdkInternalApi.class)
                        .addSuperinterface(SdkWarmUpProvider.class)
                        .addField(cannedResponseField())
                        .addMethod(warmUpMethod())
                        .build();
    }

    private MethodSpec warmUpMethod() {
        CodeBlock.Builder body = CodeBlock.builder();
        if (!model.getCustomizationConfig().isSkipSyncClientGeneration()) {
            body.add(syncClientBlock());
        }
        body.add(asyncClientBlock());

        return MethodSpec.methodBuilder("warmUp")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addCode(body.build())
                         .build();
    }

    private CodeBlock syncClientBlock() {
        ClassName syncClient = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
        return CodeBlock.builder()
                        .addStatement("$T httpClient = $T.builder().responseBody($L).statusCode(200).build()",
                                      SDK_HTTP_CLIENT, CANNED_RESPONSE_HTTP_CLIENT, CANNED_RESPONSE_FIELD)
                        .beginControlFlow("try ($1T client = $1T.builder()\n"
                                          + ".httpClient(httpClient)\n"
                                          + ".credentialsProvider($2T.create($3T.create($4S, $5S)))\n"
                                          + ".region($6T.US_EAST_1)\n"
                                          + ".endpointOverride($7T.create($8S))\n"
                                          + ".build())",
                                          syncClient,
                                          STATIC_CREDENTIALS_PROVIDER,
                                          AWS_BASIC_CREDENTIALS,
                                          "akid", "skid",
                                          REGION,
                                          URI.class,
                                          "http://localhost")
                        .endControlFlow()
                        .build();
    }

    private CodeBlock asyncClientBlock() {
        ClassName asyncClient = poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());
        return CodeBlock.builder()
                        .addStatement("$T asyncHttpClient = $T.builder().responseBody($L).statusCode(200).build()",
                                      SDK_ASYNC_HTTP_CLIENT, CANNED_RESPONSE_ASYNC_HTTP_CLIENT, CANNED_RESPONSE_FIELD)
                        .beginControlFlow("try ($1T asyncClient = $1T.builder()\n"
                                          + ".httpClient(asyncHttpClient)\n"
                                          + ".credentialsProvider($2T.create($3T.create($4S, $5S)))\n"
                                          + ".region($6T.US_EAST_1)\n"
                                          + ".endpointOverride($7T.create($8S))\n"
                                          + ".build())",
                                          asyncClient,
                                          STATIC_CREDENTIALS_PROVIDER,
                                          AWS_BASIC_CREDENTIALS,
                                          "akid", "skid",
                                          REGION,
                                          URI.class,
                                          "http://localhost")
                        .endControlFlow()
                        .build();
    }

    private FieldSpec cannedResponseField() {
        return FieldSpec.builder(byte[].class, CANNED_RESPONSE_FIELD,
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(cannedResponseInitializer(model.getMetadata().getProtocol()))
                        .build();
    }

    /**
     * A minimal, valid 200 response body per protocol. An empty body unmarshals for every protocol.
     */
    private CodeBlock cannedResponseInitializer(Protocol protocol) {
        switch (protocol) {
            case REST_JSON:
            case AWS_JSON:
            case CBOR:
                return textInitializer("{}");
            case QUERY:
            case EC2:
            case REST_XML:
                return textInitializer("<Response/>");
            case SMITHY_RPC_V2_CBOR:
                // 0xA0 is the single-byte CBOR encoding of an empty map "{}", the equivalent of "{}" for JSON.
                return CodeBlock.of("new byte[] {(byte) 0xA0}");
            default:
                throw new IllegalArgumentException("Unsupported protocol for CRaC warm-up canned response: " + protocol);
        }
    }

    private CodeBlock textInitializer(String body) {
        return CodeBlock.of("$S.getBytes($T.UTF_8)", body, StandardCharsets.class);
    }
}
