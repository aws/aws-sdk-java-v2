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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.rules.testing.BaseRuleSetClientTest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.Protocol;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@WireMockTest(httpsEnabled = true)
public class S3ExpressCreateSessionTest extends BaseRuleSetClientTest {
    private static final Logger log = Logger.loggerFor(S3ExpressCreateSessionTest.class);

    private static final Function<WireMockRuntimeInfo, URI> WM_HTTP_ENDPOINT = wm -> URI.create(wm.getHttpBaseUrl());
    private static final Function<WireMockRuntimeInfo, URI> WM_HTTPS_ENDPOINT = wm -> URI.create(wm.getHttpsBaseUrl());
    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    private static final PathStyleEnforcingInterceptor PATH_STYLE_INTERCEPTOR = new PathStyleEnforcingInterceptor();
    private static final CapturingInterceptor CAPTURING_INTERCEPTOR = new CapturingInterceptor();
    private static final String DEFAULT_BUCKET = "s3expressformat--use1-az1--x-s3";
    private static final String DEFAULT_KEY = "foo.txt";
    private static final String GET_BODY = "Hello world!";
    private static final int DEFAULT_API_CALL_TIMEOUT_VALUE_MILLIS = 10000;

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
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withBody(GET_BODY))
                             .withName("OuterGetCall"));
    }

    @Test
    public void when_clientDefaultIsUsed_andOkResponse_callIsSuccessful(WireMockRuntimeInfo wm) {
        stubFor(get(urlMatching("/.*session")).willReturn(aResponse().withStatus(200)
                                                                     .withBody(CREATE_SESSION_RESPONSE)));
        createClientAndCallGetObject(null, ClientType.SYNC, wm);
    }

    @Test
    public void when_clientDefaultIsUsed_andResponseIsDelayed_timeoutExceptionIsPropagated(WireMockRuntimeInfo wm) {
        Integer delayResponseTimeInMillis = 10000;
        stubFor(get(urlMatching("/.*session")).willReturn(aResponse().withStatus(200)
                                                                     .withBody(CREATE_SESSION_RESPONSE)
                                                                     .withFixedDelay(delayResponseTimeInMillis)));
        assertThatThrownBy(() -> createClientAndCallGetObject(null, ClientType.SYNC, wm))
            .isInstanceOf(ApiCallTimeoutException.class)
            .hasMessageContaining(String.valueOf(DEFAULT_API_CALL_TIMEOUT_VALUE_MILLIS));
    }

    @Test
    public void when_asyncClientDefaultIsUsed_andResponseIsDelayed_timeoutExceptionIsPropagated(WireMockRuntimeInfo wm) {
        Integer delayResponseTimeInMillis = 10000;
        stubFor(get(urlMatching("/.*session")).willReturn(aResponse().withStatus(200)
                                                                     .withBody(CREATE_SESSION_RESPONSE)
                                                                     .withFixedDelay(delayResponseTimeInMillis)));
        assertThatThrownBy(() -> createClientAndCallGetObject(null, ClientType.ASYNC, wm))
            .isInstanceOf(CompletionException.class)
            .hasMessageContaining(String.valueOf(DEFAULT_API_CALL_TIMEOUT_VALUE_MILLIS))
            .hasCauseInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void when_clientDefaultIsUsed_andResponseHasRetryableError_exceptionIsPropagated(WireMockRuntimeInfo wm) {
        stubFor(get(urlMatching("/.*session")).willReturn(aResponse().withStatus(500).withBody("<xml></xml>")));
        try {
            createClientAndCallGetObject(null, ClientType.SYNC, wm);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(S3Exception.class);
            assertThat(e.getSuppressed()).anySatisfy(throwable -> assertThat(throwable).isInstanceOf(SdkClientException.class));
        }
    }

    @Test
    public void when_asyncClientDefaultIsUsed_andResponseHasRetryableError_exceptionIsPropagated(WireMockRuntimeInfo wm) {
        stubFor(get(urlMatching("/.*session")).willReturn(aResponse().withStatus(500).withBody("<xml></xml>")));
        try {
            createClientAndCallGetObject(null, ClientType.ASYNC, wm);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(CompletionException.class);
            Throwable cause = e.getCause();
            assertThat(cause).isInstanceOf(S3Exception.class);
            assertThat(cause.getSuppressed()).anySatisfy(throwable -> assertThat(throwable).isInstanceOf(SdkClientException.class));
        }
    }

    @Test
    public void when_asyncClientDefaultIsUsed_andResponseHasRetryableErrorWithDelays_timeoutExceptionIsPropagated(WireMockRuntimeInfo wm) {
        stubForaResponseWithDelayedRetryableException();
        assertThatThrownBy(() -> createClientAndCallGetObject(null, ClientType.ASYNC, wm))
            .isInstanceOf(CompletionException.class)
            .hasMessageContaining(String.valueOf(DEFAULT_API_CALL_TIMEOUT_VALUE_MILLIS))
            .hasCauseInstanceOf(ApiCallTimeoutException.class);
    }

    private static Stream<Arguments> apiCallTimeoutValues() {
        return Stream.of(
            Arguments.of(1000L),
            Arguments.of(5000L)
        );
    }

    @ParameterizedTest
    @MethodSource("apiCallTimeoutValues")
    public void when_clientApiCallTimeoutConfigured_andOkResponse_callIsSuccessful(Long apiCallTimeoutValue,
                                                                                   WireMockRuntimeInfo wm) {
        stubFor(get(urlMatching("/.*session")).atPriority(1).willReturn(aResponse()
                                                                            .withStatus(200)
                                                                            .withBody(CREATE_SESSION_RESPONSE)));
        createClientAndCallGetObject(apiCallTimeoutValue, ClientType.SYNC, wm);
    }

    @ParameterizedTest
    @MethodSource("apiCallTimeoutValues")
    public void when_clientApiCallTimeoutConfigured_andResponseIsDelayed_timeoutExceptionIsPropagated(Long apiCallTimeoutValue,
                                                                                                      WireMockRuntimeInfo wm) {
        Integer delayResponseTimeInMillis = apiCallTimeoutValue.intValue() + 500;
        stubFor(get(urlMatching("/.*session")).atPriority(1).willReturn(aResponse()
                                                                            .withStatus(200)
                                                                            .withBody(CREATE_SESSION_RESPONSE)
                                                                            .withFixedDelay(delayResponseTimeInMillis)));
        assertThatThrownBy(() -> createClientAndCallGetObject(apiCallTimeoutValue, ClientType.SYNC, wm))
            .isInstanceOf(ApiCallTimeoutException.class)
            .hasMessageContaining(String.valueOf(apiCallTimeoutValue));
    }

    @ParameterizedTest
    @MethodSource("apiCallTimeoutValues")
    public void when_clientApiCallTimeoutConfigured_andResponseHasRetryableError_exceptionIsPropagated(Long apiCallTimeoutValue,
                                                                                                       WireMockRuntimeInfo wm) {
        stubFor(get(urlMatching("/.*session")).atPriority(1).willReturn(aResponse().withStatus(500).withBody("<xml></xml>")));
        try {
            createClientAndCallGetObject(apiCallTimeoutValue, ClientType.SYNC, wm);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(S3Exception.class);
            assertThat(e.getSuppressed()).anySatisfy(throwable -> assertThat(throwable).isInstanceOf(SdkClientException.class));
        }
    }

    @ParameterizedTest
    @MethodSource("apiCallTimeoutValues")
    public void when_asyncClientApiCallTimeoutConfigured_andResponseHasRetryableError_exceptionIsPropagated(Long apiCallTimeoutValue,
                                                                                                            WireMockRuntimeInfo wm) {
        stubFor(get(urlMatching("/.*session")).atPriority(1).willReturn(aResponse().withStatus(500).withBody("<xml></xml>")));
        try {
            createClientAndCallGetObject(apiCallTimeoutValue, ClientType.ASYNC, wm);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(CompletionException.class);
            Throwable cause = e.getCause();
            assertThat(cause).isInstanceOf(S3Exception.class);
            assertThat(cause.getSuppressed()).anySatisfy(throwable -> assertThat(throwable).isInstanceOf(SdkClientException.class));
        }
    }

    private void createClientAndCallGetObject(Long apiCallTimeoutValue, ClientType clientType,
                                              WireMockRuntimeInfo wm) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY).build();
        ClientOverrideConfiguration.Builder overrideConfiguration =
            ClientOverrideConfiguration.builder()
                                       .addExecutionInterceptor(CAPTURING_INTERCEPTOR)
                                       .addExecutionInterceptor(PATH_STYLE_INTERCEPTOR);
        if (apiCallTimeoutValue != null) {
            overrideConfiguration.apiCallTimeout(Duration.ofMillis(apiCallTimeoutValue));
        }
        if (clientType == ClientType.SYNC) {
            S3Client s3Client = s3Client(overrideConfiguration.build(), wm);
            s3Client.getObject(getObjectRequest);
        } else {
            S3AsyncClient s3Client = s3AsyncClient(overrideConfiguration.build(), wm);
            s3Client.getObject(getObjectRequest, AsyncResponseTransformer.toBytes()).join();
        }
    }

    private enum ClientType {
        SYNC,
        ASYNC
    }

    private void stubForaResponseWithDelayedRetryableException() {
        ResponseDefinitionBuilder errorResponse = aResponse().withStatus(500).withBody("<xml></xml>");
        stubFor(get(urlMatching("/.*session")).atPriority(1)
                                              .inScenario("retriesWithDelay")
                                              .willSetStateTo("second")
                                              .whenScenarioStateIs(Scenario.STARTED)
                                              .willReturn(errorResponse.withFixedDelay(5500)));
        stubFor(get(urlMatching("/.*session")).atPriority(1)
                                              .inScenario("retriesWithDelay")
                                              .whenScenarioStateIs("second")
                                              .willSetStateTo("third")
                                              .willReturn(errorResponse.withFixedDelay(5000)));
        stubFor(get(urlMatching("/.*session")).atPriority(1)
                                              .inScenario("retriesWithDelay")
                                              .whenScenarioStateIs("third")
                                              .willSetStateTo("finish")
                                              .willReturn(errorResponse.withFixedDelay(5000)));
    }

    private S3Client s3Client(ClientOverrideConfiguration overrideConfiguration, WireMockRuntimeInfo wm) {
        S3ClientBuilder syncClientBuilder = S3Client.builder()
                                                    .region(Region.US_EAST_1)
                                                    .overrideConfiguration(overrideConfiguration)
                                                    .credentialsProvider(CREDENTIALS_PROVIDER);
        setEndpointParametersSync(syncClientBuilder, Protocol.HTTPS, wm);
        return syncClientBuilder.build();
    }

    private S3AsyncClient s3AsyncClient(ClientOverrideConfiguration overrideConfiguration, WireMockRuntimeInfo wm) {
        S3AsyncClientBuilder asyncClientBuilder = S3AsyncClient.builder()
                                                               .region(Region.US_EAST_1)
                                                               .overrideConfiguration(overrideConfiguration)
                                                               .credentialsProvider(CREDENTIALS_PROVIDER);
        setEndpointParametersAsync(asyncClientBuilder, Protocol.HTTPS, wm);
        return asyncClientBuilder.build();
    }

    private void setEndpointParametersAsync(S3AsyncClientBuilder clientBuilder, Protocol protocol, WireMockRuntimeInfo wm) {
        if (protocol == Protocol.HTTP) {
            clientBuilder.endpointOverride(WM_HTTP_ENDPOINT.apply(wm));
        } else {
            clientBuilder.endpointOverride(WM_HTTPS_ENDPOINT.apply(wm))
                         .httpClient(NettyNioAsyncHttpClient.builder()
                                                            .buildWithDefaults(AttributeMap.builder()
                                                                                           .put(TRUST_ALL_CERTIFICATES, true).build()));
        }
    }

    private void setEndpointParametersSync(S3ClientBuilder clientBuilder, Protocol protocol, WireMockRuntimeInfo wm) {
        if (protocol == Protocol.HTTP) {
            clientBuilder.endpointOverride(WM_HTTP_ENDPOINT.apply(wm));
        } else {
            clientBuilder.endpointOverride(WM_HTTPS_ENDPOINT.apply(wm))
                         .httpClient(ApacheHttpClient.builder()
                                                     .buildWithDefaults(AttributeMap.builder()
                                                                                    .put(TRUST_ALL_CERTIFICATES, TRUE)
                                                                                    .build()));
        }
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
