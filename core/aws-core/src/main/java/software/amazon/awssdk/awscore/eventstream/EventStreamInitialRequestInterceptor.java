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

import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.HAS_INITIAL_REQUEST_EVENT;
import static software.amazon.awssdk.core.internal.util.Mimetype.MIMETYPE_EVENT_STREAM;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context.ModifyHttpRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.async.AsyncStreamPrepender;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

/**
 * An interceptor for event stream requests sent over RPC. This interceptor will prepend the initial request (i.e. the
 * serialized request POJO) to the stream of events supplied by the caller.
 */
@SdkProtectedApi
public class EventStreamInitialRequestInterceptor implements ExecutionInterceptor {
    @Override
    public SdkHttpRequest modifyHttpRequest(
        ModifyHttpRequest context, ExecutionAttributes executionAttributes
    ) {
        if (!Boolean.TRUE.equals(executionAttributes.getAttribute(HAS_INITIAL_REQUEST_EVENT))) {
            return context.httpRequest();
        }

        return context.httpRequest().toBuilder()
            .removeHeader(CONTENT_TYPE)
            .putHeader(CONTENT_TYPE, MIMETYPE_EVENT_STREAM)
            .build();
    }

    @Override
    public Optional<AsyncRequestBody> modifyAsyncHttpContent(
        ModifyHttpRequest context, ExecutionAttributes executionAttributes
    ) {
        if (!Boolean.TRUE.equals(executionAttributes.getAttribute(HAS_INITIAL_REQUEST_EVENT))) {
            return context.asyncRequestBody();
        }

        /*
         * At this point in the request execution, the requestBody contains the serialized initial request,
         * and the asyncRequestBody contains the event stream proper. We will prepend the former to the
         * latter.
         */
        byte[] payload = getInitialRequestPayload(context);
        String contentType = context.httpRequest().headers().get(CONTENT_TYPE).get(0);

        Map<String, HeaderValue> initialRequestEventHeaders = new HashMap<>();
        initialRequestEventHeaders.put(":message-type", HeaderValue.fromString("event"));
        initialRequestEventHeaders.put(":event-type", HeaderValue.fromString("initial-request"));
        initialRequestEventHeaders.put(":content-type", HeaderValue.fromString(contentType));

        ByteBuffer initialRequest = new Message(initialRequestEventHeaders, payload).toByteBuffer();

        Publisher<ByteBuffer> asyncRequestBody = context.asyncRequestBody()
            .orElseThrow(() -> new IllegalStateException("This request is an event streaming request and thus "
                        + "should have an asyncRequestBody"));
        Publisher<ByteBuffer> withInitialRequest = new AsyncStreamPrepender<>(asyncRequestBody, initialRequest);
        return Optional.of(AsyncRequestBody.fromPublisher(withInitialRequest));
    }

    private byte[] getInitialRequestPayload(ModifyHttpRequest context) {
        RequestBody requestBody = context.requestBody()
            .orElseThrow(() -> new IllegalStateException("This request should have a requestBody"));
        byte[] payload;
        try {
            try (InputStream inputStream = requestBody.contentStreamProvider().newStream()) {
                payload = new byte[inputStream.available()];
                int bytesRead = inputStream.read(payload);
                if (bytesRead != payload.length) {
                    throw new IllegalStateException("Expected " + payload.length + " bytes, but only got " +
                            bytesRead + " bytes");
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read serialized request payload", ex);
        }
        return payload;
    }
}
