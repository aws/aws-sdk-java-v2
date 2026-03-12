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

package software.amazon.awssdk.services.s3.functionaltests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.rules.testing.BaseRuleSetClientTest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.Protocol;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@WireMockTest(httpsEnabled = true)
public class S3ExpressTest extends BaseRuleSetClientTest {
    private static final Logger log = Logger.loggerFor(S3ExpressTest.class);
    private static final Function<WireMockRuntimeInfo, URI> WM_HTTP_ENDPOINT = wm -> URI.create(wm.getHttpBaseUrl());
    private static final Function<WireMockRuntimeInfo, URI> WM_HTTPS_ENDPOINT = wm -> URI.create(wm.getHttpsBaseUrl());
    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    private static final PathStyleEnforcingInterceptor PATH_STYLE_INTERCEPTOR = new PathStyleEnforcingInterceptor();
    private static final CapturingInterceptor CAPTURING_INTERCEPTOR = new CapturingInterceptor();
    private static final String DEFAULT_BUCKET = "s3expressformat--use1-az1--x-s3";
    private static final String DEFAULT_KEY = "foo.txt";
    private static final String GET_BODY = "Hello world!";
    private static final String PUT_BODY = "Hello from Java SDK";

    private static final String CREATE_SESSION_RESPONSE = String.format(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<ConnectResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n"
        + "<Credentials>\n"
        + "<SessionToken>%s</SessionToken>\n"
        + "<SecretAccessKey>%s</SecretAccessKey>\n"
        + "<AccessKeyId>%s</AccessKeyId>"
        + "</Credentials>\n"
        + "</ConnectResult>", "TheToken", "TheSecret", "TheAccessKey");

