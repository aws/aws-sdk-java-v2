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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointParams;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointProvider;

public class EndpointMetricValuesTest {
    private static final String USER_AGENT_HEADER_NAME = "User-Agent";
    private static final StaticCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));


    private RestJsonEndpointProvidersEndpointProvider mockEndpointProvider;
    private CapturingInterceptor capturingInterceptor;

    @BeforeEach
    void setup() {
        capturingInterceptor = new CapturingInterceptor();
        mockEndpointProvider = mock(RestJsonEndpointProvidersEndpointProvider.class);
    }

    @Test
    void endpointMetricValuesAreAddedToUserAgent() {
        List<String> metricValues = Arrays.asList("O", "K");
        when(mockEndpointProvider.resolveEndpoint(any(RestJsonEndpointProvidersEndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(
                Endpoint.builder()
                        .url(URI.create("https://my-service.com"))
                        .putAttribute(AwsEndpointAttribute.METRIC_VALUES, metricValues)
                        .build()));

        RestJsonEndpointProvidersClient client =
            RestJsonEndpointProvidersClient.builder()
                                           .endpointProvider(mockEndpointProvider)
                                           .region(Region.US_WEST_2)
                                           .credentialsProvider(CREDENTIALS_PROVIDER)
                                           .overrideConfiguration(c -> c.addExecutionInterceptor(capturingInterceptor))
                                           .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        })).hasMessageContaining("short-circuit");

        String userAgent = assertAndGetUserAgentString();
        Matcher businessMetricMatcher = Pattern.compile("m/([^\\s]+)").matcher(userAgent);
        assertTrue(businessMetricMatcher.find());
        assertNotNull(businessMetricMatcher.group(1));
        Set<String> metrics = new HashSet<>(Arrays.asList((businessMetricMatcher.group(1).split(","))));
        assertTrue(metrics.containsAll(metricValues));
    }

    @Test
    void endpointMetricValuesDoesNotFailOnEmptyList() {
        List<String> metricValues = Collections.emptyList();
        when(mockEndpointProvider.resolveEndpoint(any(RestJsonEndpointProvidersEndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(
                Endpoint.builder()
                        .url(URI.create("https://my-service.com"))
                        .putAttribute(AwsEndpointAttribute.METRIC_VALUES, metricValues)
                        .build()));

        RestJsonEndpointProvidersClient client =
            RestJsonEndpointProvidersClient.builder()
                                           .endpointProvider(mockEndpointProvider)
                                           .region(Region.US_WEST_2)
                                           .credentialsProvider(CREDENTIALS_PROVIDER)
                                           .overrideConfiguration(c -> c.addExecutionInterceptor(capturingInterceptor))
                                           .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        })).hasMessageContaining("short-circuit");

        String userAgent = assertAndGetUserAgentString();
        Matcher businessMetricMatcher = Pattern.compile("m/([^\\s]+)").matcher(userAgent);
        assertTrue(businessMetricMatcher.find());
        assertNotNull(businessMetricMatcher.group(1));
    }

    private String assertAndGetUserAgentString() {
        Map<String, List<String>> headers = capturingInterceptor.context.httpRequest().headers();
        assertThat(headers).containsKey(USER_AGENT_HEADER_NAME);
        return headers.get(USER_AGENT_HEADER_NAME).get(0);
    }

    private static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("short-circuit");
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }
    }
}
