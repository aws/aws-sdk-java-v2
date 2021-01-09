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

package software.amazon.awssdk.services.eventstreams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.services.eventstreamrestjson.EventStreamRestJsonAsyncClient;
import software.amazon.awssdk.services.eventstreamrestjson.model.EventStream;
import software.amazon.awssdk.services.eventstreamrestjson.model.EventStreamOperationRequest;
import software.amazon.awssdk.services.eventstreamrestjson.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.eventstreamrestjson.model.InputEventStream;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

@RunWith(MockitoJUnitRunner.class)
public class EventMarshallingTest {
    @Mock
    public SdkAsyncHttpClient mockHttpClient;

    private EventStreamRestJsonAsyncClient client;

    private List<Message> marshalledEvents;

    private MessageDecoder chunkDecoder;
    private MessageDecoder eventDecoder;

    @Before
    public void setup() {
        when(mockHttpClient.execute(any(AsyncExecuteRequest.class))).thenAnswer(this::mockExecute);
        client = EventStreamRestJsonAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .httpClient(mockHttpClient)
                .build();

        marshalledEvents = new ArrayList<>();

        chunkDecoder = new MessageDecoder();
        eventDecoder = new MessageDecoder();
    }

    @Test
    public void testMarshalling_setsCorrectEventType() {
        List<InputEventStream> inputEvents = Stream.of(
                InputEventStream.inputEventBuilder().build(),
                InputEventStream.inputEventBBuilder().build(),
                InputEventStream.inputEventTwoBuilder().build()
        ).collect(Collectors.toList());

        Flowable<InputEventStream> inputStream = Flowable.fromIterable(inputEvents);

        client.eventStreamOperation(EventStreamOperationRequest.builder().build(), inputStream, EventStreamOperationResponseHandler.builder()
                .subscriber(() -> new Subscriber<EventStream>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {

                    }

                    @Override
                    public void onNext(EventStream eventStream) {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                })
                .build()).join();

        List<String> expectedTypes = Stream.of(
                "InputEvent",
                "InputEventB",
                "InputEventTwo"
        ).collect(Collectors.toList());;

        assertThat(marshalledEvents).hasSize(inputEvents.size());

        for (int i = 0; i < marshalledEvents.size(); ++i) {
            Message marshalledEvent = marshalledEvents.get(i);
            String expectedType = expectedTypes.get(i);
            assertThat(marshalledEvent.getHeaders().get(":event-type").getString())
                    .isEqualTo(expectedType);
        }
    }

    private CompletableFuture<Void> mockExecute(InvocationOnMock invocation) {
        AsyncExecuteRequest request = invocation.getArgumentAt(0, AsyncExecuteRequest.class);
        SdkHttpContentPublisher content = request.requestContentPublisher();
        List<ByteBuffer> chunks = Flowable.fromPublisher(content).toList().blockingGet();

        for (ByteBuffer c : chunks) {
            chunkDecoder.feed(c);
        }

        for (Message m : chunkDecoder.getDecodedMessages()) {
            eventDecoder.feed(m.getPayload());
        }

        marshalledEvents.addAll(eventDecoder.getDecodedMessages());

        request.responseHandler().onHeaders(SdkHttpResponse.builder().statusCode(200).build());
        request.responseHandler().onStream(Flowable.empty());

        return CompletableFuture.completedFuture(null);
    }
}
