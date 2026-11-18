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

package software.amazon.awssdk.services.cloudfront.internal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class DerParserTest {

    @Test
    void read_lengthLargerThanInput_throwsIOException() {
        DerParser parser = new DerParser(bytes(0x30, 0x84, 0x7F, 0xFF, 0xFF, 0xFF));

        assertThatThrownBy(parser::read)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("stream too short, missing value");
    }

    @Test
    void read_negativeLength_throwsIOException() {
        DerParser parser = new DerParser(bytes(0x30, 0x84, 0x80, 0x00, 0x00, 0x00));

        assertThatThrownBy(parser::read)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("length too large");
    }

    @Test
    void read_truncatedValue_throwsIOException() {
        DerParser parser = new DerParser(bytes(0x02, 0x05, 0x01, 0x02));

        assertThatThrownBy(parser::read)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("stream too short, missing value");
    }

    @Test
    void read_truncatedLength_throwsIOException() {
        DerParser parser = new DerParser(bytes(0x02, 0x82, 0x01));

        assertThatThrownBy(parser::read)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("length too short");
    }

    @Test
    void read_validInteger_returnsValue() throws IOException {
        Asn1Object object = new DerParser(bytes(0x02, 0x02, 0x01, 0x00)).read();

        assertThat(object.getType()).isEqualTo(DerParser.INTEGER);
        assertThat(object.getLength()).isEqualTo(2);
        assertThat(object.getInteger()).isEqualTo(BigInteger.valueOf(256));
    }

    @Test
    void read_emptyValue_returnsEmptyValue() throws IOException {
        Asn1Object object = new DerParser(bytes(0x05, 0x00)).read();

        assertThat(object.getType()).isEqualTo(DerParser.NULL);
        assertThat(object.getValue()).isEmpty();
    }

    @Test
    void read_streamReturningOneBytePerRead_readsWholeValue() throws IOException {
        byte[] value = new byte[300];
        for (int i = 0; i < value.length; i++) {
            value[i] = (byte) i;
        }
        byte[] der = new byte[4 + value.length];
        der[0] = 0x04;
        der[1] = (byte) 0x82;
        der[2] = 0x01;
        der[3] = 0x2C;
        System.arraycopy(value, 0, der, 4, value.length);

        InputStream oneByteAtATime = new FilterInputStream(new ByteArrayInputStream(der)) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return super.read(b, off, Math.min(len, 1));
            }
        };

        Asn1Object object = new DerParser(oneByteAtATime).read();

        assertThat(object.getType()).isEqualTo(DerParser.OCTET_STRING);
        assertThat(object.getLength()).isEqualTo(300);
        assertThat(object.getValue()).isEqualTo(value);
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }
}
