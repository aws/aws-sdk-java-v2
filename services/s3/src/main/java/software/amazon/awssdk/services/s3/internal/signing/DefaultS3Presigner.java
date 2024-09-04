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
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME;
import static software.amazon.awssdk.utils.CollectionUtils.mergeLists;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.awscore.endpoint.AwsClientEndpointProvider;
import software.amazon.awssdk.awscore.internal.AwsExecutionContextBuilder;
import software.amazon.awssdk.awscore.internal.defaultsmode.DefaultsModeConfiguration;
import software.amazon.awssdk.awscore.presigner.PresignRequest;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
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
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.protocols.xml.AwsS3ProtocolFactory;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.auth.scheme.internal.S3AuthSchemeInterceptor;
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.S3RequestSetEndpointInterceptor;
import software.amazon.awssdk.services.s3.endpoints.internal.S3ResolveEndpointInterceptor;
import software.amazon.awssdk.services.s3.internal.endpoints.UseGlobalEndpointResolver;
import software.amazon.awssdk.services.s3.internal.s3express.S3ExpressAuthSchemeProvider;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.AbortMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.CompleteMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.CreateMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.DeleteObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedAbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedDeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;
import software.amazon.awssdk.services.s3.transform.AbortMultipartUploadRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.CompleteMultipartUploadRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.CreateMultipartUploadRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.DeleteObjectRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.GetObjectRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.PutObjectRequestMarshaller;
import software.amazon.awssdk.services.s3.transform.UploadPartRequestMarshaller;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * The default implementation of the {@link S3Presigner} interface.
 */
@SdkInternalApi
public final class DefaultS3Presigner extends DefaultSdkPresigner implements S3Presigner {
    private static final Logger log = Logger.loggerFor(DefaultS3Presigner.class);

    private static final String SERVICE_NAME = "s3";
    private static final String SIGNING_NAME = "s3";

    private final S3Configuration serviceConfiguration;
    private final List<ExecutionInterceptor> clientInterceptors;
    private final GetObjectRequestMarshaller getObjectRequestMarshaller;
    private final PutObjectRequestMarshaller putObjectRequestMarshaller;
    private final CreateMultipartUploadRequestMarshaller createMultipartUploadRequestMarshaller;
    private final UploadPartRequestMarshaller uploadPartRequestMarshaller;
    private final DeleteObjectRequestMarshaller deleteObjectRequestMarshaller;
    private final CompleteMultipartUploadRequestMarshaller completeMultipartUploadRequestMarshaller;
    private final AbortMultipartUploadRequestMarshaller abortMultipartUploadRequestMarshaller;
    private final SdkClientConfiguration clientConfiguration;
    private final UseGlobalEndpointResolver useGlobalEndpointResolver;
    private final Boolean disableS3ExpressSessionAuth;
    private final S3Client s3Client;

    private DefaultS3Presigner(Builder b) {
        super(b);

        S3Configuration serviceConfiguration = b.serviceConfiguration != null ? b.serviceConfiguration :
                                                S3Configuration.builder()
                                                               .profileFile(profileFileSupplier())
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
        this.disableS3ExpressSessionAuth = b.disableS3ExpressSessionAuth;
        this.s3Client = b.s3Client;

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

        // Copied from DefaultS3Client#deleteObject
        this.deleteObjectRequestMarshaller = new DeleteObjectRequestMarshaller(protocolFactory);

        // Copied from DefaultS3Client#completeMultipartUpload
        this.completeMultipartUploadRequestMarshaller = new CompleteMultipartUploadRequestMarshaller(protocolFactory);

        // Copied from DefaultS3Client#abortMultipartUpload
        this.abortMultipartUploadRequestMarshaller = new AbortMultipartUploadRequestMarshaller(protocolFactory);

        this.useGlobalEndpointResolver = createUseGlobalEndpointResolver();
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
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        additionalInterceptors.add(new S3AuthSchemeInterceptor());
        additionalInterceptors.add(new S3ResolveEndpointInterceptor());
        additionalInterceptors.add(new S3RequestSetEndpointInterceptor());
        s3Interceptors = mergeLists(s3Interceptors, additionalInterceptors);
        return mergeLists(interceptorFactory.getGlobalInterceptors(), s3Interceptors);
    }

