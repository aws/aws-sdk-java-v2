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

package software.amazon.awssdk.auth.signer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

public class BaseEventStreamAsyncAws4SignerTest {
    private static Map<String, HeaderValue> headers;

    @BeforeClass
    public static void setup() {
        headers = new LinkedHashMap<>();
        headers.put("header1", HeaderValue.fromInteger(42));
        headers.put("header2", HeaderValue.fromBoolean(false));
        headers.put("header3", HeaderValue.fromString("Hello world"));
    }

    @Test
    public void toDebugString_emptyPayload_generatesCorrectString() {
        Message m = new Message(headers, new byte[0]);

        assertThat(BaseEventStreamAsyncAws4Signer.toDebugString(m, false))
                .isEqualTo("Message = {headers={header1={42}, header2={false}, header3={\"Hello world\"}}, payload=}");
    }

    @Test
    public void toDebugString_noHeaders_emptyPayload_generatesCorrectString() {
        Message m = new Message(new LinkedHashMap<>(), new byte[0]);

        assertThat(BaseEventStreamAsyncAws4Signer.toDebugString(m, false))
                .isEqualTo("Message = {headers={}, payload=}");
    }

    @Test
    public void toDebugString_largePayload_truncate_generatesCorrectString() {
        byte[] payload = new byte[128];
        new Random().nextBytes(payload);
        Message m = new Message(headers, payload);

        byte[] first32 = Arrays.copyOf(payload, 32);
        String expectedPayloadString = BinaryUtils.toHex(first32);
        assertThat(BaseEventStreamAsyncAws4Signer.toDebugString(m, true))
                .isEqualTo("Message = {headers={header1={42}, header2={false}, header3={\"Hello world\"}}, payload=" + expectedPayloadString + "...}");
    }

    @Test
    public void toDebugString_largePayload_noTruncate_generatesCorrectString() {
        byte[] payload = new byte[128];
        new Random().nextBytes(payload);
        Message m = new Message(headers, payload);

        String expectedPayloadString = BinaryUtils.toHex(payload);
        assertThat(BaseEventStreamAsyncAws4Signer.toDebugString(m, false))
                .isEqualTo("Message = {headers={header1={42}, header2={false}, header3={\"Hello world\"}}, payload=" + expectedPayloadString + "}");
    }

    @Test
    public void toDebugString_smallPayload_truncate_doesNotAddEllipsis() {
        byte[] payload = new byte[8];
        new Random().nextBytes(payload);
        Message m = new Message(headers, payload);

        String expectedPayloadString = BinaryUtils.toHex(payload);
        assertThat(BaseEventStreamAsyncAws4Signer.toDebugString(m, true))
                .isEqualTo("Message = {headers={header1={42}, header2={false}, header3={\"Hello world\"}}, payload=" + expectedPayloadString + "}");
    }
}
