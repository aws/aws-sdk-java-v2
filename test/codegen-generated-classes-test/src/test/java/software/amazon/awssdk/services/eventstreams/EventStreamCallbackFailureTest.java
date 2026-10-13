/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.services.eventstreams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.reactivex.Flowable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventstreamrestjson.EventStreamRestJsonAsyncClient;
import software.amazon.awssdk.services.eventstreamrestjson.model.EventStreamOperationRequest;
import software.amazon.awssdk.services.eventstreamrestjson.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.eventstreamrestjson.model.InputEventStream;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

@RunWith(MockitoJUnitRunner.class)
public class EventStreamCallbackFailureTest {
    @Mock
    private SdkAsyncHttpClient mockHttpClient;

    private final AtomicInteger attempts = new AtomicInteger();
    private EventStreamRestJsonAsyncClient client;

    @Before
    public void setup() {
        when(mockHttpClient.execute(any(AsyncExecuteRequest.class))).thenAnswer(this::mockExecute);
        client = EventStreamRestJsonAsyncClient.builder()
                                               .region(Region.US_WEST_2)
                                               .credentialsProvider(StaticCredentialsProvider.create(
                                                   AwsBasicCredentials.create("akid", "skid")))
                                               .httpClient(mockHttpClient)
                                               .build();
    }

    @After
    public void teardown() {
        client.close();
    }

    @Test(timeout = 5000)
    public void visitorCallbackThrows_operationFailsWithoutRetry() {
        UncheckedIOException callbackFailure = new UncheckedIOException(new IOException("boom"));
        AtomicReference<Throwable> handlerFailure = new AtomicReference<>();
        EventStreamOperationResponseHandler.Visitor visitor = EventStreamOperationResponseHandler.Visitor.builder()
            .onTheEventOne(event -> {
                throw callbackFailure;
            })
            .build();
        EventStreamOperationResponseHandler handler = EventStreamOperationResponseHandler.builder()
            .subscriber(visitor)
            .onError(handlerFailure::set)
            .build();

        CompletableFuture<Void> operation = client.eventStreamOperation(
            EventStreamOperationRequest.builder().build(), Flowable.<InputEventStream>empty(), handler);

        CompletionException completionException = null;
        try {
            operation.join();
        } catch (CompletionException e) {
            completionException = e;
        }
        assertThat(completionException).isNotNull();
        assertThat(completionException.getCause()).isInstanceOf(NonRetryableException.class);
        assertThat(completionException.getCause().getCause()).isSameAs(callbackFailure);
        assertThat(handlerFailure.get()).isInstanceOf(CompletionException.class);
        assertThat(handlerFailure.get().getCause()).isInstanceOf(NonRetryableException.class);
        assertThat(handlerFailure.get().getCause().getCause()).isSameAs(callbackFailure);
        assertThat(attempts).hasValue(1);
    }

    private CompletableFuture<Void> mockExecute(InvocationOnMock invocation) {
        attempts.incrementAndGet();
        AsyncExecuteRequest request = invocation.getArgument(0, AsyncExecuteRequest.class);
        Flowable.fromPublisher(request.requestContentPublisher()).ignoreElements().blockingAwait();
        request.responseHandler().onHeaders(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .putHeader("Content-Type", "application/vnd.amazon.eventstream")
                                                           .build());
        request.responseHandler().onStream(Flowable.just(outputEvent().toByteBuffer()));
        return CompletableFuture.completedFuture(null);
    }

    private Message outputEvent() {
        Map<String, HeaderValue> headers = new HashMap<>();
        headers.put(":message-type", HeaderValue.fromString("event"));
        headers.put(":event-type", HeaderValue.fromString("TheEventOne"));
        headers.put(":content-type", HeaderValue.fromString("application/json"));
        return new Message(headers, "{\"Foo\":\"value\"}".getBytes(StandardCharsets.UTF_8));
    }
}
