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

package software.amazon.awssdk.services.s3.internal.signing;

import static java.util.stream.Collectors.toMap;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION;
import static software.amazon.awssdk.utils.CollectionUtils.mergeLists;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.awscore.internal.AwsExecutionContextBuilder;
import software.amazon.awssdk.awscore.presigner.PresignRequest;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.protocols.xml.AwsS3ProtocolFactory;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.AbortMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.CompleteMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.CreateMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedAbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import software.amazon.awssdk.services.s3.transform.AbortMultipartUploadRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.CompleteMultipartUploadRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.CreateMultipartUploadRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.GetObjectRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.PutObjectRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.UploadPartRequestMarshaller;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * The default implementation of the {@link S3Presigner} interface.
 */
@SdkInternalApi
public final class DefaultS3Presigner extends DefaultSdkPresigner implements S3Presigner {
    private static final Logger log = Logger.loggerFor(DefaultS3Presigner.class);

    private static final AwsS3V4Signer DEFAULT_SIGNER = AwsS3V4Signer.create();
    private static final String SERVICE_NAME = "s3";
    private static final String SIGNING_NAME = "s3";

    private final S3Configuration serviceConfiguration;
    private final List<ExecutionInterceptor> clientInterceptors;
    private final GetObjectRequestMarshaller getObjectRequestMarshaller;
    private final PutObjectRequestMarshaller putObjectRequestMarshaller;
    private final CreateMultipartUploadRequestMarshaller createMultipartUploadRequestMarshaller;
    private final UploadPartRequestMarshaller uploadPartRequestMarshaller;
    private final CompleteMultipartUploadRequestMarshaller completeMultipartUploadRequestMarshaller;
    private final AbortMultipartUploadRequestMarshaller abortMultipartUploadRequestMarshaller;
    private final SdkClientConfiguration clientConfiguration;

    private DefaultS3Presigner(Builder b) {
        super(b);

        S3Configuration serviceConfiguration = b.serviceConfiguration != null ? b.serviceConfiguration :
                                                S3Configuration.builder()
                                                               .profileFile(profileFile())
                                                               .profileName(profileName())
                                                               .checksumValidationEnabled(false)
                                                               .build();
        S3Configuration.Builder serviceConfigBuilder = serviceConfiguration.toBuilder();

        if (serviceConfiguration.checksumValidationEnabled()) {
            log.debug(() -> "The provided S3Configuration has ChecksumValidationEnabled set to true. Please note that "
                           + "the pre-signed request can't be executed using a web browser if checksum validation is enabled.");
        }

        if (dualstackEnabled() != null && serviceConfigBuilder.dualstackEnabled() != null) {
            throw new IllegalStateException("Dualstack has been configured in both S3Configuration and at the "
                                            + "presigner/global level. Please limit dualstack configuration to one location.");
        }

        if (dualstackEnabled() != null) {
            serviceConfigBuilder.dualstackEnabled(dualstackEnabled());
        }

        this.serviceConfiguration = serviceConfigBuilder.build();

        this.clientInterceptors = initializeInterceptors();

        this.clientConfiguration = createClientConfiguration();

        // Copied from DefaultS3Client#init
        AwsS3ProtocolFactory protocolFactory = AwsS3ProtocolFactory.builder()
                                                                   .clientConfiguration(clientConfiguration)
                                                                   .build();

        // Copied from DefaultS3Client#getObject
        this.getObjectRequestMarshaller = new GetObjectRequestMarshaller(protocolFactory);

        // Copied from DefaultS3Client#putObject
        this.putObjectRequestMarshaller = new PutObjectRequestMarshaller(protocolFactory);

        // Copied from DefaultS3Client#createMultipartUpload
        this.createMultipartUploadRequestMarshaller = new CreateMultipartUploadRequestMarshaller(protocolFactory);

        // Copied from DefaultS3Client#uploadPart
        this.uploadPartRequestMarshaller = new UploadPartRequestMarshaller(protocolFactory);

        // Copied from DefaultS3Client#completeMultipartUpload
        this.completeMultipartUploadRequestMarshaller = new CompleteMultipartUploadRequestMarshaller(protocolFactory);

        // Copied from DefaultS3Client#abortMultipartUpload
        this.abortMultipartUploadRequestMarshaller = new AbortMultipartUploadRequestMarshaller(protocolFactory);
    }

    public static S3Presigner.Builder builder() {
        return new Builder();
    }

    /**
     * Copied from {@code DefaultS3BaseClientBuilder} and {@link SdkDefaultClientBuilder}.
     */
    private List<ExecutionInterceptor> initializeInterceptors() {
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> s3Interceptors =
            interceptorFactory.getInterceptors("software/amazon/awssdk/services/s3/execution.interceptors");
        return mergeLists(interceptorFactory.getGlobalInterceptors(), s3Interceptors);
    }

