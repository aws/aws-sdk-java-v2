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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.HTTP_CHECKSUM;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.OPERATION_NAME;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.REQUEST_CHECKSUM_CALCULATION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.RESPONSE_FILE_OPTION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.RESPONSE_FILE_PATH;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.SIGNING_NAME;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.USE_S3_EXPRESS_AUTH;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.http.HttpProxyEnvironmentVariableSetting;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions;
import software.amazon.awssdk.crt.io.StandardRetryOptions;
import software.amazon.awssdk.crt.s3.ChecksumAlgorithm;
import software.amazon.awssdk.crt.s3.S3Client;
import software.amazon.awssdk.crt.s3.S3ClientOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.testutils.RandomTempFile;

public class S3CrtAsyncHttpClientTest {
    private static final URI DEFAULT_ENDPOINT = URI.create("https://127.0.0.1:443");

    private S3CrtAsyncHttpClient asyncHttpClient;
    private S3NativeClientConfiguration s3NativeClientConfiguration;

    private S3Client s3Client;

    private SdkAsyncHttpResponseHandler responseHandler;

    private SdkHttpContentPublisher contentPublisher;

    @BeforeEach
    public void methodSetup() {
        s3Client = Mockito.mock(S3Client.class);
        responseHandler = Mockito.mock(SdkAsyncHttpResponseHandler.class);
        contentPublisher = Mockito.mock(SdkHttpContentPublisher.class);
        s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
                                                                 .endpointOverride(DEFAULT_ENDPOINT)
                                                                 .credentialsProvider(
                                                                     StaticCredentialsProvider.create(AwsBasicCredentials.create("FOO", "BARR")))
                                                                 .signingRegion("us-west-2")
                                                                 .build();

        asyncHttpClient = new S3CrtAsyncHttpClient(s3Client,
                                                   S3CrtAsyncHttpClient.builder()
                                                                       .s3ClientConfiguration(s3NativeClientConfiguration));
    }

    private static Stream<Integer> ports() {
        return Stream.of(null, 1234, 443);
    }

    @ParameterizedTest
    @MethodSource("ports")
    public void defaultRequest_shouldSetMetaRequestOptionsCorrectly(Integer port) {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder(port).build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.DEFAULT);
        assertThat(actual.getCredentialsProvider()).isNull();
        String expectedEndpoint = port == null || port.equals(443) ?
                                  DEFAULT_ENDPOINT.getScheme() + "://" + DEFAULT_ENDPOINT.getHost() :
                                  DEFAULT_ENDPOINT.getScheme() + "://" + DEFAULT_ENDPOINT.getHost() + ":" + port;
        assertThat(actual.getEndpoint()).hasToString(expectedEndpoint);


        HttpRequest httpRequest = actual.getHttpRequest();
        assertThat(httpRequest.getEncodedPath()).isEqualTo("/key");

        Map<String, String> headers = httpRequest.getHeaders()
                                                 .stream()
                                                 .collect(HashMap::new, (m, h) -> m.put(h.getName(), h.getValue())
                                                     , Map::putAll);

