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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

public class EventStreamTestUtils {

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
