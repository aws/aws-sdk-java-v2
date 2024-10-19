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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.internal.useragent.businessmetrics.BusinessMetrics.METRIC_SEARCH_PATTERN;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.useragent.businessmetrics.BusinessMetricFeatureId;
import software.amazon.awssdk.core.internal.useragent.businessmetrics.BusinessMetrics;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.protocolrestjson.paginators.PaginatedOperationWithResultKeyPublisher;
import software.amazon.awssdk.services.restjsonwithwaiters.RestJsonWithWaitersAsyncClient;
import software.amazon.awssdk.services.restjsonwithwaiters.model.AllTypesRequest;
import software.amazon.awssdk.services.restjsonwithwaiters.model.AllTypesResponse;
import software.amazon.awssdk.services.restjsonwithwaiters.waiters.RestJsonWithWaitersAsyncWaiter;

class BusinessMetricsUserAgentTest {
    private CapturingInterceptor interceptor;

    private static final String USER_AGENT_HEADER_NAME = "User-Agent";
    private static final StaticCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    @BeforeEach
    public void setup() {
        this.interceptor = new CapturingInterceptor();
    }

    @AfterEach
    public void cleanup() {

    }

    private static Stream<Arguments> inputValues() {
        return Stream.of(
            Arguments.of("Default values contain at least a retry metric", Arrays.asList("D"))
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("inputValues")
    void validate_metricsString_forDifferentConfigValues(String description, List<String> expectedMetrics) {
        ProtocolRestJsonAsyncClientBuilder clientBuilder = asyncClientBuilder();

        assertThatThrownBy(() -> clientBuilder.build().allTypes().join()).hasMessageContaining("stop");

        String userAgent = assertAndGetUserAgentString();
        expectedMetrics.forEach(expectedMetric -> assertThat(userAgent).matches(METRIC_SEARCH_PATTERN.apply(expectedMetric)));
    }

    @Test
    void when_waiterIsUsed_correctMetricIsAdded() {
        RestJsonWithWaitersAsyncClient asyncClient =
            RestJsonWithWaitersAsyncClient.builder().region(Region.US_WEST_2).credentialsProvider(CREDENTIALS_PROVIDER)
                                          .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)).build();
        RestJsonWithWaitersAsyncWaiter asyncWaiter = RestJsonWithWaitersAsyncWaiter.builder().client(asyncClient).build();

        try {
            CompletableFuture<WaiterResponse<AllTypesResponse>> responseFuture =
                asyncWaiter.waitUntilAllTypesSuccess(AllTypesRequest.builder().integerMember(1).build());
            assertThat(responseFuture.get().attemptsExecuted()).isEqualTo(1);
        } catch (Exception e) {
            String userAgent = assertAndGetUserAgentString();
            assertThat(userAgent).matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.WAITER.value()));
        }
    }

    @Test
    void when_paginatedOperationIsCalled_correctMetricIsAdded() throws Exception {
        ProtocolRestJsonAsyncClientBuilder clientBuilder = asyncClientBuilder();

        PaginatedOperationWithResultKeyPublisher publisher =
            clientBuilder.build().paginatedOperationWithResultKeyPaginator(r -> r.nextToken("token"));

        try {
            CompletableFuture<Void> future = publisher.subscribe(PaginatedOperationWithResultKeyResponse::items);
            future.get();
        } catch (ExecutionException e) {
            String userAgent = assertAndGetUserAgentString();
            assertThat(userAgent).matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.PAGINATOR.value()));
        }
    }

    @Test
    void when_compressedOperationIsCalled_metricIsRecordedButNotAddedToUserAgentString() throws Exception {
        ProtocolRestJsonAsyncClientBuilder clientBuilder = asyncClientBuilder();

        assertThatThrownBy(() -> clientBuilder.build().putOperationWithRequestCompression(r -> r.body(SdkBytes.fromUtf8String(
            "whoo")).overrideConfiguration(o -> o.compressionConfiguration(c -> c.minimumCompressionThresholdInBytes(1)))).join())
            .hasMessageContaining("stop");

        String userAgent = assertAndGetUserAgentString();
        BusinessMetrics attribute = interceptor.executionAttributes().getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
        assertThat(attribute).isNotNull();
        assertThat(attribute.recordedMetrics()).contains(BusinessMetricFeatureId.GZIP_REQUEST_COMPRESSION.value());
        assertThat(userAgent).doesNotMatch(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.GZIP_REQUEST_COMPRESSION.value()));
    }

    private String assertAndGetUserAgentString() {
        Map<String, List<String>> headers = interceptor.context.httpRequest().headers();
        assertThat(headers).containsKey(USER_AGENT_HEADER_NAME);
        return headers.get(USER_AGENT_HEADER_NAME).get(0);
    }

    private ProtocolRestJsonAsyncClientBuilder asyncClientBuilder() {
        return ProtocolRestJsonAsyncClient.builder()
                                          .region(Region.US_WEST_2)
                                          .credentialsProvider(CREDENTIALS_PROVIDER)
                                          .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor));
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("stop");
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }
    }
}