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

package software.amazon.awssdk.core.client.handler;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.DrainingSubscriber;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

/**
 * Verification tests to ensure that the behavior of {@link SdkAsyncClientHandler} is in line with the
 * {@link AsyncResponseTransformer} interface.
 */
public class AsyncClientHandlerTransformerVerificationTest {
    private static final RetryPolicy RETRY_POLICY = RetryPolicy.defaultRetryPolicy();

    private final SdkRequest request = mock(SdkRequest.class);

    private final Marshaller<SdkRequest> marshaller = mock(Marshaller.class);

    private final HttpResponseHandler<SdkResponse> responseHandler = mock(HttpResponseHandler.class);

    private final HttpResponseHandler<SdkServiceException> errorResponseHandler = mock(HttpResponseHandler.class);

    private SdkAsyncHttpClient mockClient;

    private SdkAsyncClientHandler clientHandler;

    private ClientExecutionParams<SdkRequest, SdkResponse> executionParams;

    @Before
    public void testSetup() throws Exception {
        mockClient = mock(SdkAsyncHttpClient.class);

        executionParams = new ClientExecutionParams<SdkRequest, SdkResponse>()
                .withInput(request)
                .withMarshaller(marshaller)
                .withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler);

        SdkClientConfiguration config = HttpTestUtils.testClientConfiguration().toBuilder()
                .option(SdkClientOption.ASYNC_HTTP_CLIENT, mockClient)
                .option(SdkClientOption.RETRY_POLICY, RETRY_POLICY)
                .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                .build();

        clientHandler = new SdkAsyncClientHandler(config);

        when(request.overrideConfiguration()).thenReturn(Optional.empty());

        when(marshaller.marshall(eq(request))).thenReturn(ValidSdkObjects.sdkHttpFullRequest().build());

        when(responseHandler.handle(any(SdkHttpFullResponse.class), any(ExecutionAttributes.class)))
                .thenReturn(VoidSdkResponse.builder().build());
    }

    @Test
    public void marshallerThrowsException_shouldTriggerExceptionOccurred() {
        SdkClientException exception = SdkClientException.create("Could not handle response");
        when(marshaller.marshall(any(SdkRequest.class))).thenThrow(exception);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        executeAndWaitError(new TestTransformer<SdkResponse, Void>(){
            @Override
            public void exceptionOccurred(Throwable error) {
                exceptionOccurred.set(true);
                super.exceptionOccurred(error);
            }
        });

        assertThat(exceptionOccurred.get()).isTrue();
    }

    @Test
    public void nonRetryableErrorDoesNotTriggerRetry() {
        mockSuccessfulResponse();
        AtomicLong prepareCalls = new AtomicLong(0);
        executeAndWaitError(new TestTransformer<SdkResponse, Void>() {
            @Override
            public CompletableFuture<Void> prepare() {
                prepareCalls.incrementAndGet();
                return super.prepare();
            }

            @Override
            public void onStream(SdkPublisher<ByteBuffer> stream) {
                super.transformFuture().completeExceptionally(new RuntimeException("some error"));
            }
        });
        assertThat(prepareCalls.get()).isEqualTo(1L);
    }

    @Test
    public void prepareCallsEqualToExecuteAttempts() {
        mockSuccessfulResponse();
        AtomicLong prepareCalls = new AtomicLong(0);
        executeAndWaitError(new TestTransformer<SdkResponse, Void>() {
            @Override
            public CompletableFuture<Void> prepare() {
                prepareCalls.incrementAndGet();
                return super.prepare();
            }

            @Override
            public void onStream(SdkPublisher<ByteBuffer> stream) {
                stream.subscribe(new DrainingSubscriber<ByteBuffer>() {
                    @Override
                    public void onComplete() {
                        transformFuture().completeExceptionally(RetryableException.builder().message("retry me please: " + prepareCalls.get()).build());
                    }
                });
            }
        });
        assertThat(prepareCalls.get()).isEqualTo(1 + RETRY_POLICY.numRetries());
    }

    @Test(timeout = 1000L)
    public void handlerExecutionCompletesIndependentlyOfRequestExecution_CompleteAfterResponse() {
        mockSuccessfulResponse_NonSignalingStream();

        // Since we never signal any elements on the response stream, if the async client handler waited for the stream to
        // finish before completing the future, this would never return.
        assertThat(execute(new TestTransformer<SdkResponse, SdkResponse>() {
            @Override
            public void onResponse(SdkResponse response) {
                transformFuture().complete(response);
            }
        })).isNotNull();
    }

    @Test(timeout = 1000L)
    public void handlerExecutionCompletesIndependentlyOfRequestExecution_CompleteAfterStream() {
        mockSuccessfulResponse_NonSignalingStream();
        // Since we never signal any elements on the response stream, if the async client handler waited for the stream to
        // finish before completing the future, this would never return.
        assertThat(execute(new TestTransformer<SdkResponse, Publisher<ByteBuffer>>() {
            @Override
            public void onStream(SdkPublisher<ByteBuffer> stream) {
                transformFuture().complete(stream);
            }
        })).isNotNull();
    }

    private void mockSuccessfulResponse() {
        Answer<CompletableFuture<Void>> executeAnswer = invocationOnMock -> {
            SdkAsyncHttpResponseHandler handler = invocationOnMock.getArgumentAt(0, AsyncExecuteRequest.class).responseHandler();
            handler.onHeaders(SdkHttpFullResponse.builder()
                    .statusCode(200)
                    .build());
            handler.onStream(new EmptyPublisher<>());
            return CompletableFuture.completedFuture(null);
        };
        reset(mockClient);
        when(mockClient.execute(any(AsyncExecuteRequest.class))).thenAnswer(executeAnswer);
    }

    /**
     * Mock a response with success status (200), but with a stream that never signals anything on the Subscriber. This
     * roughly emulates a very slow response.
     */
    private void mockSuccessfulResponse_NonSignalingStream() {
        Answer<CompletableFuture<Void>> executeAnswer = invocationOnMock -> {
            SdkAsyncHttpResponseHandler handler = invocationOnMock.getArgumentAt(0, AsyncExecuteRequest.class).responseHandler();
            handler.onHeaders(SdkHttpFullResponse.builder()
                    .statusCode(200)
                    .build());
            handler.onStream(subscriber -> {
                // never signal onSubscribe
            });
            return CompletableFuture.completedFuture(null);
        };
        reset(mockClient);
        when(mockClient.execute(any(AsyncExecuteRequest.class))).thenAnswer(executeAnswer);
    }

    private void executeAndWaitError(AsyncResponseTransformer<SdkResponse, ?> transformer) {
        try {
            execute(transformer);
            fail("Client execution should have completed exceptionally");
        } catch (CompletionException e) {
            // ignored
        }
    }

    private <ResultT> ResultT execute(AsyncResponseTransformer<SdkResponse, ResultT> transformer) {
        return clientHandler.execute(executionParams, transformer).join();
    }

    private static class TestTransformer<ResponseT, ResultT> implements AsyncResponseTransformer<ResponseT, ResultT> {
        private volatile CompletableFuture<ResultT> transformFuture;

        @Override
        public CompletableFuture<ResultT> prepare() {
            this.transformFuture = new CompletableFuture<>();
            return transformFuture;
        }

        @Override
        public void onResponse(ResponseT response) {
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            publisher.subscribe(new DrainingSubscriber<ByteBuffer>() {
                @Override
                public void onComplete() {
                    transformFuture.complete(null);
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            transformFuture.completeExceptionally(error);
        }

        CompletableFuture<ResultT> transformFuture() {
            return transformFuture;
        }
    }
}
