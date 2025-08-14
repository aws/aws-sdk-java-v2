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

package software.amazon.awssdk.services.s3.internal.crt;

import static software.amazon.awssdk.services.s3.crt.S3CrtSdkHttpExecutionAttribute.CRT_PROGRESS_LISTENER;
import static software.amazon.awssdk.services.s3.crt.S3CrtSdkHttpExecutionAttribute.METAREQUEST_PAUSE_OBSERVABLE;
import static software.amazon.awssdk.services.s3.internal.crt.CrtChecksumUtils.checksumConfig;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.CRT_PAUSE_RESUME_TOKEN;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.HTTP_CHECKSUM;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.OBJECT_FILE_PATH;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.OPERATION_NAME;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.REQUEST_CHECKSUM_CALCULATION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.RESPONSE_FILE_OPTION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.RESPONSE_FILE_PATH;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.SIGNING_NAME;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.USE_S3_EXPRESS_AUTH;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpProxyEnvironmentVariableSetting;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.s3.ChecksumConfig;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.crt.s3.S3Client;
import software.amazon.awssdk.crt.s3.S3ClientOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * An implementation of {@link SdkAsyncHttpClient} that uses an CRT S3 HTTP client {@link S3Client} to communicate with S3.
 * Note that it does not work with other services
 */
@SdkInternalApi
public final class S3CrtAsyncHttpClient implements SdkAsyncHttpClient {

    private final S3Client crtS3Client;

    private final S3NativeClientConfiguration s3NativeClientConfiguration;
    private final S3ClientOptions s3ClientOptions;

    private S3CrtAsyncHttpClient(Builder builder) {
        s3NativeClientConfiguration = builder.clientConfiguration;
        this.s3ClientOptions = createS3ClientOption();

        this.crtS3Client = new S3Client(s3ClientOptions);
    }

    @SdkTestInternalApi
    S3CrtAsyncHttpClient(S3Client crtS3Client,
                         Builder builder) {
        s3NativeClientConfiguration = builder.clientConfiguration;
        s3ClientOptions = createS3ClientOption();
        this.crtS3Client = crtS3Client;
    }

    @SdkTestInternalApi
    public S3ClientOptions s3ClientOptions() {
        return s3ClientOptions;
    }

