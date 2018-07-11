
/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.client;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkAsyncClientHandler;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.EmptySdkResponse;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import utils.HttpTestUtils;

/**
 * Tests to ensure that any failures thrown from calling into the {@link
 * software.amazon.awssdk.core.interceptor.ExecutionInterceptor}s is reported
 * through the returned {@link java.util.concurrent.CompletableFuture}.
 */
@RunWith(Parameterized.class)
public class AsyncClientHandlerInterceptorExceptionTest {
    private final SdkRequest request = mock(SdkRequest.class);

    private final SdkAsyncHttpClient asyncHttpClient = mock(SdkAsyncHttpClient.class);

    private final Marshaller<Request<SdkRequest>, SdkRequest> marshaller = mock(Marshaller.class);

    private final HttpResponseHandler<SdkResponse> responseHandler = mock(HttpResponseHandler.class);

    private final HttpResponseHandler<SdkServiceException> errorResponseHandler = mock(HttpResponseHandler.class);

    private final Hook hook;

    private SdkAsyncClientHandler clientHandler;

    private ClientExecutionParams<SdkRequest, SdkResponse> executionParams;

    @Parameterized.Parameters(name = "Interceptor Hook: {0}")
    public static Collection<Object> data() {
        return Arrays.asList(Hook.values());
    }

    public AsyncClientHandlerInterceptorExceptionTest(Hook hook) {
        this.hook = hook;
    }

    @Before
    public void testSetup() throws Exception {
        executionParams = new ClientExecutionParams<SdkRequest, SdkResponse>()
                .withInput(request)
                .withMarshaller(marshaller)
                .withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler);

        SdkClientConfiguration config = HttpTestUtils.testClientConfiguration().toBuilder()
                .option(SdkClientOption.EXECUTION_INTERCEPTORS, Collections.singletonList(hook.interceptor()))
                .option(SdkClientOption.ASYNC_HTTP_CLIENT, asyncHttpClient)
                .option(SdkClientOption.RETRY_POLICY, RetryPolicy.none())
                .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                .build();

        clientHandler = new SdkAsyncClientHandler(config);

        when(request.overrideConfiguration()).thenReturn(Optional.empty());

        when(marshaller.marshall(eq(request))).thenReturn(new DefaultRequest<>(null));

        when(responseHandler.handle(any(HttpResponse.class), any(ExecutionAttributes.class)))
                .thenReturn(EmptySdkResponse.builder().build());

        Answer<AbortableRunnable> prepareRequestAnswer;
        if (hook != Hook.ON_EXECUTION_FAILURE) {
            prepareRequestAnswer = invocationOnMock -> {
                SdkHttpResponseHandler handler = invocationOnMock.getArgumentAt(3, SdkHttpResponseHandler.class);
                return new AbortableRunnable() {
                    @Override
                    public void run() {
                        handler.headersReceived(SdkHttpFullResponse.builder()
                                .statusCode(200)
                                .build());
                        handler.complete();
                    }

                    @Override
                    public void abort() {
                    }
                };
            };
        } else {
            prepareRequestAnswer = invocationOnMock -> {
                SdkHttpResponseHandler handler = invocationOnMock.getArgumentAt(3, SdkHttpResponseHandler.class);
                return new AbortableRunnable() {
                    @Override
                    public void run() {
                        handler.exceptionOccurred(new RuntimeException("Something went horribly wrong!"));
                    }

                    @Override
                    public void abort() {
                    }
                };
            };
        }

        when(asyncHttpClient.prepareRequest(any(SdkHttpRequest.class),
                                            any(SdkRequestContext.class),
                                            any(SdkHttpRequestProvider.class),
                                            any(SdkHttpResponseHandler.class)))
                                           .thenAnswer(prepareRequestAnswer);
    }

    @Test
    public void test() {
        if (hook != Hook.ON_EXECUTION_FAILURE) {
            doVerify(() -> clientHandler.execute(executionParams), (t) -> t.getCause().getMessage().equals(hook.name()));
        } else {
            // ON_EXECUTION_FAILURE is handled differently because we don't
            // want an exception thrown from the interceptor to replace the
            // original exception.
            doVerify(() -> clientHandler.execute(executionParams),
                     (t) -> {
                        for (; t != null; t = t.getCause()) {
                            if (Hook.ON_EXECUTION_FAILURE.name().equals(t.getMessage())) {
                                return false;
                            }
                        }
                        return true;
                     });
        }
    }

    private void doVerify(Supplier<CompletableFuture<?>> s, Predicate<Throwable> assertFn) {
        CompletableFuture<?> cf = s.get();
        try {
            cf.get();
            Assert.fail("get() method did not fail as expected.");
        } catch (Throwable t) {
            assertTrue(assertFn.test(t));
        }
    }

    public enum Hook {
        BEFORE_EXECUTION(new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(BEFORE_EXECUTION.name());
            }
        }),

        MODIFY_REQUEST(new ExecutionInterceptor() {
            @Override
            public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(MODIFY_REQUEST.name());
            }
        }),

        BEFORE_MARSHALLING(new ExecutionInterceptor() {
            @Override
            public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(BEFORE_MARSHALLING.name());
            }
        }),

        AFTER_MARSHALLING(new ExecutionInterceptor() {
            @Override
            public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(AFTER_MARSHALLING.name());
            }
        }),

        MODIFY_HTTP_REQUEST(new ExecutionInterceptor() {
            @Override
            public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(MODIFY_HTTP_REQUEST.name());
            }
        }),

        BEFORE_TRANSMISSION(new ExecutionInterceptor() {
            @Override
            public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(BEFORE_TRANSMISSION.name());
            }
        }),

        AFTER_TRANSMISSION(new ExecutionInterceptor() {
            @Override
            public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(AFTER_TRANSMISSION.name());
            }
        }),

        MODIFY_HTTP_RESPONSE(new ExecutionInterceptor() {
            @Override
            public SdkHttpFullResponse modifyHttpResponse(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(MODIFY_HTTP_RESPONSE.name());
            }
        }),

        BEFORE_UNMARSHALLING(new ExecutionInterceptor() {
            @Override
            public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(BEFORE_UNMARSHALLING.name());
            }
        }),

        AFTER_UNMARSHALLING(new ExecutionInterceptor() {
            @Override
            public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(AFTER_UNMARSHALLING.name());
            }
        }),

        MODIFY_RESPONSE(new ExecutionInterceptor() {
            @Override
            public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(MODIFY_RESPONSE.name());
            }
        }),

        AFTER_EXECUTION(new ExecutionInterceptor() {
            @Override
            public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(AFTER_EXECUTION.name());
            }
        }),

        ON_EXECUTION_FAILURE(new ExecutionInterceptor() {
            @Override
            public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
                throw new RuntimeException(ON_EXECUTION_FAILURE.name());
            }
        })
        ;

        private ExecutionInterceptor interceptor;

        Hook(ExecutionInterceptor interceptor) {
            this.interceptor = interceptor;
        }

        public ExecutionInterceptor interceptor() {
            return interceptor;
        }
    }
}

