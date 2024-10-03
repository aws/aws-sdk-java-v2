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

package software.amazon.awssdk.services.s3.internal.s3express;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@WireMockTest(httpsEnabled = true)
public class S3ExpressRetryResolveCredentialsTest {

    private static final Function<WireMockRuntimeInfo, URI> WM_HTTPS_ENDPOINT = wm -> URI.create(wm.getHttpsBaseUrl());
    private static final PathStyleEnforcingInterceptor PATH_STYLE_INTERCEPTOR = new PathStyleEnforcingInterceptor();
    private static final String S3EXPRESS_BUCKET = "s3express-cache-1--use1-az1--x-s3";
    private static final String REGULAR_S3__BUCKET = "my-test-bucket";
    private static final int RETRYABLE_ERROR_STATUS_CODE = 429;
    private static final int NON_RETRYABLE_ERROR_STATUS_CODE = 400;

    private S3Client s3;
    private S3AsyncClient s3Async;
    private TrackingCredentialsProvider trackingCredentialsProvider;

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
    public void methodSetup(WireMockRuntimeInfo wm) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("akid_client", "skid_client");
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        trackingCredentialsProvider = new TrackingCredentialsProvider(credentialsProvider);
        s3 = getS3ClientBuilder(wm).build();
        s3Async = getS3AsyncClientBuilder(wm).build();

        stubFor(get(urlMatching("/.*session")).atPriority(1).willReturn(aResponse()
                                                                            .withStatus(200)
                                                                            .withBody(CREATE_SESSION_RESPONSE)));
    }

    private static List<Arguments> testParams() {
        return Arrays.asList(
            Arguments.of(S3EXPRESS_BUCKET, RETRYABLE_ERROR_STATUS_CODE, 4), // + 3 retries
            Arguments.of(S3EXPRESS_BUCKET, NON_RETRYABLE_ERROR_STATUS_CODE, 1),
            Arguments.of(REGULAR_S3__BUCKET, RETRYABLE_ERROR_STATUS_CODE, 1),
            Arguments.of(REGULAR_S3__BUCKET, NON_RETRYABLE_ERROR_STATUS_CODE, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("testParams")
    void syncClient_resolvesIdentityProperNumberOfTimes(String bucket, int statusCode, int resolveIdentityCount) {
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(statusCode)));
        try {
            s3.putObject(r -> r.bucket(bucket).key("key"), RequestBody.fromString("tmp"));
        } catch (Exception e) {
            assertThat(trackingCredentialsProvider.resolveIdentityCount()).isEqualTo(resolveIdentityCount);
        }
    }

    @ParameterizedTest
    @MethodSource("testParams")
    void asyncClient_resolvesIdentityProperNumberOfTimes(String bucket, int statusCode, int resolveIdentityCount) {
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(statusCode)));
        try {
            s3Async.putObject(r -> r.bucket(bucket).key("key"), AsyncRequestBody.fromString("tmp")).join();
        } catch (Exception e) {
            assertThat(trackingCredentialsProvider.resolveIdentityCount()).isEqualTo(resolveIdentityCount);
        }
    }

    private S3ClientBuilder getS3ClientBuilder(WireMockRuntimeInfo wm) {
        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .overrideConfiguration(c -> c.addExecutionInterceptor(PATH_STYLE_INTERCEPTOR))
                       .credentialsProvider(trackingCredentialsProvider)
                       .endpointOverride(WM_HTTPS_ENDPOINT.apply(wm))
                       .httpClient(ApacheHttpClient.builder()
                                                   .buildWithDefaults(AttributeMap.builder()
                                                                                  .put(TRUST_ALL_CERTIFICATES, TRUE)
                                                                                  .build()));
    }

    private S3AsyncClientBuilder getS3AsyncClientBuilder(WireMockRuntimeInfo wm) {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .overrideConfiguration(c -> c.addExecutionInterceptor(PATH_STYLE_INTERCEPTOR))
                            .credentialsProvider(trackingCredentialsProvider)
                            .endpointOverride(WM_HTTPS_ENDPOINT.apply(wm))
                            .httpClient(NettyNioAsyncHttpClient.builder()
                                                               .buildWithDefaults(AttributeMap.builder()
                                                                                              .put(TRUST_ALL_CERTIFICATES, TRUE)
                                                                                              .build()));
    }

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

    private static final class TrackingCredentialsProvider implements AwsCredentialsProvider {
        private final AwsCredentialsProvider delegate;
        private int resolveIdentityCount;

        TrackingCredentialsProvider(AwsCredentialsProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return delegate.resolveCredentials();
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest resolveIdentityRequest) {
            resolveIdentityCount++;
            return delegate.resolveIdentity(resolveIdentityRequest);
        }

        public int resolveIdentityCount() {
            return resolveIdentityCount;
        }
    }
}
