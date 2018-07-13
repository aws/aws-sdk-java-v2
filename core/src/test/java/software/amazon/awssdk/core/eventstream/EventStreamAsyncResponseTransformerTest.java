/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.eventstream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

public class EventStreamAsyncResponseTransformerTest {
    @Test
    public void unknownExceptionEventsThrowException() {
        Map<String, HeaderValue> headers = new HashMap<>();
        headers.put(":message-type", HeaderValue.fromString("exception"));
        headers.put(":exception-type", HeaderValue.fromString("modeledException"));
        headers.put(":content-type", HeaderValue.fromString("application/json"));

        verifyExceptionThrown(headers);
    }

    @Test
    public void errorEventsThrowException() {
        Map<String, HeaderValue> headers = new HashMap<>();
        headers.put(":message-type", HeaderValue.fromString("error"));

        verifyExceptionThrown(headers);
    }

    private void verifyExceptionThrown(Map<String, HeaderValue> headers) {
        SdkServiceException exception = new SdkServiceException("");

        Message exceptionMessage = new Message(headers, new byte[0]);

        Flowable<ByteBuffer> bytePublisher = Flowable.just(exceptionMessage.toByteBuffer());

        AsyncResponseTransformer<SdkResponse, Void> transformer =
                new EventStreamAsyncResponseTransformer<>(new SubscribingResponseHandler(), null, null,
                                                          (response, executionAttributes) -> exception);
        transformer.responseReceived(null);
        transformer.onStream(SdkPublisher.adapt(bytePublisher));

        assertThatThrownBy(transformer::complete).isSameAs(exception);
    }

    private static class SubscribingResponseHandler implements EventStreamResponseHandler<Object, Object> {
        @Override
        public void responseReceived(Object response) {
        }

        @Override
        public void onEventStream(SdkPublisher<Object> publisher) {
            publisher.subscribe(e -> {});
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
        }

        @Override
        public void complete() {
        }
    }
}
