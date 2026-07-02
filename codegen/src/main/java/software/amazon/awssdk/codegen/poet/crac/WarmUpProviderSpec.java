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
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;

/**
 * Generates an {@link SdkWarmUpProvider} implementation per service.
 *
 * <p>For a service whose automatically selected warm-up operation is safe to invoke with an empty request (see
 * {@link WarmUpOperationSelector}), the {@code warmUp()} body makes one synthetic, network-free call through an
 * in-memory {@link software.amazon.awssdk.core.internal.crac.CannedResponseHttpClient} so that the marshal/sign/unmarshal
 * pipeline is JIT-compiled before a CRaC checkpoint. The synthetic client is wired with dummy credentials, a fixed region
 * and a local {@code endpointOverride}, and is closed via try-with-resources.
 *
 * <p>When the selector finds no operation (e.g. warm-up disabled via {@code warmUpOperation: "NONE"}, or the service
 * models no operations) or the selected operation is not safe to warm up in this version (URI-path / host-prefix /
 * streaming hazard, or an async-only service), the body is intentionally a no-op carrying a short explanatory comment.
 * Warming those services is a clean future addition behind the same selector.
 *
 * <p>The class is {@code public final}, carries {@link SdkInternalApi} and {@code @Generated}, and provides a public
 * no-arg constructor (the implicit default) as required by {@code ServiceLoader}.
 */
public class WarmUpProviderSpec implements ClassSpec {

    private static final String CANNED_RESPONSE_FIELD = "CANNED_RESPONSE";

    // Runtime types referenced by the generated synthetic call. Declared as ClassName constants so the emitted source
    // never embeds fully-qualified name string literals: every type reference below flows through a $T placeholder.
    private static final ClassName CANNED_RESPONSE_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.core.internal.crac", "CannedResponseHttpClient");
    private static final ClassName SDK_HTTP_CLIENT =
        ClassName.get("software.amazon.awssdk.http", "SdkHttpClient");
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
        OperationModel warmUpOperation = resolveWarmUpOperation();

        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addAnnotation(SdkInternalApi.class)
                                            .addSuperinterface(SdkWarmUpProvider.class);

        if (warmUpOperation != null) {
            builder.addField(cannedResponseField());
        }

        return builder.addMethod(warmUpMethod(warmUpOperation))
                      .build();
    }

    /**
     * @return the operation whose synthetic call this provider should generate, or {@code null} when the service
     *         should emit a no-op provider (warm-up disabled, no operations, an unsafe operation, or async-only).
     */
    private OperationModel resolveWarmUpOperation() {
        return WarmUpOperationSelector.selectWarmUpOperation(model)
                                      .filter(WarmUpOperationSelector::isWarmUpSafe)
                                      .filter(op -> !isAsyncOnly())
                                      .orElse(null);
    }

    private MethodSpec warmUpMethod(OperationModel warmUpOperation) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("warmUp")
                                               .addAnnotation(Override.class)
                                               .addModifiers(Modifier.PUBLIC);

        if (warmUpOperation == null) {
            return builder.addComment("No safe zero-argument warm-up operation for this service.")
                          .build();
        }

        return builder.addCode(syntheticCallBody(warmUpOperation))
                      .build();
    }

    /**
     * Emits the synthetic priming call:
     *
     * <pre>
     * SdkHttpClient httpClient = CannedResponseHttpClient.builder()
     *     .responseBody(CANNED_RESPONSE).statusCode(200).build();
     * try (FooClient client = FooClient.builder()
     *         .httpClient(httpClient)
     *         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
     *         .region(Region.US_EAST_1)
     *         .endpointOverride(URI.create("http://localhost"))
     *         .build()) {
     *     client.listBuckets();  // no-arg overload for a simple method, else fooOperation(FooOperationRequest.builder().build())
     * }
     * </pre>
     *
     * <p>No try/catch: {@code SdkWarmUp}/{@code WarmUpDiscovery} contains warm-up failures per provider.
     */
    private CodeBlock syntheticCallBody(OperationModel warmUpOperation) {
        // Use the public client interface (e.g. QueryClient), not the internal Default* implementation, so the
        // synthetic call goes through the supported builder, matching CannedResponseSyntheticCallTest.
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
                        .addStatement(invokeOperation(warmUpOperation))
                        .endControlFlow()
                        .build();
    }

    /**
     * A simple method has a hand-vetted no-arg overload (e.g. {@code client.listBuckets()}), so call that directly.
     * Every other operation is invoked with an empty request ({@code client.op(OpRequest.builder().build())}).
     */
    private CodeBlock invokeOperation(OperationModel warmUpOperation) {
        String methodName = warmUpOperation.getMethodName();
        if (warmUpOperation.getInputShape().isSimpleMethod()) {
            return CodeBlock.of("client.$N()", methodName);
        }
        // getInput() is always populated (AddEmptyInputShape synthesizes one for input-less operations) and its
        // variableType is the generated request class name, so this resolves to the <Operation>Request type.
        ClassName requestType = poetExtensions.getModelClass(warmUpOperation.getInput().getVariableType());
        return CodeBlock.of("client.$N($T.builder().build())", methodName, requestType);
    }

    private FieldSpec cannedResponseField() {
        return FieldSpec.builder(byte[].class, CANNED_RESPONSE_FIELD,
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(cannedResponseInitializer(model.getMetadata().getProtocol()))
                        .build();
    }

    /**
     * A minimal, valid 200 response body per protocol so that the success-path unmarshaller is JIT-compiled. An empty
     * body also unmarshals cleanly for every protocol (the XML root is null-guarded), so these are chosen for clarity.
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
                // Empty CBOR map (major type 5, length 0).
                return CodeBlock.of("new byte[] {(byte) 0xA0}");
            default:
                throw new IllegalArgumentException("Unsupported protocol for CRaC warm-up canned response: " + protocol);
        }
    }

    private CodeBlock textInitializer(String body) {
        return CodeBlock.of("$S.getBytes($T.UTF_8)", body, StandardCharsets.class);
    }

    private boolean isAsyncOnly() {
        return model.getCustomizationConfig().isSkipSyncClientGeneration();
    }
}
