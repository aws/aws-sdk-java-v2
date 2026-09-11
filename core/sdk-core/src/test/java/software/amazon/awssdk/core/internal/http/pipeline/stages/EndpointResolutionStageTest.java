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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.metrics.MetricCollector;

class EndpointResolutionStageTest {

    private static final URI CLIENT_ENDPOINT = URI.create("https://myservice.us-east-1.amazonaws.com");
    private static final URI RESOLVED_ENDPOINT = URI.create("https://resolved.us-west-2.amazonaws.com");

    private EndpointResolutionStage stage;
    private ExecutionAttributes executionAttributes;
    private SdkRequest sdkRequest;

    @BeforeEach
    void setup() {
        stage = new EndpointResolutionStage(null);
        sdkRequest = mock(SdkRequest.class);
        executionAttributes = new ExecutionAttributes();
    }

    @Test
    void execute_noResolver_returnsRequestUnchanged() throws Exception {
        SdkHttpFullRequest.Builder request = defaultRequest();
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result).isSameAs(request);
    }

    @Test
    void execute_discoveredEndpoint_returnsRequestUnchanged() throws Exception {
        SdkHttpFullRequest.Builder request = defaultRequest();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER,
                                         (req, attrs) -> Endpoint.builder().url(RESOLVED_ENDPOINT).build());
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result).isSameAs(request);
    }

    @Test
    void execute_happyPath_appliesResolvedUrl() throws Exception {
        SdkHttpFullRequest.Builder request = defaultRequest();
        Endpoint endpoint = Endpoint.builder().url(RESOLVED_ENDPOINT).build();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result.host()).isEqualTo("resolved.us-west-2.amazonaws.com");
        assertThat(result.protocol()).isEqualTo("https");
    }

    @Test
    void execute_happyPath_storesResolvedEndpoint() throws Exception {
        SdkHttpFullRequest.Builder request = defaultRequest();
        Endpoint endpoint = Endpoint.builder().url(RESOLVED_ENDPOINT).build();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));
        RequestExecutionContext context = createContext();

        stage.execute(request, context);

        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT)).isSameAs(endpoint);
    }

    @Test
    void execute_withHeaders_appendsToRequest() throws Exception {
        Endpoint endpoint = Endpoint.builder().url(RESOLVED_ENDPOINT)
                                    .putHeader("x-amz-custom", "val1")
                                    .putHeader("x-amz-custom", "val2")
                                    .build();

        SdkHttpFullRequest.Builder request = defaultRequest();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result.build().matchingHeaders("x-amz-custom")).containsExactly("val1", "val2");
    }

    @Test
    void execute_withMetricCollector_reportsEndpointResolveDuration() throws Exception {
        Endpoint endpoint = Endpoint.builder().url(RESOLVED_ENDPOINT).build();
        MetricCollector metricCollector = mock(MetricCollector.class);

        SdkHttpFullRequest.Builder request = defaultRequest();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));
        executionAttributes.putAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR, metricCollector);
        RequestExecutionContext context = createContext();

        stage.execute(request, context);

        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(metricCollector).reportMetric(org.mockito.ArgumentMatchers.eq(CoreMetric.ENDPOINT_RESOLVE_DURATION),
                                             durationCaptor.capture());
        assertThat(durationCaptor.getValue()).isGreaterThanOrEqualTo(Duration.ZERO);
    }

    @Test
    void execute_resolvedPathDiffersFromClientPath_combinesPaths() throws Exception {
        URI clientEndpoint = URI.create("https://myservice.amazonaws.com");
        URI resolvedUri = URI.create("https://resolved.amazonaws.com/v2");

        Endpoint endpoint = Endpoint.builder().url(resolvedUri).build();
        SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder()
                                                                .method(SdkHttpMethod.GET)
                                                                .protocol("https")
                                                                .host("myservice.amazonaws.com")
                                                                .encodedPath("/my/operation");
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(clientEndpoint));
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result.encodedPath()).isEqualTo("/v2/my/operation");
    }

    @Test
    void execute_resolverReceivesRequestFromInterceptorContext() throws Exception {
        SdkRequest modifiedRequest = mock(SdkRequest.class);
        SdkRequest[] capturedRequest = new SdkRequest[1];

        Endpoint endpoint = Endpoint.builder().url(RESOLVED_ENDPOINT).build();
        SdkHttpFullRequest.Builder request = defaultRequest();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> {
            capturedRequest[0] = req;
            return endpoint;
        });
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));

        // Use modifiedRequest in interceptor context (different from sdkRequest)
        RequestExecutionContext context = createContext(modifiedRequest);

        stage.execute(request, context);

        assertThat(capturedRequest[0]).isSameAs(modifiedRequest);
    }

    @Test
    void execute_interceptorModifiedHost_preservesCustomHostAndScheme() throws Exception {
        Endpoint endpoint = Endpoint.builder().url(URI.create("https://resolved.amazonaws.com")).build();
        SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder()
                                                                .method(SdkHttpMethod.GET)
                                                                .protocol("http")
                                                                .host("custom.example.com")
                                                                .port(8080)
                                                                .encodedPath("/my-operation");
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY,
                                         preModifyRequest("https", "myservice.amazonaws.com", null, "/my-operation"));
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result.host()).isEqualTo("custom.example.com");
        assertThat(result.protocol()).isEqualTo("http");
        assertThat(result.port()).isEqualTo(8080);
    }

    @Test
    void execute_interceptorModifiedHost_stillAppliesResolvedPath() throws Exception {
        URI clientEndpoint = URI.create("https://s3.us-west-2.amazonaws.com");
        URI resolvedUri = URI.create("https://s3.us-west-2.amazonaws.com/my-bucket");

        Endpoint endpoint = Endpoint.builder().url(resolvedUri).build();
        SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder()
                                                                .method(SdkHttpMethod.GET)
                                                                .protocol("https")
                                                                .host("example.com")
                                                                .encodedPath("/my-key");

        executionAttributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY,
                                         preModifyRequest("https", "s3.us-west-2.amazonaws.com", null, "/my-key"));
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(clientEndpoint));
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result.host()).isEqualTo("example.com");
        assertThat(result.encodedPath()).isEqualTo("/my-bucket/my-key");
    }

    @Test
    void execute_explicitStandardPortUnmodified_appliesResolvedUrl() throws Exception {
        // The snapshot always carries a resolved port, so an endpoint that explicitly specifies the protocol's
        // standard port must not be mistaken for one an interceptor modified.
        SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder()
                                                              .method(SdkHttpMethod.GET)
                                                              .protocol("https")
                                                              .host(CLIENT_ENDPOINT.getHost())
                                                              .port(443)
                                                              .encodedPath("/");
        Endpoint endpoint = Endpoint.builder().url(RESOLVED_ENDPOINT).build();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY,
                                         preModifyRequest("https", CLIENT_ENDPOINT.getHost(), null, "/"));
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result.host()).isEqualTo("resolved.us-west-2.amazonaws.com");
    }

    @Test
    void execute_interceptorModifiedPortOnly_preservesCustomPort() throws Exception {
        SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder()
                                                              .method(SdkHttpMethod.GET)
                                                              .protocol("https")
                                                              .host(CLIENT_ENDPOINT.getHost())
                                                              .port(8443)
                                                              .encodedPath("/");
        Endpoint endpoint = Endpoint.builder().url(RESOLVED_ENDPOINT).build();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY,
                                         preModifyRequest("https", CLIENT_ENDPOINT.getHost(), null, "/"));
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result.host()).isEqualTo(CLIENT_ENDPOINT.getHost());
        assertThat(result.port()).isEqualTo(8443);
    }

    @Test
    void execute_neverBuildsUriFromSnapshottedRequest() throws Exception {
        // Guards the query/ec2 protocol performance regression: for those protocols the snapshotted request still
        // holds the entire payload in its raw query parameters, so getUri() would encode and re-parse the whole
        // payload on every API call. The stage must only read the endpoint components.
        SdkHttpRequest snapshot = mock(SdkHttpRequest.class);
        when(snapshot.protocol()).thenReturn("https");
        when(snapshot.host()).thenReturn(CLIENT_ENDPOINT.getHost());
        when(snapshot.port()).thenReturn(443);
        Endpoint endpoint = Endpoint.builder().url(RESOLVED_ENDPOINT).build();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY, snapshot);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));
        RequestExecutionContext context = createContext();

        stage.execute(defaultRequest(), context);

        verify(snapshot, never()).getUri();
    }

    private static SdkHttpRequest preModifyRequest(String protocol, String host, Integer port, String encodedPath) {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.GET)
                                 .protocol(protocol)
                                 .host(host)
                                 .port(port)
                                 .encodedPath(encodedPath)
                                 .build();
    }

    private SdkHttpFullRequest.Builder defaultRequest() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.GET)
                                 .protocol("https")
                                 .host(CLIENT_ENDPOINT.getHost())
                                 .encodedPath("/");
    }

    private RequestExecutionContext createContext() {
        return createContext(sdkRequest);
    }

    private RequestExecutionContext createContext(SdkRequest request) {
        InterceptorContext interceptorContext = InterceptorContext.builder()
                                                                 .request(request)
                                                                 .build();
        ExecutionContext executionContext = ExecutionContext.builder()
                                                          .interceptorContext(interceptorContext)
                                                          .executionAttributes(executionAttributes)
                                                          .build();
        return RequestExecutionContext.builder()
                                     .executionContext(executionContext)
                                     .originalRequest(request)
                                     .build();
    }

    @Test
    void execute_endpointAlreadyResolved_skipsResolution() throws Exception {
        Endpoint preResolved = Endpoint.builder().url(URI.create("https://pre-resolved.example.com")).build();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, preResolved);

        SdkHttpFullRequest.Builder request = defaultRequest();
        SdkHttpFullRequest.Builder result = stage.execute(request, createContext());


        assertThat(result).isSameAs(request);
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT)).isSameAs(preResolved);
    }

    @Test
    void execute_resolvedEndpointWithPort_appliesPort() throws Exception {
        Endpoint endpoint = Endpoint.builder().url(URI.create("https://resolved.amazonaws.com:8443/v2")).build();
        SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder()
                                                                .method(SdkHttpMethod.GET)
                                                                .protocol("https")
                                                                .host("myservice.amazonaws.com")
                                                                .encodedPath("/operation");
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(CLIENT_ENDPOINT));
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result.host()).isEqualTo("resolved.amazonaws.com");
        assertThat(result.port()).isEqualTo(8443);
        assertThat(result.protocol()).isEqualTo("https");
    }

    @Test
    void execute_resolvedPathWithEncodedCharacters_preservesEncoding() throws Exception {
        URI clientEndpoint = URI.create("https://s3.amazonaws.com");
        Endpoint endpoint = Endpoint.builder()
                                    .url(URI.create("https://s3.amazonaws.com/bucket%20name"))
                                    .build();
        SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder()
                                                                .method(SdkHttpMethod.GET)
                                                                .protocol("https")
                                                                .host("s3.amazonaws.com")
                                                                .encodedPath("/key%2Fwith%2Fslashes");
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (req, attrs) -> endpoint);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(clientEndpoint));
        RequestExecutionContext context = createContext();

        SdkHttpFullRequest.Builder result = stage.execute(request, context);

        assertThat(result.encodedPath()).isEqualTo("/bucket%20name/key%2Fwith%2Fslashes");
    }
}
