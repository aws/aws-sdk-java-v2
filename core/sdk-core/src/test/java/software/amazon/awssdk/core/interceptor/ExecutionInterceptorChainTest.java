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

package software.amazon.awssdk.core.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpResponse;
import utils.ValidSdkObjects;

class ExecutionInterceptorChainTest {
    private static final String INTERCEPTOR_FAILURE_MESSAGE = "interceptor failure";

    @Test
    void modifyHttpResponseThrows_closesResponseBody() {
        assertResponseBodyClosedWhenInterceptorThrows(new ExecutionInterceptor() {
            @Override
            public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                      ExecutionAttributes executionAttributes) {
                throw new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
            }
        }, Hook.MODIFY_HTTP_RESPONSE);
    }

    @Test
    void modifyHttpResponseContentThrows_closesResponseBody() {
        assertResponseBodyClosedWhenInterceptorThrows(new ExecutionInterceptor() {
            @Override
            public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                                   ExecutionAttributes executionAttributes) {
                throw new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
            }
        }, Hook.MODIFY_HTTP_RESPONSE);
    }

    @Test
    void modifyHttpResponse_whenNoInterceptorThrows_doesNotCloseResponseBody() {
        TrackableInputStream responseBody = new TrackableInputStream();
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Collections.singletonList(new ExecutionInterceptor() {
            }));

        InterceptorContext result =
            chain.modifyHttpResponse(contextWithBody(responseBody), new ExecutionAttributes());

        assertThat(result.responseBody()).contains(responseBody);
        assertThat(responseBody.closed).isFalse();
    }

    @Test
    void afterTransmissionThrows_closesResponseBody() {
        assertResponseBodyClosedWhenInterceptorThrows(new ExecutionInterceptor() {
            @Override
            public void afterTransmission(Context.AfterTransmission context,
                                          ExecutionAttributes executionAttributes) {
                throw new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
            }
        }, Hook.AFTER_TRANSMISSION);
    }

    @Test
    void beforeUnmarshallingThrows_closesResponseBody() {
        assertResponseBodyClosedWhenInterceptorThrows(new ExecutionInterceptor() {
            @Override
            public void beforeUnmarshalling(Context.BeforeUnmarshalling context,
                                            ExecutionAttributes executionAttributes) {
                throw new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
            }
        }, Hook.BEFORE_UNMARSHALLING);
    }

    @Test
    void afterExecutionThrows_closesResponseBody() {
        assertResponseBodyClosedWhenInterceptorThrows(new ExecutionInterceptor() {
            @Override
            public void afterExecution(Context.AfterExecution context,
                                       ExecutionAttributes executionAttributes) {
                throw new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
            }
        }, Hook.AFTER_EXECUTION);
    }

    @Test
    void modifyHttpResponseThrows_withNoResponseBody_preservesInterceptorException() {
        RuntimeException interceptorException = new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                      ExecutionAttributes executionAttributes) {
                throw interceptorException;
            }
        };
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Collections.singletonList(interceptor));

        assertThatThrownBy(() -> chain.modifyHttpResponse(contextWithBody(null), new ExecutionAttributes()))
            .isSameAs(interceptorException);
    }

    @Test
    void modifyAsyncHttpResponseContentThrows_cancelsResponsePublisher() {
        RuntimeException interceptorException = new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
        TrackablePublisher responsePublisher = new TrackablePublisher();
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(
                Context.ModifyHttpResponse context,
                ExecutionAttributes executionAttributes) {

                throw interceptorException;
            }
        };
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Collections.singletonList(interceptor));

        assertThatThrownBy(() -> chain.modifyAsyncHttpResponse(contextWithPublisher(responsePublisher),
                                                               new ExecutionAttributes()))
            .isSameAs(interceptorException);
        assertThat(responsePublisher.cancelled).isTrue();
        assertThat(responsePublisher.requested).isFalse();
    }

    @Test
    void modifyAsyncHttpResponse_whenNoInterceptorThrows_doesNotCancelResponsePublisher() {
        TrackablePublisher responsePublisher = new TrackablePublisher();
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Collections.singletonList(new ExecutionInterceptor() {
            }));

        InterceptorContext result =
            chain.modifyAsyncHttpResponse(contextWithPublisher(responsePublisher), new ExecutionAttributes());

        assertThat(result.responsePublisher()).contains(responsePublisher);
        assertThat(responsePublisher.cancelled).isFalse();
        assertThat(responsePublisher.requested).isFalse();
    }

    @Test
    void modifyAsyncHttpResponseContentThrows_withNoResponsePublisher_preservesInterceptorException() {
        RuntimeException interceptorException = new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(
                Context.ModifyHttpResponse context,
                ExecutionAttributes executionAttributes) {

                throw interceptorException;
            }
        };
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Collections.singletonList(interceptor));

        assertThatThrownBy(() -> chain.modifyAsyncHttpResponse(contextWithPublisher(null), new ExecutionAttributes()))
            .isSameAs(interceptorException);
    }

    @Test
    void responsePublisherCancellationThrows_preservesInterceptorException() {
        RuntimeException interceptorException = new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(
                Context.ModifyHttpResponse context,
                ExecutionAttributes executionAttributes) {

                throw interceptorException;
            }
        };
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Collections.singletonList(interceptor));

        assertThatThrownBy(() -> chain.modifyAsyncHttpResponse(contextWithPublisher(subscriber -> {
            throw new RuntimeException("cancel failed");
        }), new ExecutionAttributes()))
            .isSameAs(interceptorException);
    }

    @Test
    void modifyAsyncHttpResponse_whenLaterInterceptorThrows_cancelsLatestResponsePublisher() {
        TrackablePublisher originalResponsePublisher = new TrackablePublisher();
        TrackablePublisher modifiedResponsePublisher = new TrackablePublisher(originalResponsePublisher);
        ExecutionInterceptor throwingInterceptor = new ExecutionInterceptor() {
            @Override
            public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(
                Context.ModifyHttpResponse context,
                ExecutionAttributes executionAttributes) {

                throw new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
            }
        };
        ExecutionInterceptor modifyingInterceptor = new ExecutionInterceptor() {
            @Override
            public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(
                Context.ModifyHttpResponse context,
                ExecutionAttributes executionAttributes) {

                return Optional.of(modifiedResponsePublisher);
            }
        };
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Arrays.asList(throwingInterceptor, modifyingInterceptor));

        assertThatThrownBy(() -> chain.modifyAsyncHttpResponse(contextWithPublisher(originalResponsePublisher),
                                                               new ExecutionAttributes()))
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage(INTERCEPTOR_FAILURE_MESSAGE);
        assertThat(modifiedResponsePublisher.cancelled).isTrue();
        assertThat(modifiedResponsePublisher.requested).isFalse();
        // The test publisher models a wrapping interceptor that propagates cancellation to its delegate.
        assertThat(originalResponsePublisher.cancelled).isTrue();
        assertThat(originalResponsePublisher.requested).isFalse();
    }

    @Test
    void modifyHttpResponse_whenLaterInterceptorThrows_closesLatestResponseBody() {
        TrackableInputStream originalResponseBody = new TrackableInputStream();
        TrackableInputStream modifiedResponseBody = new TrackableInputStream(originalResponseBody);
        ExecutionInterceptor throwingInterceptor = new ExecutionInterceptor() {
            @Override
            public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                      ExecutionAttributes executionAttributes) {
                throw new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
            }
        };
        ExecutionInterceptor modifyingInterceptor = new ExecutionInterceptor() {
            @Override
            public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                                   ExecutionAttributes executionAttributes) {
                return Optional.of(modifiedResponseBody);
            }
        };
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Arrays.asList(throwingInterceptor, modifyingInterceptor));

        assertThatThrownBy(() -> chain.modifyHttpResponse(contextWithBody(originalResponseBody), new ExecutionAttributes()))
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage(INTERCEPTOR_FAILURE_MESSAGE);
        assertThat(modifiedResponseBody.closed).isTrue();
        assertThat(originalResponseBody.closed).isTrue();
    }

    @Test
    void responseBodyCloseThrows_preservesInterceptorException() {
        RuntimeException interceptorException = new RuntimeException(INTERCEPTOR_FAILURE_MESSAGE);
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                      ExecutionAttributes executionAttributes) {
                throw interceptorException;
            }
        };
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Collections.singletonList(interceptor));

        assertThatThrownBy(() -> chain.modifyHttpResponse(contextWithBody(new FailingCloseInputStream()),
                                                          new ExecutionAttributes()))
            .isSameAs(interceptorException);
    }

    private void assertResponseBodyClosedWhenInterceptorThrows(ExecutionInterceptor interceptor, Hook hook) {
        TrackableInputStream responseBody = new TrackableInputStream();
        ExecutionInterceptorChain chain =
            new ExecutionInterceptorChain(Collections.singletonList(interceptor));

        assertThatThrownBy(() -> hook.invoke(chain, contextWithBody(responseBody)))
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage(INTERCEPTOR_FAILURE_MESSAGE);
        assertThat(responseBody.closed).isTrue();
    }

    private InterceptorContext contextWithBody(InputStream responseBody) {
        return InterceptorContext.builder()
                                 .request(ValidSdkObjects.sdkRequest())
                                 .httpResponse(SdkHttpResponse.builder().statusCode(200).build())
                                 .responseBody(responseBody)
                                 .build();
    }

    private InterceptorContext contextWithPublisher(Publisher<ByteBuffer> responsePublisher) {
        return InterceptorContext.builder()
                                 .request(ValidSdkObjects.sdkRequest())
                                 .httpResponse(SdkHttpResponse.builder().statusCode(200).build())
                                 .responsePublisher(responsePublisher)
                                 .build();
    }

    private enum Hook {
        AFTER_TRANSMISSION {
            @Override
            void invoke(ExecutionInterceptorChain chain, InterceptorContext context) {
                chain.afterTransmission(context, new ExecutionAttributes());
            }
        },
        MODIFY_HTTP_RESPONSE {
            @Override
            void invoke(ExecutionInterceptorChain chain, InterceptorContext context) {
                chain.modifyHttpResponse(context, new ExecutionAttributes());
            }
        },
        BEFORE_UNMARSHALLING {
            @Override
            void invoke(ExecutionInterceptorChain chain, InterceptorContext context) {
                chain.beforeUnmarshalling(context, new ExecutionAttributes());
            }
        },
        AFTER_EXECUTION {
            @Override
            void invoke(ExecutionInterceptorChain chain, InterceptorContext context) {
                chain.afterExecution(context, new ExecutionAttributes());
            }
        };

        abstract void invoke(ExecutionInterceptorChain chain, InterceptorContext context);
    }

    private static final class TrackableInputStream extends InputStream {
        private final InputStream delegate;
        private boolean closed;

        private TrackableInputStream() {
            this(null);
        }

        private TrackableInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate == null ? -1 : delegate.read();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            if (delegate != null) {
                delegate.close();
            }
        }
    }

    private static final class FailingCloseInputStream extends InputStream {
        @Override
        public int read() {
            return -1;
        }

        @Override
        public void close() throws IOException {
            throw new IOException("close failed");
        }
    }

    private static final class TrackablePublisher implements Publisher<ByteBuffer> {
        private final TrackablePublisher delegate;
        private boolean cancelled;
        private boolean requested;

        private TrackablePublisher() {
            this(null);
        }

        private TrackablePublisher(TrackablePublisher delegate) {
            this.delegate = delegate;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    requested = true;
                }

                @Override
                public void cancel() {
                    cancelled = true;
                    if (delegate != null) {
                        delegate.cancelled = true;
                    }
                }
            });
        }
    }
}
