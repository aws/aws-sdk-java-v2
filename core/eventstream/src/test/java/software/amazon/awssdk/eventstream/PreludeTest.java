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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

public class PreludeTest {
    @Test
    public void maxPayloadSize() throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(new byte[15]);
        buf.putInt(16777217 + 19); // total length
        buf.put((byte) 1); // ApplicationData
        buf.putShort((short) 0); // unmodeled data
        buf.putInt(0); // headers_length
        buf.putInt(0xf0c8e628); // prelude_crc
        buf.flip();

        assertThrows(IllegalArgumentException.class, () -> Prelude.decode(buf));
    }

    @Test
    public void maxHeaderSize() throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(new byte[15]);
        buf.putInt(131073 + 19); // total length
        buf.put((byte) 1); // ApplicationData
        buf.putShort((short) 0); // unmodeled data
        buf.putInt(131073); // headers_length
        buf.putInt(0x49fd415c); // prelude_crc
        buf.flip();

        assertThrows(IllegalArgumentException.class, () -> Prelude.decode(buf));
    }
}
