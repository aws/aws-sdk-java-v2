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

package software.amazon.awssdk.eventstream;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TestUtils {
    private final Random rand;
    private final int maxHeaders;
    private final int maxHeaderSize;
    private final int maxPayloadSize;

    public TestUtils(long seed) {
        this.rand = new Random(seed);
        this.maxHeaders = 20;
        this.maxHeaderSize = 64;
        this.maxPayloadSize = 4096;
    }

    public Message randomMessage() {
        return new Message(randomHeaders(), randomPayload());
    }

    public List<Message> randomMessages(int count, int maxPayloadSize) {
        ArrayList<Message> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messages.add(randomMessage(rand.nextInt(maxPayloadSize + 1)));
        }
        return messages;
    }

    public Message randomMessage(int payloadSize) {
        return new Message(randomHeaders(), randomPayload(payloadSize));
    }

    private Map<String, HeaderValue> randomHeaders() {
        Map<String, HeaderValue> headers = new HashMap<>();
        int numHeaders = rand.nextInt(maxHeaders + 1);
        for (int i = 0; i < numHeaders; i++) {
            headers.put("asdf" + rand.nextInt(), randomHeaderValue());
        }
        return headers;
    }

    public HeaderValue randomHeaderValue(HeaderType type) {
        switch (type) {
            case TRUE:
                return HeaderValue.fromBoolean(true);
            case FALSE:
                return HeaderValue.fromBoolean(false);
            case BYTE:
                return HeaderValue.fromByte((byte) rand.nextInt());
            case SHORT:
                return HeaderValue.fromShort((short) rand.nextInt());
            case INTEGER:
                return HeaderValue.fromInteger(rand.nextInt());
            case LONG:
                return HeaderValue.fromLong(rand.nextLong());
            case BYTE_ARRAY:
                int bytes = rand.nextInt(maxHeaderSize + 1) + 1;
                byte[] buf = new byte[bytes];
                rand.nextBytes(buf);
                return HeaderValue.fromByteArray(buf);
            case STRING:
                return HeaderValue.fromString("asdf");
            case TIMESTAMP:
                return HeaderValue.fromTimestamp(Instant.ofEpochMilli(rand.nextLong()));
            case UUID:
                return HeaderValue.fromUuid(new UUID(rand.nextLong(), rand.nextLong()));
        }
        throw new RuntimeException("unknown header type: " + type);
    }

    private HeaderValue randomHeaderValue() {
        HeaderType type = HeaderType.fromTypeId((byte) rand.nextInt(10));
        return randomHeaderValue(type);
    }

    private byte[] randomPayload() {
        return randomPayload(rand.nextInt(maxPayloadSize + 1));
    }

    private byte[] randomPayload(int payloadSize) {
        byte[] ret = new byte[payloadSize];
        rand.nextBytes(ret);
        return ret;
    }
}
