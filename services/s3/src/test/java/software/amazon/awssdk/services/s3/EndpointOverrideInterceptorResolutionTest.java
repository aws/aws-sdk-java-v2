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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * End-to-end coverage of how {@code endpointOverride} interacts with a customer {@link ExecutionInterceptor} that
 * modifies the HTTP request in {@code modifyHttpRequest}.
 *
 * <p>S3 is used because its endpoint rules <em>rewrite</em> the host taken
 * from an {@code endpointOverride}: virtual-host addressing resolves {@code {Bucket}.{url#authority}}. That makes the
 * assertions here able to distinguish "the rules-engine host was applied" from "the interceptor's host was preserved",
 * which a service whose rules pass the override through verbatim could not. Additionally, S3 endpoint rules
 * modify the path which we also need to test with.
 *
 * <p>The contract being pinned down, as implemented by {@code EndpointResolutionStage}:
 * <ul>
 *   <li>If no interceptor changed the endpoint, the resolved scheme, host and port are applied.</li>
 *   <li>If an interceptor changed the host, scheme or port, that change wins and the resolved scheme/host/port are
 *       <em>not</em> applied — but the resolved path still is.</li>
 *   <li>An override that spells out the protocol's default port is not a change.</li>
 * </ul>
 */
class EndpointOverrideInterceptorResolutionTest {

    private static final String BUCKET = "bucketname";

    // The rules engine rewrites the override's authority to "<bucket>.<authority>" for virtual-host addressing.
    private static final URI OVERRIDE = URI.create("https://beta.example.com");
    private static final String RESOLVED_HOST = BUCKET + ".beta.example.com";

    @Test
    void noInterceptor_appliesResolvedHost() {
        SdkHttpRequest wireRequest = sync(OVERRIDE, null);

        assertThat(wireRequest.host()).isEqualTo(RESOLVED_HOST);
        assertThat(wireRequest.protocol()).isEqualTo("https");
        assertThat(wireRequest.getUri()).isEqualTo(URI.create("https://" + RESOLVED_HOST));
    }

    @Test
    void noInterceptor_overrideSpellsOutDefaultPort_stillAppliesResolvedHost() {
        SdkHttpRequest wireRequest = sync(URI.create("https://beta.example.com:443"), null);

        assertThat(wireRequest.host()).isEqualTo(RESOLVED_HOST);
        assertThat(wireRequest.port()).isEqualTo(443);
    }

    @Test
    void noInterceptor_overrideSpellsOutDefaultHttpPort_stillAppliesResolvedHost() {
        SdkHttpRequest wireRequest = sync(URI.create("http://beta.example.com:80"), null);

        assertThat(wireRequest.host()).isEqualTo(RESOLVED_HOST);
        assertThat(wireRequest.port()).isEqualTo(80);
    }

    @Test
    void noInterceptor_overrideWithNonDefaultPort_appliesResolvedHostAndKeepsPort() {
        SdkHttpRequest wireRequest = sync(URI.create("https://beta.example.com:8443"), null);

        assertThat(wireRequest.host()).isEqualTo(RESOLVED_HOST);
        assertThat(wireRequest.port()).isEqualTo(8443);
    }

    @Test
    void interceptorReturnsRequestUnchanged_appliesResolvedHost() {
        SdkHttpRequest wireRequest = sync(OVERRIDE, r -> r);

        assertThat(wireRequest.host()).isEqualTo(RESOLVED_HOST);
    }

    @Test
    void interceptorAddsHeaderOnly_appliesResolvedHost() {
        SdkHttpRequest wireRequest = sync(OVERRIDE, r -> r.toBuilder().putHeader("x-custom", "value").build());

        assertThat(wireRequest.host()).isEqualTo(RESOLVED_HOST);
        assertThat(wireRequest.firstMatchingHeader("x-custom")).hasValue("value");
    }

    @Test
    void interceptorRestatesTheSameEndpoint_appliesResolvedHost() {
        SdkHttpRequest wireRequest = sync(OVERRIDE, r -> r.toBuilder()
                                                          .protocol(r.protocol())
                                                          .host(r.host())
                                                          .port(r.port())
                                                          .build());

        assertThat(wireRequest.host()).isEqualTo(RESOLVED_HOST);
    }

    @Test
    void interceptorChangesHost_hostWinsOverResolvedEndpoint() {
        SdkHttpRequest wireRequest = sync(OVERRIDE, r -> r.toBuilder().host("interceptor.example.com").build());

        assertThat(wireRequest.host()).isEqualTo("interceptor.example.com");
        assertThat(wireRequest.protocol()).isEqualTo("https");
    }

    @Test
    void interceptorChangesScheme_schemeWinsOverResolvedEndpoint() {
        SdkHttpRequest wireRequest = sync(OVERRIDE, r -> r.toBuilder().protocol("http").port(80).build());

        assertThat(wireRequest.protocol()).isEqualTo("http");
        assertThat(wireRequest.host()).isEqualTo("beta.example.com");
    }

    @Test
    void interceptorChangesPort_portWinsOverResolvedEndpoint() {
        SdkHttpRequest wireRequest = sync(OVERRIDE, r -> r.toBuilder().port(9999).build());

        assertThat(wireRequest.port()).isEqualTo(9999);
        assertThat(wireRequest.host()).isEqualTo("beta.example.com");
    }

    @Test
    void interceptorChangesHost_resolvedPathIsStillApplied() {
        // The override carries a path, so the rules engine prefixes it. That path must survive even though the
        // interceptor's host suppresses the resolved host.
        SdkHttpRequest wireRequest = sync(URI.create("https://beta.example.com/path"),
                                          r -> r.toBuilder().host("interceptor.example.com").build());

        assertThat(wireRequest.host()).isEqualTo("interceptor.example.com");
        assertThat(wireRequest.encodedPath()).startsWith("/path");
    }

    @Test
    void async_overrideSpellsOutDefaultPort_stillAppliesResolvedHost() {
        SdkHttpRequest wireRequest = async(URI.create("https://beta.example.com:443"), null);

        assertThat(wireRequest.host()).isEqualTo(RESOLVED_HOST);
    }

    @Test
    void async_interceptorChangesHost_hostWinsOverResolvedEndpoint() {
        SdkHttpRequest wireRequest = async(OVERRIDE, r -> r.toBuilder().host("interceptor.example.com").build());

        assertThat(wireRequest.host()).isEqualTo("interceptor.example.com");
    }

    // ---------------------------------------------------------------------------------------------------------

    /** Runs a listObjects against a mocked HTTP client and returns the request that reached the wire. */
    private SdkHttpRequest sync(URI endpointOverride, UnaryOperator<SdkHttpRequest> modifyHttpRequest) {
        try (MockSyncHttpClient httpClient = new MockSyncHttpClient()) {
            httpClient.stubNextResponse(listObjectsResponse());
            S3Client client = S3Client.builder()
                                      .region(Region.US_WEST_2)
                                      .credentialsProvider(credentials())
                                      .endpointOverride(endpointOverride)
                                      .httpClient(httpClient)
                                      .overrideConfiguration(c -> c.addExecutionInterceptor(
                                          new EndpointModifyingInterceptor(modifyHttpRequest)))
                                      .build();

            client.listObjects(r -> r.bucket(BUCKET));
            return httpClient.getLastRequest();
        }
    }

    private SdkHttpRequest async(URI endpointOverride, UnaryOperator<SdkHttpRequest> modifyHttpRequest) {
        try (MockAsyncHttpClient httpClient = new MockAsyncHttpClient()) {
            httpClient.stubNextResponse(listObjectsResponse());
            S3AsyncClient client = S3AsyncClient.builder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(credentials())
                                                .endpointOverride(endpointOverride)
                                                .httpClient(httpClient)
                                                .overrideConfiguration(c -> c.addExecutionInterceptor(
                                                    new EndpointModifyingInterceptor(modifyHttpRequest)))
                                                .build();

            client.listObjects(r -> r.bucket(BUCKET)).join();
            return httpClient.getLastRequest();
        }
    }

    private static HttpExecuteResponse listObjectsResponse() {
        try {
            return S3MockUtils.mockListObjectsResponse();
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }

    /**
     * A customer-style interceptor. Also asserts that the pre-modify endpoint snapshot is visible here and describes
     * the request as it was marshalled, before endpoint resolution ran.
     */
    private static final class EndpointModifyingInterceptor implements ExecutionInterceptor {
        private final UnaryOperator<SdkHttpRequest> modifyHttpRequest;

        private EndpointModifyingInterceptor(UnaryOperator<SdkHttpRequest> modifyHttpRequest) {
            this.modifyHttpRequest = modifyHttpRequest;
        }

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes attrs) {
            assertThat(attrs.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY))
                .as("the pre-modify endpoint snapshot must be readable from modifyHttpRequest")
                .isNotNull()
                .satisfies(endpoint -> assertThat(endpoint.host()).isEqualTo(context.httpRequest().host()));

            return modifyHttpRequest == null ? context.httpRequest()
                                             : modifyHttpRequest.apply(context.httpRequest());
        }
    }
}