    private S3ClientOptions createS3ClientOption() {
        Long initialWindowSize = s3NativeClientConfiguration.readBufferSizeInBytes();

        S3ClientOptions options =
            new S3ClientOptions().withRegion(s3NativeClientConfiguration.signingRegion())
                                 .withEndpoint(s3NativeClientConfiguration.endpointOverride() == null ? null :
                                               s3NativeClientConfiguration.endpointOverride().toString())
                                 .withCredentialsProvider(s3NativeClientConfiguration.credentialsProvider())
                                 .withClientBootstrap(s3NativeClientConfiguration.clientBootstrap())
                                 .withTlsContext(s3NativeClientConfiguration.tlsContext())
                                 .withPartSize(s3NativeClientConfiguration.partSizeBytes())
                                 .withMultipartUploadThreshold(s3NativeClientConfiguration.thresholdInBytes())
                                 .withComputeContentMd5(false)
                                 .withEnableS3Express(true)
                                 .withMaxConnections(s3NativeClientConfiguration.maxConcurrency())
                                 .withMemoryLimitInBytes(s3NativeClientConfiguration.maxNativeMemoryLimitInBytes())
                                 .withThroughputTargetGbps(s3NativeClientConfiguration.targetThroughputInGbps())
                                 .withInitialReadWindowSize(initialWindowSize)
                                 .withReadBackpressureEnabled(true);

        if (s3NativeClientConfiguration.standardRetryOptions() != null) {
            options.withStandardRetryOptions(s3NativeClientConfiguration.standardRetryOptions());
        }
        if (Boolean.FALSE.equals(s3NativeClientConfiguration.isUseEnvironmentVariableValues())) {
            options.withProxyEnvironmentVariableSetting(disabledHttpProxyEnvironmentVariableSetting());
        }
        Optional.ofNullable(s3NativeClientConfiguration.proxyOptions()).ifPresent(options::withProxyOptions);
        Optional.ofNullable(s3NativeClientConfiguration.connectionTimeout())
                .map(Duration::toMillis)
                .map(NumericUtils::saturatedCast)
                .ifPresent(options::withConnectTimeoutMs);
        Optional.ofNullable(s3NativeClientConfiguration.httpMonitoringOptions())
                .ifPresent(options::withHttpMonitoringOptions);
        return options;
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest asyncRequest) {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        URI uri = asyncRequest.request().getUri();
        HttpRequest httpRequest = toCrtRequest(asyncRequest);
        SdkHttpExecutionAttributes httpExecutionAttributes = asyncRequest.httpExecutionAttributes();
        CompletableFuture<S3MetaRequestWrapper> s3MetaRequestFuture = new CompletableFuture<>();

        String operationName = asyncRequest.httpExecutionAttributes().getAttribute(OPERATION_NAME);
        S3MetaRequestOptions.MetaRequestType requestType = requestType(operationName);

        ResumeToken resumeToken = httpExecutionAttributes.getAttribute(CRT_PAUSE_RESUME_TOKEN);
        Region signingRegion = httpExecutionAttributes.getAttribute(SIGNING_REGION);
        Path requestFilePath = httpExecutionAttributes.getAttribute(OBJECT_FILE_PATH);

        HttpChecksum httpChecksum = httpExecutionAttributes.getAttribute(HTTP_CHECKSUM);
        RequestChecksumCalculation requestChecksumCalculation =
            httpExecutionAttributes.getAttribute(REQUEST_CHECKSUM_CALCULATION);
        ResponseChecksumValidation responseChecksumValidation =
            httpExecutionAttributes.getAttribute(RESPONSE_CHECKSUM_VALIDATION);
        ChecksumConfig checksumConfig = checksumConfig(httpChecksum, requestType, requestChecksumCalculation,
                                                       responseChecksumValidation);

        Path responseFilePath = httpExecutionAttributes.getAttribute(RESPONSE_FILE_PATH);
        S3MetaRequestOptions.ResponseFileOption responseFileOption = httpExecutionAttributes.getAttribute(RESPONSE_FILE_OPTION);

        S3CrtResponseHandlerAdapter responseHandler =
            new S3CrtResponseHandlerAdapter(
                executeFuture,
                asyncRequest.responseHandler(),
                httpExecutionAttributes.getAttribute(CRT_PROGRESS_LISTENER),
                s3MetaRequestFuture);

        URI endpoint = getEndpoint(uri);

        AwsSigningConfig signingConfig = awsSigningConfig(signingRegion, httpExecutionAttributes);

        S3MetaRequestOptions requestOptions = new S3MetaRequestOptions()
            .withHttpRequest(httpRequest)
            .withMetaRequestType(requestType)
            .withChecksumConfig(checksumConfig)
            .withEndpoint(endpoint)
            .withResponseHandler(responseHandler)
            .withResumeToken(resumeToken)
            .withOperationName(operationName)
            .withRequestFilePath(requestFilePath)
            .withSigningConfig(signingConfig)
            .withResponseFilePath(responseFilePath);

        if (responseFileOption != null) {
            requestOptions = requestOptions.withResponseFileOption(responseFileOption);
        }

        try {
            S3MetaRequestWrapper requestWrapper = new S3MetaRequestWrapper(crtS3Client.makeMetaRequest(requestOptions));
            s3MetaRequestFuture.complete(requestWrapper);

            S3MetaRequestPauseObservable observable =
                httpExecutionAttributes.getAttribute(METAREQUEST_PAUSE_OBSERVABLE);

            if (observable != null) {
                observable.subscribe(requestWrapper);
            }
        } finally {
            signingConfig.close();
        }

        return executeFuture;
    }