    /**
     * Copied from {@link AwsDefaultClientBuilder}.
     */
    private SdkClientConfiguration createClientConfiguration() {
        if (endpointOverride() != null) {
            return SdkClientConfiguration.builder()
                                         .option(SdkClientOption.ENDPOINT, endpointOverride())
                                         .option(SdkClientOption.ENDPOINT_OVERRIDDEN, true)
                                         .build();
        } else {
            URI defaultEndpoint = new DefaultServiceEndpointBuilder(SERVICE_NAME, "https")
                .withRegion(region())
                .withProfileFile(this::profileFile)
                .withProfileName(profileName())
                .withDualstackEnabled(serviceConfiguration.dualstackEnabled())
                .withFipsEnabled(fipsEnabled())
                .getServiceEndpoint();
            return SdkClientConfiguration.builder()
                                         .option(SdkClientOption.ENDPOINT, defaultEndpoint)
                                         .build();
        }
    }

    @Override
    public PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest request) {
        return presign(PresignedGetObjectRequest.builder(),
                       request,
                       request.getObjectRequest(),
                       GetObjectRequest.class,
                       getObjectRequestMarshaller::marshall,
                       "GetObject")
            .build();
    }

    @Override
    public PresignedPutObjectRequest presignPutObject(PutObjectPresignRequest request) {
        return presign(PresignedPutObjectRequest.builder(),
                       request,
                       request.putObjectRequest(),
                       PutObjectRequest.class,
                       putObjectRequestMarshaller::marshall,
                       "PutObject")
            .build();
    }

    @Override
    public PresignedCreateMultipartUploadRequest presignCreateMultipartUpload(CreateMultipartUploadPresignRequest request) {
        return presign(PresignedCreateMultipartUploadRequest.builder(),
                       request,
                       request.createMultipartUploadRequest(),
                       CreateMultipartUploadRequest.class,
                       createMultipartUploadRequestMarshaller::marshall,
                       "CreateMultipartUpload")
            .build();
    }

    @Override
    public PresignedUploadPartRequest presignUploadPart(UploadPartPresignRequest request) {
        return presign(PresignedUploadPartRequest.builder(),
                       request,
                       request.uploadPartRequest(),
                       UploadPartRequest.class,
                       uploadPartRequestMarshaller::marshall,
                       "UploadPart")
            .build();
    }

    @Override
    public PresignedCompleteMultipartUploadRequest presignCompleteMultipartUpload(CompleteMultipartUploadPresignRequest request) {
        return presign(PresignedCompleteMultipartUploadRequest.builder(),
                       request,
                       request.completeMultipartUploadRequest(),
                       CompleteMultipartUploadRequest.class,
                       completeMultipartUploadRequestMarshaller::marshall,
                       "CompleteMultipartUpload")
            .build();
    }

    @Override
    public PresignedAbortMultipartUploadRequest presignAbortMultipartUpload(AbortMultipartUploadPresignRequest request) {
        return presign(PresignedAbortMultipartUploadRequest.builder(),
                       request,
                       request.abortMultipartUploadRequest(),
                       AbortMultipartUploadRequest.class,
                       abortMultipartUploadRequestMarshaller::marshall,
                       "AbortMultipartUpload")
            .build();
    }

    protected S3Configuration serviceConfiguration() {
        return serviceConfiguration;
    }

    /**
     * Generate a {@link PresignedRequest} from a {@link PresignedRequest} and {@link SdkRequest}.
     */
    private <T extends PresignedRequest.Builder, U> T presign(T presignedRequest,
                                                              PresignRequest presignRequest,
                                                              SdkRequest requestToPresign,
                                                              Class<U> requestToPresignType,
                                                              Function<U, SdkHttpFullRequest> requestMarshaller,
                                                              String operationName) {

        ExecutionContext execCtx = invokeInterceptorsAndCreateExecutionContext(presignRequest, requestToPresign, operationName);

        callBeforeMarshallingHooks(execCtx);
        marshalRequestAndUpdateContext(execCtx, requestToPresignType, requestMarshaller);
        callAfterMarshallingHooks(execCtx);
        addRequestLevelHeadersAndQueryParameters(execCtx);
        callModifyHttpRequestHooksAndUpdateContext(execCtx);

        SdkHttpFullRequest httpRequest = getHttpFullRequest(execCtx);
        SdkHttpFullRequest signedHttpRequest = presignRequest(execCtx, httpRequest);

        initializePresignedRequest(presignedRequest, execCtx, signedHttpRequest);

        return presignedRequest;
    }

    /**
     * Creates an execution context from the provided request information.
     */
    private ExecutionContext invokeInterceptorsAndCreateExecutionContext(PresignRequest presignRequest,
                                                                         SdkRequest sdkRequest,
                                                                         String operationName) {

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, SIGNING_NAME)
            .putAttribute(AwsExecutionAttribute.AWS_REGION, region())
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, region())
            .putAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX, false)
            .putAttribute(SdkExecutionAttribute.CLIENT_TYPE, ClientType.SYNC)
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE_NAME)
            .putAttribute(SdkExecutionAttribute.OPERATION_NAME, operationName)
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG, serviceConfiguration())
            .putAttribute(PRESIGNER_EXPIRATION, Instant.now().plus(presignRequest.signatureDuration()))
            .putAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT, clientConfiguration.option(SdkClientOption.ENDPOINT))
            .putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN,
                          clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN));

        ExecutionInterceptorChain executionInterceptorChain = new ExecutionInterceptorChain(clientInterceptors);

        InterceptorContext interceptorContext = InterceptorContext.builder()
                                                                  .request(sdkRequest)
                                                                  .build();
        interceptorContext = AwsExecutionContextBuilder.runInitialInterceptors(interceptorContext,
                                                                               executionAttributes,
                                                                               executionInterceptorChain);

        AwsCredentialsProvider credentialsProvider =
            AwsExecutionContextBuilder.resolveCredentialsProvider(sdkRequest, credentialsProvider());
        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        Validate.validState(credentials != null, "Credential providers must never return null.");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, credentials);

        return ExecutionContext.builder()
                               .interceptorChain(executionInterceptorChain)
                               .interceptorContext(interceptorContext)
                               .executionAttributes(executionAttributes)
                               .signer(AwsExecutionContextBuilder.resolveSigner(interceptorContext.request(), DEFAULT_SIGNER))
                               .build();
    }

    /**
     * Call the before-marshalling interceptor hooks.
     */
    private void callBeforeMarshallingHooks(ExecutionContext execCtx) {
        execCtx.interceptorChain().beforeMarshalling(execCtx.interceptorContext(), execCtx.executionAttributes());
    }

    /**
     * Marshal the request and update the execution context with the result.
     */
    private <T> void marshalRequestAndUpdateContext(ExecutionContext execCtx,
                                                    Class<T> requestType,
                                                    Function<T, SdkHttpFullRequest> requestMarshaller) {
        T sdkRequest = Validate.isInstanceOf(requestType, execCtx.interceptorContext().request(),
                                             "Interceptor generated unsupported type (%s) when %s was expected.",
                                             execCtx.interceptorContext().request().getClass(), requestType);

        SdkHttpFullRequest marshalledRequest = requestMarshaller.apply(sdkRequest);

        // TODO: The core SDK doesn't put the request body into the interceptor context. That should be fixed.
        Optional<RequestBody> requestBody = marshalledRequest.contentStreamProvider()
                                                             .map(ContentStreamProvider::newStream)
                                                             .map(is -> invokeSafely(() -> IoUtils.toByteArray(is)))
                                                             .map(RequestBody::fromBytes);

        execCtx.interceptorContext(execCtx.interceptorContext().copy(r -> r.httpRequest(marshalledRequest)
                                                                           .requestBody(requestBody.orElse(null))));
    }

    /**
     * Call the after-marshalling interceptor hooks.
     */
    private void callAfterMarshallingHooks(ExecutionContext execCtx) {
        execCtx.interceptorChain().afterMarshalling(execCtx.interceptorContext(), execCtx.executionAttributes());
    }

    /**
     * Update the provided HTTP request by adding any HTTP headers or query parameters specified as part of the
     * {@link SdkRequest}.
     */
    private void addRequestLevelHeadersAndQueryParameters(ExecutionContext execCtx) {
        SdkHttpRequest httpRequest = execCtx.interceptorContext().httpRequest();
        SdkRequest sdkRequest = execCtx.interceptorContext().request();
        SdkHttpRequest updatedHttpRequest =
            httpRequest.toBuilder()
                       .applyMutation(b -> addRequestLevelHeaders(b, sdkRequest))
                       .applyMutation(b -> addRequestLeveQueryParameters(b, sdkRequest))
                       .build();
        execCtx.interceptorContext(execCtx.interceptorContext().copy(c -> c.httpRequest(updatedHttpRequest)));
    }

    private void addRequestLevelHeaders(SdkHttpRequest.Builder builder, SdkRequest request) {
        request.overrideConfiguration().ifPresent(overrideConfig -> {
            if (!overrideConfig.headers().isEmpty()) {
                overrideConfig.headers().forEach(builder::putHeader);
            }
        });
    }

    private void addRequestLeveQueryParameters(SdkHttpRequest.Builder builder, SdkRequest request) {
        request.overrideConfiguration().ifPresent(overrideConfig -> {
            if (!overrideConfig.rawQueryParameters().isEmpty()) {
                overrideConfig.rawQueryParameters().forEach(builder::putRawQueryParameter);
            }
        });
    }

    /**
     * Call the after-marshalling interceptor hooks and return the HTTP request that should be pre-signed.
     */
    private void callModifyHttpRequestHooksAndUpdateContext(ExecutionContext execCtx) {
        execCtx.interceptorContext(execCtx.interceptorChain().modifyHttpRequestAndHttpContent(execCtx.interceptorContext(),
                                                                                              execCtx.executionAttributes()));
    }

    /**
     * Get the HTTP full request from the execution context.
     */
    private SdkHttpFullRequest getHttpFullRequest(ExecutionContext execCtx) {
        SdkHttpRequest requestFromInterceptor = execCtx.interceptorContext().httpRequest();
        Optional<RequestBody> bodyFromInterceptor = execCtx.interceptorContext().requestBody();

        return SdkHttpFullRequest.builder()
                                 .method(requestFromInterceptor.method())
                                 .protocol(requestFromInterceptor.protocol())
                                 .host(requestFromInterceptor.host())
                                 .port(requestFromInterceptor.port())
                                 .encodedPath(requestFromInterceptor.encodedPath())
                                 .rawQueryParameters(requestFromInterceptor.rawQueryParameters())
                                 .headers(requestFromInterceptor.headers())
                                 .contentStreamProvider(bodyFromInterceptor.map(RequestBody::contentStreamProvider)
                                                                           .orElse(null))
                                 .build();
    }

    /**
     * Presign the provided HTTP request.
     */
    private SdkHttpFullRequest presignRequest(ExecutionContext execCtx, SdkHttpFullRequest request) {
        Presigner presigner = Validate.isInstanceOf(Presigner.class, execCtx.signer(),
                                                    "Configured signer (%s) does not support presigning (must implement %s).",
                                                    execCtx.signer().getClass(), Presigner.class);

        return presigner.presign(request, execCtx.executionAttributes());
    }

    /**
     * Initialize the provided presigned request.
     */
    private void initializePresignedRequest(PresignedRequest.Builder presignedRequest,
                                            ExecutionContext execCtx,
                                            SdkHttpFullRequest signedHttpRequest) {
        SdkBytes signedPayload = signedHttpRequest.contentStreamProvider()
                                                  .map(p -> SdkBytes.fromInputStream(p.newStream()))
                                                  .orElse(null);

        List<String> signedHeadersQueryParam = signedHttpRequest.rawQueryParameters().get("X-Amz-SignedHeaders");
        Validate.validState(signedHeadersQueryParam != null,
                            "Only SigV4 presigners are supported at this time, but the configured "
                            + "presigner (%s) did not seem to generate a SigV4 signature.", execCtx.signer());

        Map<String, List<String>> signedHeaders =
            signedHeadersQueryParam.stream()
                                   .flatMap(h -> Stream.of(h.split(";")))
                                   .collect(toMap(h -> h, h -> signedHttpRequest.firstMatchingHeader(h)
                                                                                .map(Collections::singletonList)
                                                                                .orElseGet(ArrayList::new)));

        boolean isBrowserExecutable = signedHttpRequest.method() == SdkHttpMethod.GET &&
                                      signedPayload == null &&
                                      (signedHeaders.isEmpty() ||
                                       (signedHeaders.size() == 1 && signedHeaders.containsKey("host")));

        presignedRequest.expiration(execCtx.executionAttributes().getAttribute(PRESIGNER_EXPIRATION))
                        .isBrowserExecutable(isBrowserExecutable)
                        .httpRequest(signedHttpRequest)
                        .signedHeaders(signedHeaders)
                        .signedPayload(signedPayload);
    }

    @SdkInternalApi
    public static final class Builder extends DefaultSdkPresigner.Builder<Builder>
        implements S3Presigner.Builder {

        private S3Configuration serviceConfiguration;

        private Builder() {
        }

        /**
         * Allows providing a custom S3 serviceConfiguration by providing a {@link S3Configuration} object;
         *
         * Note: chunkedEncodingEnabled and checksumValidationEnabled do not apply to presigned requests.
         *
         * @param serviceConfiguration {@link S3Configuration}
         * @return this Builder
         */
        @Override
        public Builder serviceConfiguration(S3Configuration serviceConfiguration) {
            this.serviceConfiguration = serviceConfiguration;
            return this;
        }

        @Override
        public S3Presigner build() {
            return new DefaultS3Presigner(this);
        }
    }
}
