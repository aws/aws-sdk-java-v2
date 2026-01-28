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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class IoUtilsTest {

    private final Random random = new Random();

    @Test
    public void testEmptyByteArray() throws Exception {
        String s = IoUtils.toUtf8String(new ByteArrayInputStream(new byte[0]));
        assertEquals("", s);
    }

    @Test
    public void testZeroByteStream() throws Exception {
        String s = IoUtils.toUtf8String(new InputStream() {
            @Override
            public int read() throws IOException {
                return -1;
            }
        });
        assertEquals("", s);
    }

    @Test
    public void test() throws Exception {
        String s = IoUtils.toUtf8String(new ByteArrayInputStream("Testing".getBytes(StandardCharsets.UTF_8)));
        assertEquals("Testing", s);
    }

    @Test
    public void drainInputStream_AlreadyEos_DoesNotThrowException() throws IOException {
        final InputStream inputStream = randomInputStream();
        while (inputStream.read() != -1) {
        }
        IoUtils.drainInputStream(inputStream);
    }

    @Test
    public void drainInputStream_RemainingBytesInStream_ReadsAllRemainingData() throws IOException {
        final InputStream inputStream = randomInputStream();
        IoUtils.drainInputStream(inputStream);
        assertEquals(-1, inputStream.read());
    }

    @Test
    public void applyMaxReadLimit_shouldHonor() throws IOException {
        int length = 1 << 18;
        InputStream inputStream = randomInputStream(length);
        InputStream bufferedInputStream = new BufferedInputStream(inputStream);

        IoUtils.markStreamWithMaxReadLimit(bufferedInputStream, length + 1);
        IoUtils.drainInputStream(bufferedInputStream);
        assertThat(bufferedInputStream.available()).isEqualTo(0);
        bufferedInputStream.reset();

        assertThat(bufferedInputStream.available()).isEqualTo(length);
    }

    private InputStream randomInputStream() {
        byte[] data = new byte[100];
        random.nextBytes(data);
        return new ByteArrayInputStream(data);
    }

    private InputStream randomInputStream(int length) {
        byte[] data = new byte[length];
        random.nextBytes(data);
        return new ByteArrayInputStream(data);
    }
}