    @BeforeEach
    public void commonSetup() {
        stubFor(get(urlMatching("/.*session")).atPriority(1).willReturn(aResponse()
                                                                            .withStatus(200)
                                                                            .withBody(CREATE_SESSION_RESPONSE)));
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(200)));
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(GET_BODY)));
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    public void putObject(ClientType clientType, Protocol protocol,
                          S3ExpressSessionAuth s3ExpressSessionAuth, ChecksumAlgorithm checksumAlgorithm,
                          WireMockRuntimeInfo wm) {
        createClientAndCallPutObject(clientType, protocol, s3ExpressSessionAuth, checksumAlgorithm, wm);

        verifyPutObject(s3ExpressSessionAuth);
        verifyPutObjectHeaders(clientType, protocol, checksumAlgorithm);
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    public void uploadPart(ClientType clientType, Protocol protocol,
                           S3ExpressSessionAuth s3ExpressSessionAuth, ChecksumAlgorithm checksumAlgorithm,
                           WireMockRuntimeInfo wm) {
        createClientAndCallUploadPart(clientType, protocol, s3ExpressSessionAuth, checksumAlgorithm, wm);

        verifyUploadPart(s3ExpressSessionAuth);
        verifyUploadPartHeaders(clientType, protocol);
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    public void getObject(ClientType clientType, Protocol protocol,
                          S3ExpressSessionAuth s3ExpressSessionAuth, ChecksumAlgorithm checksumAlgorithm,
                          WireMockRuntimeInfo wm) {
        createClientAndCallGetObject(clientType, protocol, s3ExpressSessionAuth, checksumAlgorithm, wm);

        verifyGetObject(s3ExpressSessionAuth);
        verifyGetObjectHeaders();
    }

    @ParameterizedTest
    @MethodSource("syncOnlyTestParameters")
    public void operation_withChecksumRequired_CalculatesCrc32InsteadOfMD5(Protocol protocol,
                                                                           S3ExpressSessionAuth s3ExpressSessionAuth,
                                                                           ChecksumAlgorithm checksumAlgorithm,
                                                                           WireMockRuntimeInfo wm) {
        S3Client syncClient = getSyncClient(protocol, wm, s3ExpressSessionAuth);
        DeleteObjectsRequest.Builder requestBuilder = DeleteObjectsRequest.builder().bucket(DEFAULT_BUCKET)
                                                                          .delete(Delete.builder().build());
        if (checksumAlgorithm != ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION) {
            requestBuilder.checksumAlgorithm(checksumAlgorithm);
        }
        syncClient.deleteObjects(requestBuilder.build());

        Map<String, List<String>> headers = CAPTURING_INTERCEPTOR.headers;
        assertThat(headers.get("Content-Length")).isNotNull();
        assertThat(headers.get("x-amz-content-sha256")).isNotNull();
        if (checksumAlgorithm == ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION) {
            assertThat(headers.get("x-amz-sdk-checksum-algorithm")).isNotNull();
            assertThat(headers.get("x-amz-sdk-checksum-algorithm").get(0)).isEqualToIgnoringCase(ChecksumAlgorithm.CRC32.name());

            String expectedChecksumHeader = String.format("x-amz-checksum-%s", ChecksumAlgorithm.CRC32.name());
            assertThat(headers.get(expectedChecksumHeader)).isNotNull();
        } else {
            assertThat(headers.get("x-amz-sdk-checksum-algorithm")).isNotNull();
            assertThat(headers.get("x-amz-sdk-checksum-algorithm").get(0)).isEqualToIgnoringCase(checksumAlgorithm.name());
            String expectedChecksumHeader = String.format("x-amz-checksum-%s", checksumAlgorithm.name());
            assertThat(headers.get(expectedChecksumHeader)).isNotNull();
        }
    }

    @ParameterizedTest
    @MethodSource("syncOnlyTestParameters")
    public void operation_withoutChecksumTrait(Protocol protocol,
                                               S3ExpressSessionAuth s3ExpressSessionAuth,
                                               ChecksumAlgorithm checksumAlgorithm,
                                               WireMockRuntimeInfo wm) {

        S3Client syncClient = getSyncClient(protocol, wm, s3ExpressSessionAuth);
        CreateBucketRequest request = CreateBucketRequest.builder().bucket(DEFAULT_BUCKET).build();
        syncClient.createBucket(request);

        Map<String, List<String>> headers = CAPTURING_INTERCEPTOR.headers;
        assertThat(headers.get("x-amz-sdk-checksum-algorithm")).isNull();
        assertThat(headers.get("Content-MD5")).isNull();
        assertSignatureHeader(headers);
    }

    private void assertSignatureHeader(Map<String, List<String>> headers) {
        assertThat(headers.get("x-amz-content-sha256")).isNotNull();
        assertThat(headers.get("x-amz-content-sha256").get(0)).isEqualToIgnoringCase("UNSIGNED-PAYLOAD");
    }

    private void createClientAndCallPutObject(ClientType clientType, Protocol protocol, S3ExpressSessionAuth s3ExpressSessionAuth,
                                              ChecksumAlgorithm checksumAlgorithm, WireMockRuntimeInfo wm) {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY);
        if (checksumAlgorithm != ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION) {
            requestBuilder.checksumAlgorithm(checksumAlgorithm);
        }
        PutObjectRequest putObjectRequest = requestBuilder.build();
        if (clientType == ClientType.SYNC) {
            getSyncClient(protocol, wm, s3ExpressSessionAuth).putObject(putObjectRequest, RequestBody.fromString(PUT_BODY));
        } else {
            getAsyncClient(protocol, wm, s3ExpressSessionAuth).putObject(putObjectRequest, AsyncRequestBody.fromString(PUT_BODY)).join();
        }
    }

    private void createClientAndCallUploadPart(ClientType clientType, Protocol protocol, S3ExpressSessionAuth s3ExpressSessionAuth,
                                               ChecksumAlgorithm checksumAlgorithm, WireMockRuntimeInfo wm) {
        UploadPartRequest.Builder requestBuilder =
            UploadPartRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY).partNumber(0).uploadId("test");
        if (checksumAlgorithm != ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION) {
            requestBuilder.checksumAlgorithm(checksumAlgorithm);
        }
        UploadPartRequest uploadPartRequest = requestBuilder.build();
        if (clientType == ClientType.SYNC) {
            getSyncClient(protocol, wm, s3ExpressSessionAuth).uploadPart(uploadPartRequest, RequestBody.fromString(PUT_BODY));
        } else {
            getAsyncClient(protocol, wm, s3ExpressSessionAuth).uploadPart(uploadPartRequest, AsyncRequestBody.fromString(PUT_BODY)).join();
        }
    }

    private void createClientAndCallGetObject(ClientType clientType, Protocol protocol, S3ExpressSessionAuth s3ExpressSessionAuth,
                                              ChecksumAlgorithm checksumAlgorithm, WireMockRuntimeInfo wm) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY).build();
        if (clientType == ClientType.SYNC) {
            getSyncClient(protocol, wm, s3ExpressSessionAuth).getObject(getObjectRequest);
        } else {
            getAsyncClient(protocol, wm, s3ExpressSessionAuth).getObject(getObjectRequest, AsyncResponseTransformer.toBytes()).join();
        }
    }

    private static void verifyPutObject(S3ExpressSessionAuth s3ExpressSessionAuth) {
        if (s3ExpressSessionAuth != S3ExpressSessionAuth.DISABLE_AUTH) {
            verifySessionHeaders();
            verify(1, putRequestedFor(urlMatching("/.*" + DEFAULT_KEY))
                .withHeader("x-amz-s3session-token", equalTo("TheToken")));
        } else {
            verify(1, putRequestedFor(urlMatching("/.*" + DEFAULT_KEY)));
        }
    }

    private static void verifyUploadPart(S3ExpressSessionAuth s3ExpressSessionAuth) {
        if (s3ExpressSessionAuth != S3ExpressSessionAuth.DISABLE_AUTH) {
            verifySessionHeaders();
            verify(1, putRequestedFor(urlMatching("/.*"))
                .withHeader("x-amz-s3session-token", equalTo("TheToken")));
        } else {
            verify(1, putRequestedFor(urlMatching("/.*")));
        }
    }

    private static void verifyGetObject(S3ExpressSessionAuth s3ExpressSessionAuth) {
        if (s3ExpressSessionAuth != S3ExpressSessionAuth.DISABLE_AUTH) {
            verifySessionHeaders();
            verify(1, getRequestedFor(urlMatching("/.*" + DEFAULT_KEY))
                .withHeader("x-amz-s3session-token", equalTo("TheToken")));
        } else {
            verify(1, getRequestedFor(urlMatching("/.*" + DEFAULT_KEY)));
        }
    }

    private static void verifySessionHeaders() {
        verify(1, getRequestedFor(urlMatching("/.*session"))
            .withoutHeader("x-amz-create-session-mode")
            .withHeader("x-amz-content-sha256", equalTo("UNSIGNED-PAYLOAD")));
    }

    void verifyPutObjectHeaders(ClientType clientType, Protocol protocol, ChecksumAlgorithm checksumAlgorithm) {
        String streamingSha256 = "STREAMING-UNSIGNED-PAYLOAD-TRAILER";
        if (protocol == Protocol.HTTP && clientType == ClientType.SYNC) {
            streamingSha256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER";
        }
        ChecksumAlgorithm expectedChecksumAlgorithm = checksumAlgorithm == ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION ?
                                                      ChecksumAlgorithm.CRC32 : checksumAlgorithm;

        Map<String, List<String>> headers = CAPTURING_INTERCEPTOR.headers;
        assertThat(headers.get("Content-Length")).isNotNull();
        assertThat(headers.get("x-amz-decoded-content-length")).isNotNull();
        assertThat(headers.get("Content-encoding")).isNotNull();
        assertThat(headers.get("Content-encoding").get(0)).isEqualToIgnoringCase("aws-chunked");
        assertThat(headers.get("x-amz-sdk-checksum-algorithm")).isNotNull();
        assertThat(headers.get("x-amz-sdk-checksum-algorithm").get(0)).isEqualToIgnoringCase(expectedChecksumAlgorithm.name());
        assertThat(headers.get("x-amz-trailer")).isNotNull();
        assertThat(headers.get("x-amz-trailer").get(0)).isEqualToIgnoringCase(String.format("x-amz-checksum-%s",
                                                                                            expectedChecksumAlgorithm.name()));
        assertThat(headers.get("x-amz-content-sha256")).isNotNull();
        assertThat(headers.get("x-amz-content-sha256").get(0)).isEqualToIgnoringCase(streamingSha256);
    }

    void verifyUploadPartHeaders(ClientType clientType, Protocol protocol) {
        Map<String, List<String>> headers = CAPTURING_INTERCEPTOR.headers;
        assertThat(headers.get("Content-Length")).isNotNull();
        assertThat(headers.get("x-amz-content-sha256")).isNotNull();

        assertThat(headers.get("x-amz-decoded-content-length")).isNotNull();
        String streamingSha256 = "STREAMING-UNSIGNED-PAYLOAD-TRAILER";
        if (protocol == Protocol.HTTP && clientType == ClientType.SYNC) {
            streamingSha256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER";
        }
        assertThat(headers.get("x-amz-content-sha256").get(0)).isEqualToIgnoringCase(streamingSha256);
    }

    void verifyGetObjectHeaders() {
        Map<String, List<String>> headers = CAPTURING_INTERCEPTOR.headers;
        assertSignatureHeader(headers);
        assertThat(headers.get("x-amz-te")).isNull();
    }

    private static List<Arguments> testParameters() {
        List<Arguments> testCases = new ArrayList<>();
        for (ClientType clientType : ClientType.values()) {
            for (Protocol protocol : httpProtocols) {
                for (S3ExpressSessionAuth s3ExpressSessionAuth : S3ExpressSessionAuth.values()) {
                    for (ChecksumAlgorithm checksumAlgorithm : checksumAlgorithms) {
                        testCases.add(Arguments.arguments(clientType, protocol, s3ExpressSessionAuth, checksumAlgorithm));
                    }
                }
            }
        }
        return testCases;
    }

    private static List<Arguments> syncOnlyTestParameters() {
        List<Arguments> testCases = new ArrayList<>();
        for (Protocol protocol : httpProtocols) {
            for (S3ExpressSessionAuth s3ExpressSessionAuth : S3ExpressSessionAuth.values()) {
                for (ChecksumAlgorithm checksumAlgorithm : checksumAlgorithms) {
                    testCases.add(Arguments.arguments(protocol, s3ExpressSessionAuth, checksumAlgorithm));
                }
            }
        }
        return testCases;
    }

    private enum ClientType {
        SYNC,
        ASYNC
    }

    private enum S3ExpressSessionAuth {
        ENABLE_AUTH,
        DISABLE_AUTH
    }

    private static final Set<Protocol> httpProtocols = EnumSet.of(Protocol.HTTP, Protocol.HTTPS);
    private static final Set<ChecksumAlgorithm> checksumAlgorithms = EnumSet.of(ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION,
                                                                                ChecksumAlgorithm.CRC32,
                                                                                ChecksumAlgorithm.SHA1);

    private S3Client getSyncClient(Protocol protocol, WireMockRuntimeInfo wm, S3ExpressSessionAuth s3ExpressSessionAuth) {
        S3ClientBuilder s3ClientBuilder = getS3ClientBuilder();
        if (protocol == Protocol.HTTP) {
            s3ClientBuilder.endpointOverride(WM_HTTP_ENDPOINT.apply(wm));
        } else {
            s3ClientBuilder.endpointOverride(WM_HTTPS_ENDPOINT.apply(wm))
                           .httpClient(ApacheHttpClient.builder()
                                                       .buildWithDefaults(AttributeMap.builder()
                                                                                      .put(TRUST_ALL_CERTIFICATES, TRUE)
                                                                                      .build()));
        }
        if (s3ExpressSessionAuth == S3ExpressSessionAuth.DISABLE_AUTH) {
            s3ClientBuilder.disableS3ExpressSessionAuth(true);
        }
        return s3ClientBuilder.build();
    }

    private S3AsyncClient getAsyncClient(Protocol protocol, WireMockRuntimeInfo wm, S3ExpressSessionAuth s3ExpressSessionAuth) {
        S3AsyncClientBuilder s3ClientBuilder = getS3AsyncClientBuilder();
        if (protocol == Protocol.HTTP) {
            s3ClientBuilder.endpointOverride(WM_HTTP_ENDPOINT.apply(wm));
        } else {
            s3ClientBuilder.endpointOverride(WM_HTTPS_ENDPOINT.apply(wm))
                           .httpClient(NettyNioAsyncHttpClient.builder()
                                                              .buildWithDefaults(AttributeMap.builder()
                                                                                             .put(TRUST_ALL_CERTIFICATES, true).build()));
        }
        if (s3ExpressSessionAuth == S3ExpressSessionAuth.DISABLE_AUTH) {
            s3ClientBuilder.disableS3ExpressSessionAuth(true);
        }
        return s3ClientBuilder.build();
    }

    private S3ClientBuilder getS3ClientBuilder() {
        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .overrideConfiguration(c -> c.addExecutionInterceptor(CAPTURING_INTERCEPTOR)
                                                    .addExecutionInterceptor(PATH_STYLE_INTERCEPTOR))
                       .credentialsProvider(CREDENTIALS_PROVIDER);

    }

    private S3AsyncClientBuilder getS3AsyncClientBuilder() {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .overrideConfiguration(c -> c.addExecutionInterceptor(CAPTURING_INTERCEPTOR)
                                                         .addExecutionInterceptor(PATH_STYLE_INTERCEPTOR))
                            .credentialsProvider(CREDENTIALS_PROVIDER);
    }

    /**
     * S3Express does not support path style enforcement through client configuration and the endpoint will resolve
     * to virtual style. However, path style is required for the HTTP client to be able to direct requests to localhost
     * and the WireMock port.
     */
    private static final class PathStyleEnforcingInterceptor implements ExecutionInterceptor {

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            SdkHttpRequest sdkHttpRequest = context.httpRequest();
            String host = sdkHttpRequest.host();
            String bucket = host.substring(0, host.indexOf(".localhost"));

            return sdkHttpRequest.toBuilder().host("localhost")
                                 .encodedPath(SdkHttpUtils.appendUri(bucket, sdkHttpRequest.encodedPath()))
                                 .build();
        }
    }

    private static final class CapturingInterceptor implements ExecutionInterceptor {
        private Map<String, List<String>> headers;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SdkHttpRequest sdkHttpRequest = context.httpRequest();
            this.headers = sdkHttpRequest.headers();
            log.debug(() -> String.format("%s %s%n", sdkHttpRequest.method(), sdkHttpRequest.encodedPath()));
            headers.forEach((k, strings) -> log.debug(() -> String.format("%s, %s%n", k, strings)));
        }
    }
}