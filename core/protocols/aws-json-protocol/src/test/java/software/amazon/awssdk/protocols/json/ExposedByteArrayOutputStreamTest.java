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

package software.amazon.awssdk.protocols.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ExposedByteArrayOutputStreamTest {

    @Test
    void emptyStream_hasZeroSize() {
        ExposedByteArrayOutputStream stream = new ExposedByteArrayOutputStream(64);
        assertThat(stream.size()).isEqualTo(0);
        assertThat(stream.toByteArray()).isEmpty();
    }

    @Test
    void buf_returnsInternalBuffer() {
        ExposedByteArrayOutputStream stream = new ExposedByteArrayOutputStream(64);
        byte[] data = {1, 2, 3, 4, 5};
        stream.write(data, 0, data.length);

        byte[] buf = stream.buf();
        // buf is the live internal buffer — it may be larger than size()
        assertThat(buf.length).isGreaterThanOrEqualTo(stream.size());
        // The valid data in buf[0..size()-1] matches what was written
        assertThat(Arrays.copyOf(buf, stream.size())).isEqualTo(data);
    }

    @Test
    void buf_reflectsWrittenData_afterGrowth() {
        // Start with a tiny buffer to force growth
        ExposedByteArrayOutputStream stream = new ExposedByteArrayOutputStream(4);
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        stream.write(data, 0, data.length);

        byte[] buf = stream.buf();
        assertThat(stream.size()).isEqualTo(100);
        assertThat(Arrays.copyOf(buf, stream.size())).isEqualTo(data);
    }

    @Test
    void toByteArray_returnsCopy_notSameReference() {
        ExposedByteArrayOutputStream stream = new ExposedByteArrayOutputStream(64);
        stream.write(new byte[]{1, 2, 3}, 0, 3);

        byte[] copy = stream.toByteArray();
        byte[] buf = stream.buf();
        // toByteArray returns a copy, buf returns the live buffer
        assertThat(copy).isNotSameAs(buf);
        assertThat(copy).isEqualTo(Arrays.copyOf(buf, stream.size()));
    }

    @Test
    void singleByteWrite_worksCorrectly() {
        ExposedByteArrayOutputStream stream = new ExposedByteArrayOutputStream(64);
        stream.write(0x42);
        assertThat(stream.size()).isEqualTo(1);
        assertThat(stream.buf()[0]).isEqualTo((byte) 0x42);
    }
}