        String expectedPort = port == null || port.equals(443)  ? "" : ":" + port;
        assertThat(headers).hasSize(4)
                           .containsEntry("Host", DEFAULT_ENDPOINT.getHost() + expectedPort)
                           .containsEntry("custom-header", "foobar")
                           .containsEntry("amz-sdk-invocation-id", "1234")
                           .containsEntry("Content-Length", "100");
    }

    @Test
    public void getObject_shouldSetMetaRequestTypeCorrectly() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                          "GetObject").build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.GET_OBJECT);
        assertThat(actual.getOperationName()).isEqualTo("GetObject");
    }

    @Test
    public void putObject_shouldSetMetaRequestTypeCorrectly() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                          "PutObject").build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.PUT_OBJECT);
        assertThat(actual.getOperationName()).isEqualTo("PutObject");
    }

    @Test
    public void nonStreamingOperation_shouldSetMetaRequestTypeCorrectly() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "CreateBucket").build();
        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.DEFAULT);
        assertThat(actual.getOperationName()).isEqualTo("CreateBucket");
    }


    @Test
    public void cancelRequest_shouldForwardCancellation() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().build();
        S3MetaRequest metaRequest = Mockito.mock(S3MetaRequest.class);
        when(s3Client.makeMetaRequest(any(S3MetaRequestOptions.class))).thenReturn(metaRequest);

        CompletableFuture<Void> future = asyncHttpClient.execute(asyncExecuteRequest);

        future.cancel(false);

        verify(metaRequest).cancel();
    }

    @Test
    public void nonStreamingOperation_noChecksumAlgoProvided_shouldSetToDefaultCRC32() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(false)
                                                .build();

        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "NonStreaming")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumConfig().getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC32);
    }

    @Test
    public void nonStreamingOperation_checksumAlgoSHA1Provided_shouldPassToCrt() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(false)
                                                .requestAlgorithm("SHA1")
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "NonStreaming")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumConfig().getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.SHA1);
    }

    @Test
    public void nonStreamingOperation_checksumAlgoCRC64NVMEProvided_shouldPassToCrt() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(false)
                                                .requestAlgorithm("CRC64NVME")
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "NonStreaming")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumConfig().getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC64NVME);
    }

    @Test
    public void checksumRequiredOperation_noChecksumAlgoProvided_shouldSetByDefault() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .requestChecksumRequired(true)
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "PutObject")
                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .putHttpExecutionAttribute(REQUEST_CHECKSUM_CALCULATION,
                                                                                                       RequestChecksumCalculation.WHEN_SUPPORTED)
                                                                            .putHttpExecutionAttribute(RESPONSE_CHECKSUM_VALIDATION,
                                                                                                       ResponseChecksumValidation.WHEN_SUPPORTED)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC32);
    }

    @Test
    public void streamingOperation_noChecksumAlgoProvided_shouldSetByDefault() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(true)
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "PutObject")
                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .putHttpExecutionAttribute(REQUEST_CHECKSUM_CALCULATION,
                                                                                                       RequestChecksumCalculation.WHEN_SUPPORTED)
                                                                            .putHttpExecutionAttribute(RESPONSE_CHECKSUM_VALIDATION,
                                                                                                       ResponseChecksumValidation.WHEN_SUPPORTED)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC32);
    }

    @Test
    public void operationWithResponseAlgorithms_validateNotSpecified_shouldValidateByDefault() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .responseAlgorithms("CRC32")
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "GetObject")
                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .putHttpExecutionAttribute(REQUEST_CHECKSUM_CALCULATION,
                                                                                                       RequestChecksumCalculation.WHEN_SUPPORTED)
                                                                            .putHttpExecutionAttribute(RESPONSE_CHECKSUM_VALIDATION,
                                                                                                       ResponseChecksumValidation.WHEN_SUPPORTED)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getValidateChecksum()).isTrue();
    }

    @Test
    public void operationWithResponseAlgorithms_optOutValidationFromClient_shouldHonor() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .responseAlgorithms("CRC32")
                                                .build();

        s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
                                                                 .endpointOverride(DEFAULT_ENDPOINT)
                                                                 .credentialsProvider(null)
                                                                 .build();

        asyncHttpClient = new S3CrtAsyncHttpClient(s3Client, S3CrtAsyncHttpClient.builder()
                                                                                 .s3ClientConfiguration(s3NativeClientConfiguration));

        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "GetObject")
                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getValidateChecksum()).isFalse();
    }

    @Test
    public void operationWithResponseAlgorithms_optInFromRequest_shouldHonor() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .responseAlgorithms("CRC32")
                                                .requestValidationMode("Enabled")
                                                .build();

        s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
                                                                 .endpointOverride(DEFAULT_ENDPOINT)
                                                                 .credentialsProvider(null)
                                                                 .build();

        asyncHttpClient = new S3CrtAsyncHttpClient(s3Client, S3CrtAsyncHttpClient.builder()
                                                                                 .s3ClientConfiguration(s3NativeClientConfiguration));

        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "GetObject")
                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getValidateChecksum()).isTrue();
    }

    private S3MetaRequestOptions makeRequest(AsyncExecuteRequest asyncExecuteRequest) {
        ArgumentCaptor<S3MetaRequestOptions> s3MetaRequestOptionsArgumentCaptor =
            ArgumentCaptor.forClass(S3MetaRequestOptions.class);

        asyncHttpClient.execute(asyncExecuteRequest);

        verify(s3Client).makeMetaRequest(s3MetaRequestOptionsArgumentCaptor.capture());

        return s3MetaRequestOptionsArgumentCaptor.getValue();
    }

    @Test
    public void streamingOperation_checksumAlgoProvided_shouldTakePrecedence() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(true)
                                                .requestAlgorithm("SHA1")
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "PutObject")
                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.SHA1);
    }

    @Test
    public void s3Express_shouldUseS3ExpressSigner() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(true)
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "PutObject")
                                                                            .putHttpExecutionAttribute(SIGNING_REGION,
                                                                                                       Region.AP_SOUTH_1)
                                                                            .putHttpExecutionAttribute(SIGNING_NAME, "s3express")
                                                                            .putHttpExecutionAttribute(USE_S3_EXPRESS_AUTH, true)
                                                                            .putHttpExecutionAttribute(REQUEST_CHECKSUM_CALCULATION,
                                                                                                       RequestChecksumCalculation.WHEN_SUPPORTED)
                                                                            .putHttpExecutionAttribute(RESPONSE_CHECKSUM_VALIDATION,
                                                                                                       ResponseChecksumValidation.WHEN_SUPPORTED)
                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC32);
        AwsSigningConfig signingConfig = actual.getSigningConfig();
        assertThat(signingConfig.getRegion()).isEqualTo("ap-south-1");
        assertThat(signingConfig.getAlgorithm()).isEqualTo(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_S3EXPRESS);
        assertThat(signingConfig.getService()).isEqualTo("s3express");
    }

    @Test
    public void nonS3Express_shouldUseDefaultSigner() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(true)
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "PutObject")
                                                                            .putHttpExecutionAttribute(SIGNING_REGION,
                                                                                                       Region.AP_SOUTH_1)
                                                                            .putHttpExecutionAttribute(USE_S3_EXPRESS_AUTH, false)
                                                                            .putHttpExecutionAttribute(SIGNING_NAME, "s3")
                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .putHttpExecutionAttribute(REQUEST_CHECKSUM_CALCULATION,
                                                                                                       RequestChecksumCalculation.WHEN_SUPPORTED)
                                                                            .putHttpExecutionAttribute(RESPONSE_CHECKSUM_VALIDATION,
                                                                                                       ResponseChecksumValidation.WHEN_SUPPORTED)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC32);
        AwsSigningConfig signingConfig = actual.getSigningConfig();
        assertThat(signingConfig.getRegion()).isEqualTo("ap-south-1");
        assertThat(signingConfig.getAlgorithm()).isEqualTo(AwsSigningConfig.AwsSigningAlgorithm.SIGV4);
        assertThat(signingConfig.getService()).isEqualTo("s3");
    }

    @Test
    public void closeHttpClient_shouldCloseUnderlyingResources() {
        asyncHttpClient.close();
        verify(s3Client).close();
        s3NativeClientConfiguration.close();
    }

    @Test
    void build_shouldPassThroughParameters() {
        String signingRegion = "us-west-2";
        S3NativeClientConfiguration configuration =
            S3NativeClientConfiguration.builder()
                                       .maxConcurrency(100)
                                       .signingRegion(signingRegion)
                                       .thresholdInBytes(1024L)
                                       .targetThroughputInGbps(3.5)
                                       .maxNativeMemoryLimitInBytes(5L * 1024 * 1024 * 1024)
                                       .standardRetryOptions(
                                           new StandardRetryOptions()
                                               .withBackoffRetryOptions(new ExponentialBackoffRetryOptions().withMaxRetries(7)))
                                       .httpConfiguration(S3CrtHttpConfiguration.builder()
                                                                                .connectionTimeout(Duration.ofSeconds(1))
                                                                                .connectionHealthConfiguration(c -> c.minimumThroughputInBps(1024L)
                                                                                                                     .minimumThroughputTimeout(Duration.ofSeconds(2)))
                                                                                .proxyConfiguration(p -> p.host("127.0.0.1").port(8080))
                                                                                .build())
                                       .build();
        try (S3CrtAsyncHttpClient client =
                 (S3CrtAsyncHttpClient) S3CrtAsyncHttpClient.builder().s3ClientConfiguration(configuration).build()) {
            S3ClientOptions clientOptions = client.s3ClientOptions();
            assertThat(clientOptions.getConnectTimeoutMs()).isEqualTo(1000);
            assertThat(clientOptions.getMultiPartUploadThreshold()).isEqualTo(1024);
            assertThat(clientOptions.getStandardRetryOptions().getBackoffRetryOptions().getMaxRetries()).isEqualTo(7);
            assertThat(clientOptions.getMaxConnections()).isEqualTo(100);
            assertThat(clientOptions.getMonitoringOptions()).satisfies(options -> {
                assertThat(options.getMinThroughputBytesPerSecond()).isEqualTo(1024);
                assertThat(options.getAllowableThroughputFailureIntervalSeconds()).isEqualTo(2);
            });
            assertThat(clientOptions.getProxyOptions()).satisfies(options -> {
                assertThat(options.getHost()).isEqualTo("127.0.0.1");
                assertThat(options.getPort()).isEqualTo(8080);
            });
            assertThat(clientOptions.getMonitoringOptions()).satisfies(options -> {
                assertThat(options.getAllowableThroughputFailureIntervalSeconds()).isEqualTo(2);
                assertThat(options.getMinThroughputBytesPerSecond()).isEqualTo(1024);
            });
            assertThat(clientOptions.getMaxConnections()).isEqualTo(100);
            assertThat(clientOptions.getThroughputTargetGbps()).isEqualTo(3.5);
            assertThat(clientOptions.getMemoryLimitInBytes()).isEqualTo(5L * 1024 * 1024 * 1024);
        }
    }

    @Test
    void build_partSizeConfigured_shouldApplyToThreshold() {
        long partSizeInBytes = 1024 * 8L;
        S3NativeClientConfiguration configuration =
            S3NativeClientConfiguration.builder()
                                       .partSizeInBytes(partSizeInBytes)
                                       .build();
        try (S3CrtAsyncHttpClient client =
                 (S3CrtAsyncHttpClient) S3CrtAsyncHttpClient.builder().s3ClientConfiguration(configuration).build()) {
            S3ClientOptions clientOptions = client.s3ClientOptions();
            assertThat(clientOptions.getPartSize()).isEqualTo(partSizeInBytes);
            assertThat(clientOptions.getMultiPartUploadThreshold()).isEqualTo(clientOptions.getPartSize());
        }
    }

    @Test
    void build_nullHttpConfiguration() {
        S3NativeClientConfiguration configuration =
            S3NativeClientConfiguration.builder()
                                       .build();
        try (S3CrtAsyncHttpClient client =
                 (S3CrtAsyncHttpClient) S3CrtAsyncHttpClient.builder().s3ClientConfiguration(configuration).build()) {
            S3ClientOptions clientOptions = client.s3ClientOptions();
            assertThat(clientOptions.getConnectTimeoutMs()).isZero();
            assertThat(clientOptions.getMaxConnections()).isZero();
            assertThat(clientOptions.getMonitoringOptions()).isNull();
            assertThat(clientOptions.getProxyOptions()).isNull();
            assertThat(clientOptions.getMonitoringOptions()).isNull();
        }
    }

    private static Stream<Arguments> s3CrtHttpConfigurations() {
        return Stream.of(
            Arguments.of(S3CrtHttpConfiguration.builder()
                                               .connectionTimeout(Duration.ofSeconds(1))
                                               .connectionHealthConfiguration(c -> c.minimumThroughputInBps(1024L)
                                                                                    .minimumThroughputTimeout(Duration.ofSeconds(2)))
                                               .proxyConfiguration(p -> p.host("127.0.0.1").port(8080))
                                               .build(),
                         null,
                         "S3CrtHttpConfiguration with default useEnvironmentVariableFlag does not set "
                         + "HttpProxyEnvironmentVariableSetting"),

            Arguments.of(S3CrtHttpConfiguration.builder()
                                               .proxyConfiguration(p -> p.host("127.0.0.1").port(8080).useEnvironmentVariableValues(false))
                                               .build(),
                         HttpProxyEnvironmentVariableSetting.HttpProxyEnvironmentVariableType.DISABLED,
                         "S3CrtHttpConfiguration with other settings and useEnvironmentVariableFlag as false sets "
                         + "HttpProxyEnvironmentVariableSetting as DISABLED"),

            Arguments.of(S3CrtHttpConfiguration.builder().build(),
                         null, "S3CrtHttpConfiguration as null does not set HttpProxyEnvironmentVariableSetting"),

            Arguments.of(S3CrtHttpConfiguration.builder()
                                               .proxyConfiguration(p -> p.useEnvironmentVariableValues(false))
                                               .build(),
                         HttpProxyEnvironmentVariableSetting.HttpProxyEnvironmentVariableType.DISABLED,
                         "S3CrtHttpConfiguration with only useEnvironmentVariableFlag as false sets "
                         + "HttpProxyEnvironmentVariableSetting as DISABLED"
            ),

            Arguments.of(S3CrtHttpConfiguration.builder()
                                               .proxyConfiguration(p -> p.useEnvironmentVariableValues(true))
                                               .build(),
                         null,
                         "S3CrtHttpConfiguration with only useEnvironmentVariableFlag as true sets "
                         + "does not set HttpProxyEnvironmentVariableSetting")
        );
    }

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("s3CrtHttpConfigurations")
    void build_ProxyConfigurationWithEnvironmentVariables(S3CrtHttpConfiguration s3CrtHttpConfiguration,
                                                          HttpProxyEnvironmentVariableSetting.HttpProxyEnvironmentVariableType environmentVariableType,
                                                          String testCase) {
        S3NativeClientConfiguration configuration =
            S3NativeClientConfiguration.builder()
                                       .httpConfiguration(s3CrtHttpConfiguration)
                                       .build();
        try(S3CrtAsyncHttpClient client =
            (S3CrtAsyncHttpClient) S3CrtAsyncHttpClient.builder().s3ClientConfiguration(configuration).build()) {
            S3ClientOptions clientOptions = client.s3ClientOptions();
            if (environmentVariableType == null) {
                assertThat(clientOptions.getHttpProxyEnvironmentVariableSetting()).isNull();
            } else {
                assertThat(clientOptions.getHttpProxyEnvironmentVariableSetting().getEnvironmentVariableType())
                    .isEqualTo(environmentVariableType);
            }
        }
    }

    @Test
    public void responseFilePathAndOption_shouldPassToCrt() {
        Path path = RandomTempFile.randomUncreatedFile().toPath();

        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder()
            .putHttpExecutionAttribute(OPERATION_NAME,"GetObject")
            .putHttpExecutionAttribute(RESPONSE_FILE_PATH, path)
            .putHttpExecutionAttribute(RESPONSE_FILE_OPTION, S3MetaRequestOptions.ResponseFileOption.CREATE_OR_APPEND)
            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getResponseFilePath()).isEqualTo(path);
        assertThat(actual.getResponseFileOption()).isEqualTo(S3MetaRequestOptions.ResponseFileOption.CREATE_OR_APPEND);
        S3MetaRequestResponseHandler handler = actual.getResponseHandler();
        System.out.println(handler);
    }

    private AsyncExecuteRequest.Builder getExecuteRequestBuilder() {
        return getExecuteRequestBuilder(443);
    }

    private AsyncExecuteRequest.Builder getExecuteRequestBuilder(Integer port) {
        return AsyncExecuteRequest.builder()
                                  .responseHandler(responseHandler)
                                  .requestContentPublisher(contentPublisher)
                                  .request(SdkHttpRequest.builder()
                                                         .protocol(DEFAULT_ENDPOINT.getScheme())
                                                         .method(SdkHttpMethod.GET)
                                                         .host(DEFAULT_ENDPOINT.getHost())
                                                         .port(port)
                                                         .encodedPath("/key")
                                                         .putHeader(CONTENT_LENGTH, "100")
                                                         .putHeader("amz-sdk-invocation-id",
                                                                    "1234")
                                                         .putHeader("custom-header", "foobar")
                                                         .build());
    }
}
