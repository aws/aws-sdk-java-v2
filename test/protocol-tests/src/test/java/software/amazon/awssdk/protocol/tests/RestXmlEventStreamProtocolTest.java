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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlAsyncClient;
import software.amazon.awssdk.services.protocolrestxml.model.EventStream;
import software.amazon.awssdk.services.protocolrestxml.model.EventStreamOperationResponse;
import software.amazon.awssdk.services.protocolrestxml.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.protocolrestxml.model.ProtocolRestXmlException;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

public class RestXmlEventStreamProtocolTest {
    private static final Region REGION = Region.US_WEST_2;
    private static final AwsCredentialsProvider CREDENTIALS = StaticCredentialsProvider.create(
        AwsBasicCredentials.create("akid", "skid"));

    @Rule
    public WireMockRule wireMock = new WireMockRule();

    private ProtocolRestXmlAsyncClient client;

    @Before
    public void setup() {
        client = ProtocolRestXmlAsyncClient.builder()
                                           .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                           .region(REGION)
                                           .credentialsProvider(CREDENTIALS)
                                           .build();
    }

    @After
    public void teardown() {
        client.close();
    }

    @Test
    public void eventStreamOperation_unmarshallsHeaderMembersIntoResponse() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        event("SomeUnknownEvent", new byte[0]).encode(baos);

        String expectedValue = "HelloEventStreams";
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("Header-Member", expectedValue)
                                                    .withBody(baos.toByteArray())));

        TestHandler testHandler = new TestHandler();
        client.eventStreamOperation(r -> {}, testHandler).join();

        assertThat(testHandler.receivedResponse.headerMember()).isEqualTo(expectedValue);
    }

    @Test
    public void unknownEventType_unmarshallsToUnknown() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        event("SomeUnknownEvent", new byte[0]).encode(baos);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(baos.toByteArray())));

        TestHandler testHandler = new TestHandler();
        client.eventStreamOperation(r -> {}, testHandler).join();

        assertThat(testHandler.receivedEvents).containsExactly(EventStream.UNKNOWN);
        assertThat(testHandler.receivedEvents.get(0).sdkEventType()).isEqualTo(EventStream.EventType.UNKNOWN_TO_SDK_VERSION);
    }

    @Test
    public void eventPayloadEvent_unmarshallsToEventPayloadEvent() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        event(EventStream.EventType.EVENT_PAYLOAD_EVENT.toString(),
              "<Foo>bar</Foo>".getBytes(StandardCharsets.UTF_8))
            .encode(baos);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(baos.toByteArray())));

        TestHandler testHandler = new TestHandler();
        client.eventStreamOperation(r -> {}, testHandler).join();

        assertThat(testHandler.receivedEvents).containsExactly(EventStream.eventPayloadEventBuilder().foo("bar").build());
        assertThat(testHandler.receivedEvents.get(0).sdkEventType()).isEqualTo(EventStream.EventType.EVENT_PAYLOAD_EVENT);
    }

    @Test
    public void secondEventPayloadEvent_unmarshallsToSecondEventPayloadEvent() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        event(EventStream.EventType.SECOND_EVENT_PAYLOAD_EVENT.toString(),
              "<Foo>bar</Foo>".getBytes(StandardCharsets.UTF_8))
            .encode(baos);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(baos.toByteArray())));

        TestHandler testHandler = new TestHandler();
        client.eventStreamOperation(r -> {}, testHandler).join();

        assertThat(testHandler.receivedEvents).containsExactly(EventStream.secondEventPayloadEventBuilder().foo("bar").build());
        assertThat(testHandler.receivedEvents.get(0).sdkEventType()).isEqualTo(EventStream.EventType.SECOND_EVENT_PAYLOAD_EVENT);
    }

    @Test
    public void nonEventPayloadEvent_unmarshallsToNonEventPayloadEvent() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        event(EventStream.EventType.NON_EVENT_PAYLOAD_EVENT.toString(),
              "<NonEventPayloadEvent><Bar>baz</Bar></NonEventPayloadEvent>".getBytes(StandardCharsets.UTF_8))
            .encode(baos);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(baos.toByteArray())));

        TestHandler testHandler = new TestHandler();
        client.eventStreamOperation(r -> {}, testHandler).join();

        assertThat(testHandler.receivedEvents).containsExactly(EventStream.nonEventPayloadEventBuilder().bar("baz").build());
        assertThat(testHandler.receivedEvents.get(0).sdkEventType()).isEqualTo(EventStream.EventType.NON_EVENT_PAYLOAD_EVENT);
    }

    @Test
    public void errorResponse_unmarshalledCorrectly() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(500)));

        TestHandler testHandler = new TestHandler();
        CompletableFuture<Void> responseFuture = client.eventStreamOperation(r -> {
        }, testHandler);

        assertThatThrownBy(responseFuture::join).hasCauseInstanceOf(ProtocolRestXmlException.class);
    }

    private static Message event(String type, byte[] payload) {
        Map<String, HeaderValue> headers = new HashMap<>();
        headers.put(":message-type", HeaderValue.fromString("event"));
        headers.put(":event-type", HeaderValue.fromString(type));
        return new Message(headers, payload);
    }

    private static class TestHandler implements EventStreamOperationResponseHandler {
        private final List<EventStream> receivedEvents = new ArrayList<>();
        private EventStreamOperationResponse receivedResponse;

        @Override
        public void responseReceived(EventStreamOperationResponse response) {
            this.receivedResponse = response;
        }

        @Override
        public void onEventStream(SdkPublisher<EventStream> publisher) {
            publisher.subscribe(receivedEvents::add);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
        }

        @Override
        public void complete() {
        }
    }
}