    private AwsSigningConfig awsSigningConfig(Region signingRegion, SdkHttpExecutionAttributes httpExecutionAttributes) {
        AwsSigningConfig defaultS3SigningConfig =
            AwsSigningConfig.getDefaultS3SigningConfig(s3ClientOptions.getRegion(), s3ClientOptions.getCredentialsProvider());

        // Override the region only if the signing region has changed from the previously configured region.
        if (signingRegion != null && !s3ClientOptions.getRegion().equals(signingRegion.id())) {
            defaultS3SigningConfig.setRegion(signingRegion.id());
        }

        defaultS3SigningConfig.setService(httpExecutionAttributes.getAttribute(SIGNING_NAME));

        if (Boolean.TRUE.equals(httpExecutionAttributes.getAttribute(USE_S3_EXPRESS_AUTH))) {
            defaultS3SigningConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_S3EXPRESS);
        }
        return defaultS3SigningConfig;
    }

    private static URI getEndpoint(URI uri) {
        return invokeSafely(() -> new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null));
    }

    @Override
    public String clientName() {
        return "s3crt";
    }

    private static S3MetaRequestOptions.MetaRequestType requestType(String operationName) {
        if (operationName != null) {
            switch (operationName) {
                case "GetObject":
                    return S3MetaRequestOptions.MetaRequestType.GET_OBJECT;
                case "PutObject":
                    return S3MetaRequestOptions.MetaRequestType.PUT_OBJECT;
                default:
                    return S3MetaRequestOptions.MetaRequestType.DEFAULT;
            }
        }
        return S3MetaRequestOptions.MetaRequestType.DEFAULT;
    }

    private static HttpRequest toCrtRequest(AsyncExecuteRequest asyncRequest) {
        SdkHttpRequest sdkRequest = asyncRequest.request();

        Path requestFilePath = asyncRequest.httpExecutionAttributes().getAttribute(OBJECT_FILE_PATH);

        String method = sdkRequest.method().name();
        String encodedPath = sdkRequest.encodedPath();
        if (encodedPath == null || encodedPath.isEmpty()) {
            encodedPath = "/";
        }

        String encodedQueryString = sdkRequest.encodedQueryParameters()
                                              .map(value -> "?" + value)
                                              .orElse("");

        HttpHeader[] crtHeaderArray = createHttpHeaderList(asyncRequest).toArray(new HttpHeader[0]);


        S3CrtRequestBodyStreamAdapter sdkToCrtRequestPublisher =
            requestFilePath == null ? new S3CrtRequestBodyStreamAdapter(asyncRequest.requestContentPublisher()) : null;

        return new HttpRequest(method, encodedPath + encodedQueryString, crtHeaderArray, sdkToCrtRequestPublisher);
    }

    @Override
    public void close() {
        s3NativeClientConfiguration.close();
        crtS3Client.close();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements SdkAsyncHttpClient.Builder<S3CrtAsyncHttpClient.Builder> {
        private S3NativeClientConfiguration clientConfiguration;

        public Builder s3ClientConfiguration(S3NativeClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        @Override
        public SdkAsyncHttpClient build() {
            return new S3CrtAsyncHttpClient(this);
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            // Intentionally ignore serviceDefaults
            return build();
        }
    }

    private static List<HttpHeader> createHttpHeaderList(AsyncExecuteRequest asyncRequest) {
        SdkHttpRequest sdkRequest = asyncRequest.request();
        List<HttpHeader> crtHeaderList = new ArrayList<>();

        // Set Host Header if needed
        if (!sdkRequest.firstMatchingHeader(Header.HOST).isPresent()) {
            String hostHeader = getHostHeaderValue(asyncRequest.request());
            crtHeaderList.add(new HttpHeader(Header.HOST, hostHeader));
        }

        // Set Content-Length if needed
        Optional<Long> contentLength = asyncRequest.requestContentPublisher().contentLength();
        if (!sdkRequest.firstMatchingHeader(Header.CONTENT_LENGTH).isPresent() && contentLength.isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.CONTENT_LENGTH, Long.toString(contentLength.get())));
        }

        // Add the rest of the Headers
        sdkRequest.forEachHeader((key, value) -> value.stream().map(val -> new HttpHeader(key, val))
                                                      .forEach(crtHeaderList::add));

        return crtHeaderList;
    }

    private static String getHostHeaderValue(SdkHttpRequest request) {
        return SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())
               ? request.host()
               : request.host() + ":" + request.port();
    }

    private static HttpProxyEnvironmentVariableSetting disabledHttpProxyEnvironmentVariableSetting() {
        HttpProxyEnvironmentVariableSetting proxyEnvSetting = new HttpProxyEnvironmentVariableSetting();
        proxyEnvSetting.setEnvironmentVariableType(HttpProxyEnvironmentVariableSetting.HttpProxyEnvironmentVariableType.DISABLED);
        return proxyEnvSetting;
    }
}
