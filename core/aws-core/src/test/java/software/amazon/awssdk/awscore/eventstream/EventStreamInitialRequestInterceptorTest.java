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

package software.amazon.awssdk.awscore.eventstream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.HAS_INITIAL_REQUEST_EVENT;
import static software.amazon.awssdk.core.internal.util.Mimetype.MIMETYPE_EVENT_STREAM;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context.ModifyHttpRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

public class EventStreamInitialRequestInterceptorTest {
    static final String RPC_CONTENT_TYPE = "rpc-format";
    Message eventMessage = getEventMessage();
    Flowable<ByteBuffer> bytePublisher = Flowable.just(eventMessage.toByteBuffer(), eventMessage.toByteBuffer());
    byte[] payload = "initial request payload".getBytes(StandardCharsets.UTF_8);
    ExecutionAttributes attr = new ExecutionAttributes().putAttribute(HAS_INITIAL_REQUEST_EVENT, true);
    EventStreamInitialRequestInterceptor interceptor = new EventStreamInitialRequestInterceptor();

    @Test
    public void testHttpHeaderModification() {
        ModifyHttpRequest context = buildContext(bytePublisher, payload);

        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(context, attr);

        List<String> contentType = modifiedRequest.headers().get(CONTENT_TYPE);
        assertEquals(1, contentType.size());
        assertEquals(MIMETYPE_EVENT_STREAM, contentType.get(0));
    }

    @Test
    public void testInitialRequestEvent() {
        ModifyHttpRequest context = buildContext(bytePublisher, payload);

        Optional<AsyncRequestBody> modifiedBody = interceptor.modifyAsyncHttpContent(context, attr);

        List<Message> messages = Flowable.fromPublisher(modifiedBody.get()).map(Message::decode).toList().blockingGet();
        Message initialRequestEvent = messages.get(0);
        assertArrayEquals(payload, initialRequestEvent.getPayload());
        assertEquals(RPC_CONTENT_TYPE, initialRequestEvent.getHeaders().get(":content-type").getString());
    }

    @Test
    public void testPrepending() {
        ModifyHttpRequest context = buildContext(bytePublisher, payload);

        Optional<AsyncRequestBody> modifiedBody = interceptor.modifyAsyncHttpContent(context, attr);

        List<Message> messages = Flowable.fromPublisher(modifiedBody.get()).map(Message::decode).toList().blockingGet();
        assertEquals(3, messages.size());
        assertEquals(eventMessage, messages.get(1));
        assertEquals(eventMessage, messages.get(2));
    }

    @Test
    public void testDisabled() {
        ModifyHttpRequest context = buildContext(bytePublisher, payload);
        attr.putAttribute(HAS_INITIAL_REQUEST_EVENT, false);

        assertSame(context.httpRequest(), interceptor.modifyHttpRequest(context, attr));
        assertSame(context.asyncRequestBody(), interceptor.modifyAsyncHttpContent(context, attr));
    }

    private ModifyHttpRequest buildContext(Flowable<ByteBuffer> bytePublisher, byte[] payload) {
        SdkHttpFullRequest request = buildRequest();
        ModifyHttpRequest context = Mockito.mock(ModifyHttpRequest.class);
        when(context.httpRequest()).thenReturn(request);
        when(context.asyncRequestBody()).thenReturn(Optional.of(AsyncRequestBody.fromPublisher(bytePublisher)));
        when(context.requestBody()).thenReturn(Optional.of(RequestBody.fromByteBuffer(ByteBuffer.wrap(payload))));
        return context;
    }

    private SdkHttpFullRequest buildRequest() {
        return SdkHttpFullRequest.builder()
            .method(SdkHttpMethod.POST)
            .uri(URI.create("https://example.com/"))
            .putHeader(CONTENT_TYPE, RPC_CONTENT_TYPE)
            .build();
    }

    private Message getEventMessage() {
        Map<String, HeaderValue> headers = ImmutableMap.of(":message-type", HeaderValue.fromString("event"),
            ":event-type", HeaderValue.fromString("foo"));
        return new Message(headers, new byte[0]);
    }
}
