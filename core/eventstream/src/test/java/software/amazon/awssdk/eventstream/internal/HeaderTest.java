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
package software.amazon.awssdk.eventstream.internal;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.eventstream.HeaderType;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.TestUtils;


public class HeaderTest {
    private final TestUtils util = new TestUtils(Double.doubleToLongBits(Math.random()));

    @Test
    public void headerEncoding() throws Exception {
        for (byte i = 0; i <= 9; i++) {
            HeaderType headerType = HeaderType.fromTypeId(i);
            assertEquals(i, headerType.headerTypeId());
            HeaderValue value = util.randomHeaderValue(headerType);
            roundTrip(new Header("test-header", value));
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
}