    /**
     * Copied from {@link AwsDefaultClientBuilder}.
     */
    private SdkClientConfiguration createClientConfiguration() {
        AwsClientEndpointProvider endpointProvider =
            AwsClientEndpointProvider.builder()
                                     .clientEndpointOverride(endpointOverride())
                                     .serviceEndpointOverrideEnvironmentVariable("AWS_ENDPOINT_URL_S3")
                                     .serviceEndpointOverrideSystemProperty("aws.endpointUrlS3")
                                     .serviceProfileProperty("s3")
                                     .serviceEndpointPrefix(SERVICE_NAME)
                                     .defaultProtocol("https")
                                     .region(region())
                                     .profileFile(profileFileSupplier())
                                     .profileName(profileName())
                                     .dualstackEnabled(serviceConfiguration.dualstackEnabled())
                                     .fipsEnabled(fipsEnabled())
                                     .build();

        // Make sure the endpoint resolver can actually resolve an endpoint, so that we fail now instead of
        // when a request is made.
        endpointProvider.clientEndpoint();

        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                                             endpointProvider)
                                     .build();
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
    public PresignedDeleteObjectRequest presignDeleteObject(DeleteObjectPresignRequest request) {
        return presign(PresignedDeleteObjectRequest.builder(),
                       request,
                       request.deleteObjectRequest(),
                       DeleteObjectRequest.class,
                       deleteObjectRequestMarshaller::marshall,
                       "DeleteObject")
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

        // A fixed signingClock is used, so that the current time used by the signing logic, as well as to determine expiration
        // are the same.
        Instant signingInstant = Instant.now();
        Clock signingClock = Clock.fixed(signingInstant, ZoneOffset.UTC);
        Duration expirationDuration = presignRequest.signatureDuration();
        Instant expiration = signingInstant.plus(expirationDuration);

        ExecutionContext execCtx =
            invokeInterceptorsAndCreateExecutionContext(requestToPresign, operationName, expiration, signingClock);

        callBeforeMarshallingHooks(execCtx);
        marshalRequestAndUpdateContext(execCtx, requestToPresignType, requestMarshaller);
        callAfterMarshallingHooks(execCtx);
        addRequestLevelHeadersAndQueryParameters(execCtx);
        callModifyHttpRequestHooksAndUpdateContext(execCtx);

        SdkHttpFullRequest httpRequest = getHttpFullRequest(execCtx);

        SdkHttpFullRequest signedHttpRequest = execCtx.signer() != null
                                               ? presignRequest(execCtx, httpRequest)
                                               : sraPresignRequest(execCtx, httpRequest, signingClock, expirationDuration);

        initializePresignedRequest(presignedRequest, signedHttpRequest, expiration);

        return presignedRequest;
    }

