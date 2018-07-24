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

package software.amazon.awssdk.services.kinesis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.services.kinesis.model.KinesisException;
import software.amazon.awssdk.services.kinesis.model.Record;
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

    @Mock
    private SdkAsyncHttpClient sdkHttpClient;

    private KinesisAsyncClient client;

    @Before
    public void setup() {
        this.client = KinesisAsyncClient.builder()
                                        .credentialsProvider(() -> CREDENTIALS)
                                        .httpClient(sdkHttpClient)
                                        .build();
    }

    @Test
    public void exceptionWithMessage_UnmarshalledCorrectly() throws InterruptedException {
        AbortableInputStream content = new MessageWriter()
            .writeInitialResponse(new byte[0])
            .writeException("{\"message\": \"foo\"}")
            .toInputStream();

        stubResponse(SdkHttpFullResponse.builder()
                                        .statusCode(200)
                                        .content(content)
                                        .build());

        assertThatThrownBy(this::subscribeToShard)
            .isInstanceOf(KinesisException.class)
            .hasMessageContaining("foo");
    }

    @Test
    public void errorWithMessage_UnmarshalledCorrectly() throws Throwable {
        AbortableInputStream content = new MessageWriter()
            .writeInitialResponse(new byte[0])
            .writeError("bar")
            .toInputStream();

        stubResponse(SdkHttpFullResponse.builder()
                                        .statusCode(200)
                                        .content(content)
                                        .build());

        assertThatThrownBy(this::subscribeToShard)
            .isInstanceOf(KinesisException.class)
            .hasMessageContaining("bar");
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
        ArgumentCaptor<SdkHttpResponseHandler> captor = ArgumentCaptor.forClass(SdkHttpResponseHandler.class);
        when(sdkHttpClient.prepareRequest(any(SdkHttpRequest.class),
                                          any(SdkRequestContext.class),
                                          any(SdkHttpRequestProvider.class),
                                          captor.capture()))
            .thenReturn(new AbortableRunnable() {
                @Override
                public void run() {
                    SdkHttpResponseHandler value = captor.getValue();
                    value.headersReceived(response);
                    value.onStream(subscriber -> subscriber.onSubscribe(new Subscription() {
                        @Override
                        public void request(long l) {
                            try {
                                response.content().ifPresent(c -> {
                                    byte[] bytes = invokeSafely(() -> IoUtils.toByteArray(c));
                                    subscriber.onNext(ByteBuffer.wrap(bytes));
                                });
                            } finally {
                                subscriber.onComplete();
                                value.complete();
                            }
                        }

                        @Override
                        public void cancel() {
                        }
                    }));
                }

                @Override
                public void abort() {
                }
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

        public MessageWriter writeException(String payload) {
            new Message(ImmutableMap.of(":message-type", HeaderValue.fromString("exception")),
                        payload.getBytes(StandardCharsets.UTF_8)).encode(baos);
            return this;
        }

        public MessageWriter writeError(String message) {
            new Message(ImmutableMap.of(":message-type", HeaderValue.fromString("error"),
                                        ":error-message", HeaderValue.fromString(message)),
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
            return new AbortableInputStream(new ByteArrayInputStream(baos.toByteArray()), () -> {
            });
        }
    }
}
