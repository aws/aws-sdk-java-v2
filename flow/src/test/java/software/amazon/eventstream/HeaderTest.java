/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.eventstream;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static software.amazon.eventstream.HeaderValue.fromInteger;

public class HeaderTest {
    @Test
    public void genericHeaders() throws Exception {
        roundTrip(new Header("test-string-header", "test-string-value"));
        roundTrip(new Header("test-byte-array-header", HeaderValue.fromByteArray(bb(1, 2, 3, 4, 5, 6, 7, 8))));
        roundTrip(new Header("test-uint32-header", fromInteger(8918230)));
    }

    @Test
    public void typeId() {
        for (byte i = 0; i <= 9; i++) {
            assertEquals(i, HeaderType.fromTypeId(i).headerTypeId);
        }
    }

    static void roundTrip(Header header) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            header.encode(dos);
        }
        byte[] bytes = baos.toByteArray();
        Header actual = Header.decode(ByteBuffer.wrap(bytes));

        assertEquals(header, actual);
    }

    static byte[] bb(int... bytes) {
        byte[] bs = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            bs[i] = (byte) bytes[i];
        }
        return bs;
    }
}
