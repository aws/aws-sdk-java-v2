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

package software.amazon.awssdk.services.kinesis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.KinesisException;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

/**
 * Functional tests for the SubscribeToShard API.
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscribeToShardUnmarshallingTest {
    private static final AwsBasicCredentials CREDENTIALS = AwsBasicCredentials.create("akid", "skid");
    private static final String REQUEST_ID = "a79394c5-59ee-4b36-8127-880aaefa91fc";

    @Mock
    private SdkAsyncHttpClient sdkHttpClient;

    private KinesisAsyncClient client;

    @Before
    public void setup() {
        this.client = KinesisAsyncClient.builder()
                                        .credentialsProvider(() -> CREDENTIALS)
                                        .region(Region.US_EAST_1)
                                        .httpClient(sdkHttpClient)
                                        .build();
    }

    @Test
    public void exceptionWithMessage_UnmarshalledCorrectly() throws Throwable {
        String errorCode = "ResourceNotFoundException";
        AbortableInputStream content = new MessageWriter()
            .writeInitialResponse(new byte[0])
            .writeException("{\"message\": \"foo\"}", errorCode)
            .toInputStream();

        stubResponse(SdkHttpFullResponse.builder()
                                        .statusCode(200)
                                        .content(content)
                                        .putHeader("x-amzn-requestid", REQUEST_ID)
                                        .build());

        try {
            subscribeToShard();
            fail("Expected ResourceNotFoundException exception");
        } catch (ResourceNotFoundException e) {
            assertThat(e.requestId()).isEqualTo(REQUEST_ID);
            assertThat(e.statusCode()).isEqualTo(500);
            assertThat(e.awsErrorDetails().errorCode()).isEqualTo(errorCode);
            assertThat(e.awsErrorDetails().errorMessage()).isEqualTo("foo");
            assertThat(e.awsErrorDetails().serviceName()).isEqualTo("kinesis");
        }
    }

    @Test
    public void errorWithMessage_UnmarshalledCorrectly() throws Throwable {
        String errorCode = "InternalError";
        String message = "error message";
        AbortableInputStream content = new MessageWriter()
            .writeInitialResponse(new byte[0])
            .writeError(errorCode, message)
            .toInputStream();

        stubResponse(SdkHttpFullResponse.builder()
                                        .statusCode(200)
                                        .content(content)
                                        .putHeader("x-amzn-requestid", REQUEST_ID)
                                        .build());

        try {
            subscribeToShard();
            fail("Expected ResourceNotFoundException exception");
        } catch (KinesisException e) {
            assertThat(e.requestId()).isEqualTo(REQUEST_ID);
            assertThat(e.statusCode()).isEqualTo(500);
            assertThat(e.awsErrorDetails().errorCode()).isEqualTo(errorCode);
            assertThat(e.awsErrorDetails().errorMessage()).isEqualTo(message);
            assertThat(e.awsErrorDetails().serviceName()).isEqualTo("kinesis");
        }
    }

    @Test
    public void eventWithRecords_UnmarshalledCorrectly() throws Throwable {
        String data = BinaryUtils.toBase64("foobar".getBytes(StandardCharsets.UTF_8));
        AbortableInputStream content = new MessageWriter()
            .writeInitialResponse(new byte[0])
            .writeEvent("SubscribeToShardEvent",
                        String.format("{\"ContinuationSequenceNumber\": \"1234\","
                                      + "\"MillisBehindLatest\": 0,"
                                      + "\"Records\": [{\"Data\": \"%s\"}]"
                                      + "}", data))
            .toInputStream();
        SubscribeToShardEvent event = SubscribeToShardEvent.builder()
                                                           .continuationSequenceNumber("1234")
                                                           .millisBehindLatest(0L)
                                                           .records(Record.builder()
                                                                          .data(SdkBytes.fromUtf8String("foobar"))
                                                                          .build())
                                                           .build();

        stubResponse(SdkHttpFullResponse.builder()
                                        .statusCode(200)
                                        .content(content)
                                        .build());

        List<SubscribeToShardEventStream> events = subscribeToShard();
        assertThat(events).containsOnly(event);
    }

    @Test
    public void unknownEventType_UnmarshalledCorrectly() throws Throwable {
        AbortableInputStream content = new MessageWriter()
            .writeInitialResponse(new byte[0])
            .writeEvent("ExampleUnknownEventType", "{\"Foo\": \"Bar\"}")
            .toInputStream();

        stubResponse(SdkHttpFullResponse.builder()
                                        .statusCode(200)
                                        .content(content)
                                        .build());

        AtomicInteger unknownEvents = new AtomicInteger(0);
        AtomicInteger knownEvents = new AtomicInteger(0);

        client.subscribeToShard(SubscribeToShardRequest.builder().build(),
                                SubscribeToShardResponseHandler.builder().subscriber(new SubscribeToShardResponseHandler.Visitor() {
                                    @Override
                                    public void visitDefault(SubscribeToShardEventStream event) {
                                        unknownEvents.incrementAndGet();
                                    }

                                    @Override
                                    public void visit(SubscribeToShardEvent event) {
                                        knownEvents.incrementAndGet();
                                    }
                                }).build())
              .get();

        assertThat(unknownEvents.get()).isEqualTo(1);
        assertThat(knownEvents.get()).isEqualTo(0);
    }

    private List<SubscribeToShardEventStream> subscribeToShard() throws Throwable {
        try {
            List<SubscribeToShardEventStream> events = new ArrayList<>();
            client.subscribeToShard(SubscribeToShardRequest.builder().build(),
                                    SubscribeToShardResponseHandler.builder()
                                                                   .subscriber(events::add)
                                                                   .build())
                  .join();
            return events;
        } catch (CompletionException e) {
            throw e.getCause();
        }
    }

    private void stubResponse(SdkHttpFullResponse response) {
        when(sdkHttpClient.execute(any(AsyncExecuteRequest.class))).thenAnswer((Answer<CompletableFuture<Void>>) invocationOnMock -> {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            AsyncExecuteRequest req = invocationOnMock.getArgumentAt(0, AsyncExecuteRequest.class);
            SdkAsyncHttpResponseHandler value = req.responseHandler();
            value.onHeaders(response);
            value.onStream(subscriber -> subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {
                    try {
                        response.content().ifPresent(c -> {
                            byte[] bytes = invokeSafely(() -> IoUtils.toByteArray(c));
                            subscriber.onNext(ByteBuffer.wrap(bytes));
                        });

                        subscriber.onComplete();
                        cf.complete(null);
                    } catch (Throwable e) {
                        subscriber.onError(e);
                        value.onError(e);
                        cf.completeExceptionally(e);
                    }
                }

                @Override
                public void cancel() {
                    RuntimeException e = new RuntimeException();
                    subscriber.onError(e);
                    value.onError(e);
                }
            }));
            return cf;
        });
    }

        public static class MessageWriter {

        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public MessageWriter writeInitialResponse(byte[] payload) {
            new Message(ImmutableMap.of(":message-type", HeaderValue.fromString("event"),
                                        ":event-type", HeaderValue.fromString("initial-response")),
                        payload).encode(baos);
            return this;
        }

        public MessageWriter writeException(String payload, String modeledExceptionName) {
            new Message(ImmutableMap.of(":message-type", HeaderValue.fromString("exception"),
                                        ":exception-type", HeaderValue.fromString(modeledExceptionName)),
                        payload.getBytes(StandardCharsets.UTF_8)).encode(baos);
            return this;
        }

        public MessageWriter writeError(String errorCode, String errorMessage) {
            new Message(ImmutableMap.of(":message-type", HeaderValue.fromString("error"),
                                        ":error-code", HeaderValue.fromString(errorCode),
                                        ":error-message", HeaderValue.fromString(errorMessage)),
                        new byte[0]).encode(baos);
            return this;
        }

        public MessageWriter writeEvent(String eventType, String payload) {
            new Message(ImmutableMap.of(":message-type", HeaderValue.fromString("event"),
                                        ":event-type", HeaderValue.fromString(eventType)),
                        payload.getBytes(StandardCharsets.UTF_8)).encode(baos);
            return this;
        }

        public AbortableInputStream toInputStream() {
            return AbortableInputStream.create(new ByteArrayInputStream(baos.toByteArray()));
        }
    }
}
