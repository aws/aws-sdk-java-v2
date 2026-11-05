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

package software.amazon.awssdk.protocol.tests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR;
import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.ExecutionAwareExecutor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;

public class AsyncResponseThreadingTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private static final String STREAMING_OUTPUT_PATH = "/2016-03-11/streamingOutputOperation";

    private static final ThreadLocal<String> CALLER_CONTEXT = new ThreadLocal<>();
    private static final ExecutionAttribute<String> CAPTURED_CALLER_CONTEXT = new ExecutionAttribute<>("CapturedCallerContext");

    private final ExecutorService completionPool = Executors.newSingleThreadExecutor();
    private final CountDownLatch continuationAttached = new CountDownLatch(1);

    @After
    public void tearDown() {
        CALLER_CONTEXT.remove();
        completionPool.shutdownNow();
    }

    @Test
    public void completionWithNioThreadWorksCorrectly() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).willReturn(aResponse().withStatus(200).withBody("test")));

        Executor mockExecutor = Mockito.spy(new SpyableExecutor());

        ProtocolRestJsonAsyncClient client =
                ProtocolRestJsonAsyncClient.builder()
                                           .region(Region.US_WEST_1)
                                           .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                           .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                           .asyncConfiguration(c -> c.advancedOption(FUTURE_COMPLETION_EXECUTOR, mockExecutor))
                                           .build();

        ResponseBytes<StreamingOutputOperationResponse> response =
                client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                                AsyncResponseTransformer.toBytes()).join();

        verify(mockExecutor).execute(any());

        byte[] arrayCopy = response.asByteArray();
        assertThat(arrayCopy).containsExactly('t', 'e', 's', 't');
    }

    @Test
    public void connectionError_completionWithNioThreadWorksCorrectly() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER).withBody("test")));

        Executor mockExecutor = Mockito.spy(new SpyableExecutor());

        ProtocolRestJsonAsyncClient client =
            ProtocolRestJsonAsyncClient.builder()
                                       .region(Region.US_WEST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                       .asyncConfiguration(c -> c.advancedOption(FUTURE_COMPLETION_EXECUTOR, mockExecutor))
                                       .overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.doNotRetry()))
                                       .build();

        assertThatThrownBy(() ->
            client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                            AsyncResponseTransformer.toBytes()).join())
            .hasCauseInstanceOf(SdkClientException.class);

        verify(mockExecutor).execute(any());
    }

    @Test
    public void serverError_completionWithNioThreadWorksCorrectly() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).willReturn(aResponse().withStatus(500).withBody("test")));

        Executor mockExecutor = Mockito.spy(new SpyableExecutor());

        ProtocolRestJsonAsyncClient client =
            ProtocolRestJsonAsyncClient.builder()
                                       .region(Region.US_WEST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                       .overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.doNotRetry()))
                                       .asyncConfiguration(c -> c.advancedOption(FUTURE_COMPLETION_EXECUTOR, mockExecutor))
                                       .build();

        assertThatThrownBy(() ->
            client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                            AsyncResponseTransformer.toBytes()).join()).hasCauseInstanceOf(ProtocolRestJsonException.class);
        verify(mockExecutor).execute(any());
    }

    @Test
    public void executionAwareExecutor_success_propagatesCallerContextToCompletion() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).willReturn(aResponse().withStatus(200).withBody("test")));

        CompletableFuture<String> contextInContinuation =
            callWithCallerContext(contextPropagatingClient()).handle((r, t) -> CALLER_CONTEXT.get());
        continuationAttached.countDown();

        assertThat(contextInContinuation.join()).isEqualTo("caller-context");
    }

    @Test
    public void executionAwareExecutor_connectionError_propagatesCallerContextToCompletion() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        CompletableFuture<?> response = callWithCallerContext(contextPropagatingClient());
        CompletableFuture<String> contextInContinuation = response.handle((r, t) -> CALLER_CONTEXT.get());
        continuationAttached.countDown();

        assertThat(contextInContinuation.join()).isEqualTo("caller-context");
        assertThatThrownBy(response::join).hasCauseInstanceOf(SdkClientException.class);
    }

    @Test
    public void executionAwareExecutor_serverError_propagatesCallerContextToCompletion() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).willReturn(aResponse().withStatus(500).withBody("test")));

        CompletableFuture<?> response = callWithCallerContext(contextPropagatingClient());
        CompletableFuture<String> contextInContinuation = response.handle((r, t) -> CALLER_CONTEXT.get());
        continuationAttached.countDown();

        assertThat(contextInContinuation.join()).isEqualTo("caller-context");
        assertThatThrownBy(response::join).hasCauseInstanceOf(ProtocolRestJsonException.class);
    }

    @Test
    public void build_withBothFutureCompletionExecutors_throwsException() {
        assertThatThrownBy(() -> ProtocolRestJsonAsyncClient.builder()
                                                            .asyncConfiguration(c -> c.advancedOption(FUTURE_COMPLETION_EXECUTOR,
                                                                                                      Runnable::run)
                                                                                      .advancedOption(
                                                                                          EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR,
                                                                                          (command, attributes) -> command.run())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only one of FUTURE_COMPLETION_EXECUTOR or EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR");
    }

    private ProtocolRestJsonAsyncClient contextPropagatingClient() {
        ExecutionInterceptor captureCallerContext = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                executionAttributes.putAttribute(CAPTURED_CALLER_CONTEXT, CALLER_CONTEXT.get());
            }
        };

        // Completion waits for the test to attach its continuation, so that the continuation runs on the completion thread
        ExecutionAwareExecutor restoreCallerContext = (command, executionAttributes) -> completionPool.execute(() -> {
            awaitContinuationAttached();
            CALLER_CONTEXT.set(executionAttributes.getAttribute(CAPTURED_CALLER_CONTEXT));
            try {
                command.run();
            } finally {
                CALLER_CONTEXT.remove();
            }
        });

        return ProtocolRestJsonAsyncClient.builder()
                                          .region(Region.US_WEST_1)
                                          .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                          .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                          .overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.doNotRetry())
                                                                       .addExecutionInterceptor(captureCallerContext))
                                          .asyncConfiguration(c -> c.advancedOption(EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR,
                                                                                    restoreCallerContext))
                                          .build();
    }

    private void awaitContinuationAttached() {
        try {
            assertThat(continuationAttached.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private CompletableFuture<ResponseBytes<StreamingOutputOperationResponse>> callWithCallerContext(
        ProtocolRestJsonAsyncClient client) {
        CALLER_CONTEXT.set("caller-context");
        try {
            return client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                                   AsyncResponseTransformer.toBytes());
        } finally {
            CALLER_CONTEXT.remove();
        }
    }

    private static class SpyableExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
