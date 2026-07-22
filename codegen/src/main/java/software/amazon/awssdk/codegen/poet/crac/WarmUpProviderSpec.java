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
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;

/**
 * Generates an {@link SdkWarmUpProvider} implementation per service.
 *
 * <p>The {@code warmUpClient(ClientType)} body instantiates and closes a synthetic sync client (when the service
 * generates one) and a synthetic async client, each guarded by the requested {@link ClientType} and wired to an
 * in-memory canned HTTP client, dummy credentials, a fixed region and a local {@code endpointOverride}. Building the
 * clients JIT-compiles the client construction and configuration-resolution path before a CRaC checkpoint. Each client
 * also invokes the operation chosen by {@link WarmUpOperationSelector}, if any, to prime the marshalling, signing and
 * unmarshalling paths. The {@code syncClientClassName()}/{@code asyncClientClassName()} strings let
 * {@code SdkWarmUp.prime(Class...)} match a requested client class to this provider.
 */
public class WarmUpProviderSpec implements ClassSpec {

    private static final String CANNED_RESPONSE_FIELD = "CANNED_RESPONSE";

    // Values emitted into the warm-up call. Dummy credentials and a local endpoint keep the call offline; a 200 status
    // exercises the success path.
    private static final int SUCCESS_STATUS_CODE = 200;
    private static final String DUMMY_ACCESS_KEY_ID = "akid";
    private static final String DUMMY_SECRET_ACCESS_KEY = "skid";
    private static final String DUMMY_TOKEN = "warmup-dummy-token";
    private static final String LOCAL_ENDPOINT = "http://localhost";

    private static final String DUMMY_MEMBER_VALUE = "warmup";

    private static final String DUMMY_ARN_VALUE = "arn:aws:service:us-east-1:123456789012:warmup";

    private static final ClassName CANNED_RESPONSE_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.core.crac.http", "CannedResponseHttpClient");
    private static final ClassName CANNED_RESPONSE_ASYNC_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.core.crac.http", "CannedResponseAsyncHttpClient");
    private static final ClassName SDK_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.http", "SdkHttpClient");
    private static final ClassName SDK_ASYNC_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.http.async", "SdkAsyncHttpClient");
    private static final ClassName STATIC_CREDENTIALS_PROVIDER =
        ClassName.get("software.amazon.awssdk.auth.credentials", "StaticCredentialsProvider");
    private static final ClassName AWS_BASIC_CREDENTIALS =
        ClassName.get("software.amazon.awssdk.auth.credentials", "AwsBasicCredentials");
    private static final ClassName STATIC_TOKEN_PROVIDER =
        ClassName.get("software.amazon.awssdk.auth.token.credentials", "StaticTokenProvider");
    private static final ClassName REGION =
        ClassName.get("software.amazon.awssdk.regions", "Region");

    private final IntermediateModel model;
    private final PoetExtension poetExtensions;
    private final Optional<OperationModel> warmUpOperation;

    public WarmUpProviderSpec(IntermediateModel model) {
        this.model = model;
        this.poetExtensions = new PoetExtension(model);
        this.warmUpOperation = WarmUpOperationSelector.selectWarmUpOperation(model);
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
                        .addMethod(syncClientClassNameMethod())
                        .addMethod(asyncClientClassNameMethod())
                        .addMethod(warmUpClientMethod())
                        .build();
    }

