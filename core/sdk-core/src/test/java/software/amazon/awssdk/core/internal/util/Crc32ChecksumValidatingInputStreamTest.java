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

package software.amazon.awssdk.core.internal.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.CRC32;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.Crc32MismatchException;
import software.amazon.awssdk.testutils.InputStreamUtils;
import software.amazon.awssdk.utils.StringInputStream;

class Crc32ChecksumValidatingInputStreamTest {

    @Test
    void noException_correctChecksum_shouldNotThrow() {
        String data = RandomStringUtils.random(128);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        CRC32 crc32 = new CRC32();
        crc32.update(dataBytes, 0, dataBytes.length);
        Crc32ChecksumValidatingInputStream is = new Crc32ChecksumValidatingInputStream(
            new StringInputStream(data), crc32.getValue());
        assertThatCode(() -> InputStreamUtils.drainInputStream(is)).doesNotThrowAnyException();
        assertThatCode(is::close).doesNotThrowAnyException();
    }

    @Test
    void noException_incorrectChecksum_shouldThrowCRC32Exception() {
        String data = RandomStringUtils.random(128);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        CRC32 crc32 = new CRC32();
        crc32.update(dataBytes, 0, dataBytes.length);
        Crc32ChecksumValidatingInputStream is = new Crc32ChecksumValidatingInputStream(
            new StringInputStream(data), crc32.getValue() == 0 ? 1 : crc32.getValue()/2);
        InputStreamUtils.drainInputStream(is);

        assertThatThrownBy(is::close).isInstanceOf(Crc32MismatchException.class);
    }

    @Test
    void exceptionWhileReading_shouldNotValidateChecksum() {
        Crc32ChecksumValidatingInputStream is = new Crc32ChecksumValidatingInputStream(
            new FailAfterNInputStream(128, new IOException("test io exception")), 123);
        assertThatThrownBy(() -> InputStreamUtils.drainInputStream(is))
            .hasCauseInstanceOf(IOException.class)
            .hasMessageContaining("test io exception");

        assertThatCode(is::close).doesNotThrowAnyException();
    }

    @Test
    void exception_readMethodCall_shouldNotValidateChecksum() {
        Crc32ChecksumValidatingInputStream is = new Crc32ChecksumValidatingInputStream(
            new FailAfterNInputStream(2, new IOException("test io exception")), 123);
        assertThatThrownBy(() -> {
            is.read();
            is.read();
        })
            .isInstanceOf(IOException.class)
            .hasMessageContaining("test io exception");

        assertThatCode(is::close).doesNotThrowAnyException();
    }

    class FailAfterNInputStream extends InputStream {
        private final int failAfter;
        private final Random random;
        private final IOException exceptionToThrow;

        private int totalRead = 0;

        FailAfterNInputStream(int failAfter, IOException exceptionToThrow) {
            this.failAfter = failAfter;
            this.exceptionToThrow = exceptionToThrow;
            this.random = new Random();
        }

        @Override
        public int read() throws IOException {
            totalRead++;
            if (totalRead >= failAfter) {
                throw exceptionToThrow;
            }
            return random.nextInt();
        }
    }
}