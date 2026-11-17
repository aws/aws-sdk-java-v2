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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils.noOpResponseHandler;
import static utils.HttpTestUtils.testAsyncClientBuilder;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import utils.StubSdkAsyncHttpClient;
import utils.ValidSdkObjects;

/**
 * Checks that the async request pipeline hands control back to the caller while the identity is still being resolved.
 */
class AsyncHttpClientIdentityResolutionTest {

    @Test
    void execute_identityNotYetResolved_returnsWithoutWaitingForIdentity() {
        AmazonAsyncHttpClient client = testAsyncClientBuilder()
            .retryStrategy(DefaultRetryStrategy.doNotRetry())
            .asyncHttpClient(StubSdkAsyncHttpClient.create())
            .build();
        CompletableFuture<AwsCredentialsIdentity> identityFuture = new CompletableFuture<>();

        CompletableFuture<Object> response = assertTimeoutPreemptively(
            Duration.ofSeconds(5),
            () -> client.requestExecutionBuilder()
                        .request(request())
                        .originalRequest(NoopTestRequest.builder().build())
                        .executionContext(executionContext(identityFuture))
                        .execute(noOpResponseHandler()));

        assertThat(response).isNotDone();
    }

    @SuppressWarnings("unchecked")
    private static ExecutionContext executionContext(CompletableFuture<AwsCredentialsIdentity> identity) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, Collections.emptyMap());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                         new SelectedAuthScheme<>(identity,
                                                                  Mockito.mock(HttpSigner.class),
                                                                  AuthSchemeOption.builder().schemeId("mock").build()));
        InterceptorContext interceptorContext =
            InterceptorContext.builder()
                              .request(NoopTestRequest.builder().build())
                              .httpRequest(request())
                              .build();
        return ExecutionContext.builder()
                               .interceptorChain(new ExecutionInterceptorChain(Collections.emptyList()))
                               .executionAttributes(executionAttributes)
                               .interceptorContext(interceptorContext)
                               .metricCollector(MetricCollector.create("ApiCall"))
                               .build();
    }

    private static SdkHttpFullRequest request() {
        return ValidSdkObjects.sdkHttpFullRequest()
                              .host("localhost")
                              .contentStreamProvider(() -> new ByteArrayInputStream("test".getBytes()))
                              .build();
    }
}