    private MethodSpec syncClientClassNameMethod() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("syncClientClassName")
                                              .addAnnotation(Override.class)
                                              .addModifiers(Modifier.PUBLIC)
                                              .returns(String.class);
        if (model.getCustomizationConfig().isSkipSyncClientGeneration()) {
            method.addStatement("return null");
        } else {
            method.addStatement("return $S", syncClientClass().toString());
        }
        return method.build();
    }

    private MethodSpec asyncClientClassNameMethod() {
        return MethodSpec.methodBuilder("asyncClientClassName")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(String.class)
                         .addStatement("return $S", asyncClientClass().toString())
                         .build();
    }

    private MethodSpec warmUpClientMethod() {
        CodeBlock.Builder body = CodeBlock.builder();
        if (!model.getCustomizationConfig().isSkipSyncClientGeneration()) {
            body.beginControlFlow("if (clientType == $T.SYNC)", ClientType.class)
                .add(clientBlock(syncClientClass(),
                                 CANNED_RESPONSE_HTTP_CLIENT, SDK_HTTP_CLIENT, "httpClient", "client", false))
                .endControlFlow();
        }
        body.beginControlFlow("if (clientType == $T.ASYNC)", ClientType.class)
            .add(clientBlock(asyncClientClass(),
                             CANNED_RESPONSE_ASYNC_HTTP_CLIENT, SDK_ASYNC_HTTP_CLIENT, "asyncHttpClient", "asyncClient", true))
            .endControlFlow();

        return MethodSpec.methodBuilder("warmUpClient")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ClientType.class, "clientType")
                         .addCode(body.build())
                         .build();
    }

    private ClassName syncClientClass() {
        return poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
    }

    private ClassName asyncClientClass() {
        return poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());
    }

    /**
     * Emits a canned HTTP client plus a try-with-resources that builds {@code clientType}, calls the selected
     * warm-up operation (if any) and closes the client. The sync and async paths differ only in these types, variable
     * names and joining the async call, so they share this emitter.
     */
    private CodeBlock clientBlock(ClassName clientType, ClassName cannedHttpClientType, ClassName httpClientType,
                                  String httpClientVar, String clientVar, boolean async) {
        CodeBlock.Builder block = CodeBlock.builder()
            .addStatement("$T $N = $T.builder().responseBody($L).statusCode($L).build()",
                          httpClientType, httpClientVar, cannedHttpClientType, CANNED_RESPONSE_FIELD,
                          SUCCESS_STATUS_CODE)
            .beginControlFlow("try ($L)", clientBuilder(clientType, clientVar, httpClientVar));

        warmUpOperation.ifPresent(op -> block.add(warmUpOperationCall(op, clientVar, async)));

        return block.endControlFlow()
                    .build();
    }

    /**
     * Builds the warm-up client: canned HTTP client, dummy credentials, local endpoint, plus any service-specific
     * options from {@link #addServiceSpecificOptions}.
     */
    private CodeBlock clientBuilder(ClassName clientType, String clientVar, String httpClientVar) {
        CodeBlock.Builder builder = CodeBlock.builder()
            .add("$1T $2N = $1T.builder()\n", clientType, clientVar)
            .add(".httpClient($N)\n", httpClientVar)
            .add(".credentialsProvider($T.create($T.create($S, $S)))\n",
                 STATIC_CREDENTIALS_PROVIDER, AWS_BASIC_CREDENTIALS, DUMMY_ACCESS_KEY_ID, DUMMY_SECRET_ACCESS_KEY);
        addServiceSpecificOptions(builder);
        return builder.add(".region($T.US_EAST_1)\n", REGION)
                      .add(".endpointOverride($T.create($S))\n", URI.class, LOCAL_ENDPOINT)
                      .add(".build()")
                      .build();
    }

    /**
     * Options only some services need: a dummy token for bearer-auth services, and disabling endpoint discovery for
     * services that have it (avoids a WARN about the endpoint override disabling it).
     */
    private void addServiceSpecificOptions(CodeBlock.Builder builder) {
        if (AuthUtils.usesBearerAuth(model)) {
            builder.add(".tokenProvider($T.create(() -> $S))\n", STATIC_TOKEN_PROVIDER, DUMMY_TOKEN);
        }
        if (model.getEndpointOperation().isPresent()) {
            builder.add(".endpointDiscoveryEnabled(false)\n");
        }
    }

    /**
     * Verified simple methods use the generated no-arg overload. Other operations pass an empty request, except for
     * members from {@link WarmUpOperationSelector#membersRequiringDummyValue} which get a dummy value.
     */
    private CodeBlock warmUpOperationCall(OperationModel operation, String clientVar, boolean async) {
        String join = async ? ".join()" : "";
        if (operation.getInputShape().isSimpleMethod()) {
            return CodeBlock.builder()
                            .addStatement("$N.$N()" + join, clientVar, operation.getMethodName())
                            .build();
        }
        ClassName requestType = poetExtensions.getModelClass(operation.getInputShape().getShapeName());
        CodeBlock.Builder request = CodeBlock.builder().add("$T.builder()", requestType);
        for (MemberModel member : WarmUpOperationSelector.membersRequiringDummyValue(operation)) {
            String value = WarmUpOperationSelector.isArnMember(member) ? DUMMY_ARN_VALUE : DUMMY_MEMBER_VALUE;
            request.add(".$N($S)", member.getFluentSetterMethodName(), value);
        }
        request.add(".build()");
        return CodeBlock.builder()
                        .addStatement("$N.$N($L)" + join, clientVar, operation.getMethodName(), request.build())
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
                return textInitializer("{}");
            case QUERY:
            case EC2:
            case REST_XML:
                return textInitializer("<Response/>");
            case CBOR:
            case SMITHY_RPC_V2_CBOR:
                // Both are binary CBOR protocols, so a text "{}" is not a valid body. 0xA0 is the single-byte CBOR
                // encoding of an empty map, the equivalent of "{}" for JSON.
                return CodeBlock.of("new byte[] {(byte) 0xA0}");
            default:
                throw new IllegalStateException("Unsupported protocol for CRaC warm-up canned response: " + protocol
                                                + " (service: " + model.getMetadata().getServiceName() + ")");
        }
    }

    private CodeBlock textInitializer(String body) {
        return CodeBlock.of("$S.getBytes($T.UTF_8)", body, StandardCharsets.class);
    }
}