    /**
     * Creates an execution context from the provided request information.
     */
    private ExecutionContext invokeInterceptorsAndCreateExecutionContext(SdkRequest sdkRequest,
                                                                         String operationName,
                                                                         Instant expiration,
                                                                         Clock signingClock) {

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, SIGNING_NAME)
            .putAttribute(AwsExecutionAttribute.AWS_REGION, region())
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, region())
            .putAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX, false)
            .putAttribute(SdkExecutionAttribute.CLIENT_TYPE, ClientType.SYNC)
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE_NAME)
            .putAttribute(SdkExecutionAttribute.OPERATION_NAME, operationName)
            .putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, serviceConfiguration())
            .putAttribute(PRESIGNER_EXPIRATION, expiration)
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_CLOCK, signingClock)
            .putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                          clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER))
            .putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, fipsEnabled())
            .putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, serviceConfiguration.dualstackEnabled())
            .putAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER, S3EndpointProvider.defaultProvider())
            .putAttribute(AwsExecutionAttribute.USE_GLOBAL_ENDPOINT, useGlobalEndpointResolver.resolve(region()))
            .putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER,
                          S3ExpressAuthSchemeProvider.create(S3AuthSchemeProvider.defaultProvider()))
            .putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, authSchemes())
            .putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, resolveIdentityProviders(sdkRequest));

        Boolean resolvedDisableS3ExpressSessionAuth = disableS3ExpressSessionAuth;
        if (s3Client != null) {
            executionAttributes.putAttribute(SdkInternalExecutionAttribute.SDK_CLIENT, s3Client);
        } else {
            resolvedDisableS3ExpressSessionAuth = Boolean.TRUE;
        }
        AttributeMap clientContextParams = createClientContextParams(resolvedDisableS3ExpressSessionAuth);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, clientContextParams);

        ExecutionInterceptorChain executionInterceptorChain = new ExecutionInterceptorChain(clientInterceptors);

        InterceptorContext interceptorContext = InterceptorContext.builder()
                                                                  .request(sdkRequest)
                                                                  .build();
        interceptorContext = AwsExecutionContextBuilder.runInitialInterceptors(interceptorContext,
                                                                               executionAttributes,
                                                                               executionInterceptorChain);

        Signer signer = sdkRequest.overrideConfiguration().flatMap(RequestOverrideConfiguration::signer).orElse(null);

        return ExecutionContext.builder()
                               .interceptorChain(executionInterceptorChain)
                               .interceptorContext(interceptorContext)
                               .executionAttributes(executionAttributes)
                               .signer(signer)
                               .build();
    }

    private IdentityProviders resolveIdentityProviders(SdkRequest originalRequest) {
        IdentityProvider<? extends AwsCredentialsIdentity> identityProvider =
            originalRequest.overrideConfiguration()
                           .filter(c -> c instanceof AwsRequestOverrideConfiguration)
                           .map(c -> (AwsRequestOverrideConfiguration) c)
                           .flatMap(AwsRequestOverrideConfiguration::credentialsIdentityProvider)
                           .orElse(credentialsProvider());
        return IdentityProviders.builder()
                                .putIdentityProvider(identityProvider)
                                .build();
    }


    private Map<String, AuthScheme<?>> authSchemes() {
        Map<String, AuthScheme<?>> schemes = new HashMap<>(3);
        AwsV4AuthScheme awsV4AuthScheme = AwsV4AuthScheme.create();
        schemes.put(awsV4AuthScheme.schemeId(), awsV4AuthScheme);
        AwsV4aAuthScheme awsV4aAuthScheme = AwsV4aAuthScheme.create();
        schemes.put(awsV4aAuthScheme.schemeId(), awsV4aAuthScheme);
        S3ExpressAuthScheme s3ExpressAuthScheme = S3ExpressAuthScheme.create();
        schemes.put(s3ExpressAuthScheme.schemeId(), s3ExpressAuthScheme);
        return Collections.unmodifiableMap(schemes);
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
                                 .applyMutation(r -> {
                                     requestFromInterceptor.forEachHeader(r::putHeader);
                                     requestFromInterceptor.forEachRawQueryParameter(r::putRawQueryParameter);
                                 })
                                 .contentStreamProvider(bodyFromInterceptor.map(RequestBody::contentStreamProvider).orElse(null))
                                 .build();
    }

    /**
     * Presign the provided HTTP request using old Signer
     */
    private SdkHttpFullRequest presignRequest(ExecutionContext execCtx, SdkHttpFullRequest request) {
        Presigner presigner = Validate.isInstanceOf(Presigner.class, execCtx.signer(),
                                                    "Configured signer (%s) does not support presigning (must implement %s).",
                                                    execCtx.signer().getClass(), Presigner.class);

        return presigner.presign(request, execCtx.executionAttributes());
    }

    /**
     * Presign the provided HTTP request using SRA HttpSigner
     */
    private SdkHttpFullRequest sraPresignRequest(ExecutionContext execCtx, SdkHttpFullRequest request,
                                              Clock signingClock, Duration expirationDuration) {
        SelectedAuthScheme selectedAuthScheme = execCtx.executionAttributes().getAttribute(SELECTED_AUTH_SCHEME);
        return doSraPresign(request, selectedAuthScheme, signingClock, expirationDuration);
    }

    private <T extends Identity> SdkHttpFullRequest doSraPresign(SdkHttpFullRequest request,
                                                                 SelectedAuthScheme<T> selectedAuthScheme,
                                                                 Clock signingClock, Duration expirationDuration) {
        CompletableFuture<? extends T> identityFuture = selectedAuthScheme.identity();
        T identity = CompletableFutureUtils.joinLikeSync(identityFuture);

        // presigned url puts auth info in query string, does not sign the payload, and has an expiry.
        SignRequest.Builder<T> signRequestBuilder = SignRequest
            .builder(identity)
            .putProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION, AwsV4FamilyHttpSigner.AuthLocation.QUERY_STRING)
            .putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false)
            .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, expirationDuration)
            .putProperty(HttpSigner.SIGNING_CLOCK, signingClock)
            .putProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH, false)
            .putProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, false)
            .request(request)
            .payload(request.contentStreamProvider().orElse(null));
        AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();
        authSchemeOption.forEachSignerProperty(signRequestBuilder::putProperty);

        HttpSigner<T> signer = selectedAuthScheme.signer();
        SignedRequest signedRequest = signer.sign(signRequestBuilder.build());
        return toSdkHttpFullRequest(signedRequest);
    }

    private SdkHttpFullRequest toSdkHttpFullRequest(SignedRequest signedRequest) {
        SdkHttpRequest request = signedRequest.request();

        return SdkHttpFullRequest.builder()
                                 .contentStreamProvider(signedRequest.payload().orElse(null))
                                 .protocol(request.protocol())
                                 .method(request.method())
                                 .host(request.host())
                                 .port(request.port())
                                 .encodedPath(request.encodedPath())
                                 .applyMutation(r -> request.forEachHeader(r::putHeader))
                                 .applyMutation(r -> request.forEachRawQueryParameter(r::putRawQueryParameter))
                                 .build();
    }

    /**
     * Initialize the provided presigned request.
     */
    private void initializePresignedRequest(PresignedRequest.Builder presignedRequest,
                                            SdkHttpFullRequest signedHttpRequest,
                                            Instant expiration) {
        SdkBytes signedPayload = signedHttpRequest.contentStreamProvider()
                                                  .map(p -> SdkBytes.fromInputStream(p.newStream()))
                                                  .orElse(null);

        List<String> signedHeadersQueryParam = signedHttpRequest.firstMatchingRawQueryParameters("X-Amz-SignedHeaders");
        Validate.validState(!signedHeadersQueryParam.isEmpty(),
                            "Only SigV4 signers are supported at this time, but the configured "
                            + "signer did not seem to generate a SigV4 signature.");

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

        presignedRequest.expiration(expiration)
                        .isBrowserExecutable(isBrowserExecutable)
                        .httpRequest(signedHttpRequest)
                        .signedHeaders(signedHeaders)
                        .signedPayload(signedPayload);
    }

    private AttributeMap createClientContextParams(Boolean resolvedDisableS3ExpressSessionAuth) {
        AttributeMap.Builder params = AttributeMap.builder();

        params.put(S3ClientContextParams.USE_ARN_REGION, serviceConfiguration.useArnRegionEnabled());
        params.put(S3ClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS,
                                !serviceConfiguration.multiRegionEnabled());
        params.put(S3ClientContextParams.FORCE_PATH_STYLE, serviceConfiguration.pathStyleAccessEnabled());
        params.put(S3ClientContextParams.ACCELERATE, serviceConfiguration.accelerateModeEnabled());
        params.put(S3ClientContextParams.DISABLE_S3_EXPRESS_SESSION_AUTH, resolvedDisableS3ExpressSessionAuth);
        return params.build();
    }

    private UseGlobalEndpointResolver createUseGlobalEndpointResolver() {
        String legacyOption =
            DefaultsModeConfiguration.defaultConfig(DefaultsMode.LEGACY)
                                     .get(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT);

        SdkClientConfiguration config = clientConfiguration.toBuilder()
            .option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, legacyOption)
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, profileFileSupplier())
            .option(SdkClientOption.PROFILE_NAME, profileName())
            .build();

        return new UseGlobalEndpointResolver(config);
    }

    @SdkInternalApi
    public static final class Builder extends DefaultSdkPresigner.Builder<Builder>
        implements S3Presigner.Builder {

        private S3Configuration serviceConfiguration;
        private Boolean disableS3ExpressSessionAuth;
        private S3Client s3Client;

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
        public Builder disableS3ExpressSessionAuth(Boolean disableS3ExpressSessionAuth) {
            this.disableS3ExpressSessionAuth = disableS3ExpressSessionAuth;
            return this;
        }

        @Override
        public Builder s3Client(S3Client s3Client) {
            this.s3Client = s3Client;
            return this;
        }

        @Override
        public S3Presigner build() {
            return new DefaultS3Presigner(this);
        }
    }
}
