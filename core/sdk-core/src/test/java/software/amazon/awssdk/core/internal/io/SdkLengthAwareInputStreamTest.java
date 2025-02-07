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

package software.amazon.awssdk.core.internal.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.IoUtils;

class SdkLengthAwareInputStreamTest {
    private InputStream delegateStream;

    @BeforeEach
    void setup() {
        delegateStream = mock(InputStream.class);
    }

    @Test
    void read_lengthIs0_returnsEof() throws IOException {
        when(delegateStream.available()).thenReturn(Integer.MAX_VALUE);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegateStream, 0);

        assertThat(is.read()).isEqualTo(-1);
        assertThat(is.read(new byte[16], 0, 16)).isEqualTo(-1);
    }

    @Test
    void read_lengthNonZero_delegateEof_returnsEof() throws IOException {
        when(delegateStream.read()).thenReturn(-1);
        when(delegateStream.read(any(byte[].class), any(int.class), any(int.class))).thenReturn(-1);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegateStream, 0);

        assertThat(is.read()).isEqualTo(-1);
        assertThat(is.read(new byte[16], 0, 16)).isEqualTo(-1);
    }

    @Test
    void readByte_lengthNonZero_delegateHasAvailable_returnsDelegateData() throws IOException {
        when(delegateStream.read()).thenReturn(42);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegateStream, 16);

        assertThat(is.read()).isEqualTo(42);
    }

    @Test
    void readArray_lengthNonZero_delegateHasAvailable_returnsDelegateData() throws IOException {
        when(delegateStream.read(any(byte[].class), any(int.class), any(int.class))).thenReturn(8);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegateStream, 16);

        assertThat(is.read(new byte[16], 0, 16)).isEqualTo(8);
    }

    @Test
    void readArray_lengthNonZero_propagatesCallToDelegate() throws IOException {
        when(delegateStream.read(any(byte[].class), any(int.class), any(int.class))).thenReturn(8);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegateStream, 16);
        byte[] buff = new byte[16];
        is.read(buff, 0, 16);

        verify(delegateStream).read(buff, 0, 16);
    }

    @Test
    void read_markAndReset_availableReflectsNewLength() throws IOException {
        delegateStream = new ByteArrayInputStream(new byte[32]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegateStream, 16);

        for (int i = 0; i < 4; ++i) {
            is.read();
        }
        assertThat(is.available()).isEqualTo(12);

        is.mark(16);

        for (int i = 0; i < 4; ++i) {
            is.read();
        }
        assertThat(is.available()).isEqualTo(8);

        is.reset();

        assertThat(is.available()).isEqualTo(12);
    }

    @Test
    void skip_markAndReset_availableReflectsNewLength() throws IOException {
        delegateStream = new ByteArrayInputStream(new byte[32]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegateStream, 16);

        is.skip(4);

        assertThat(is.remaining()).isEqualTo(12);

        is.mark(16);

        for (int i = 0; i < 4; ++i) {
            is.read();
        }

        assertThat(is.remaining()).isEqualTo(8);

        is.reset();

        assertThat(is.remaining()).isEqualTo(12);
    }

    @Test
    void skip_delegateSkipsLessThanRequested_availableUpdatedCorrectly() throws IOException {
        when(delegateStream.skip(any(long.class))).thenAnswer(i -> {
            Long n = i.getArgument(0, Long.class);
            return n / 2;
        });

        when(delegateStream.read(any(byte[].class), any(int.class), any(int.class))).thenReturn(1);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegateStream, 16);

        long skipped = is.skip(4);

        assertThat(skipped).isEqualTo(2);
        assertThat(is.remaining()).isEqualTo(14);
    }

    @Test
    void readArray_delegateReadsLessThanRequested_availableUpdatedCorrectly() throws IOException {
        when(delegateStream.read(any(byte[].class), any(int.class), any(int.class))).thenAnswer(i -> {
            Integer n = i.getArgument(2, Integer.class);
            return n / 2;
        });

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegateStream, 16);

        long read = is.read(new byte[16], 0, 8);

        assertThat(read).isEqualTo(4);
        assertThat(is.remaining()).isEqualTo(12);
    }

    @Test
    void readArray_delegateShorterThanExpected_throws() {
        int delegateLength = 16;
        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[delegateLength]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegate, delegateLength + 1);

        assertThatThrownBy(() -> {
            int read;
            byte[] buff = new byte[4096];
            while ((read = is.read(buff, 0, buff.length)) != -1) {
            }
        })
            .isInstanceOf(IOException.class)
            .hasMessage("Reached EOF before reading entire expected content");
    }

    @Test
    void readArray_readExactLength_doesNotThrow() throws IOException {
        int delegateLength = 16;
        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[delegateLength]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegate, delegateLength);

        int total = 0;
        int read;
        byte[] buff = new byte[delegateLength];
        while ((read = is.read(buff, 0, delegateLength)) != -1) {
            total += read;
        }

        assertThat(total).isEqualTo(delegateLength);
    }

    @Test
    void readArray_delegateLongerThanRequired_truncated() throws IOException {
        int delegateLength = 32;
        int length = 16;
        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[delegateLength]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegate, length);

        int total = 0;
        int read;
        byte[] buff = new byte[delegateLength];
        while ((read = is.read(buff, 0, delegateLength)) != -1) {
            total += read;
        }

        assertThat(total).isEqualTo(length);
    }

    @Test
    void readByte_delegateShorterThanExpected_throws() {
        int delegateLength = 16;
        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[delegateLength]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegate, delegateLength + 1);

        assertThatThrownBy(() -> {
            int read;
            while ((read = is.read()) != -1) {
            }
        })
            .isInstanceOf(IOException.class)
            .hasMessage("Reached EOF before reading entire expected content");
    }

    @Test
    void readByte_readExactLength_doesNotThrow() throws IOException {
        int delegateLength = 16;
        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[delegateLength]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegate, delegateLength);

        int total = 0;
        while (total != delegateLength && is.read() != -1) {
            ++total;
        }

        assertThat(total).isEqualTo(delegateLength);
    }

    @Test
    void readByte_delegateLongerThanRequired_truncated() throws IOException {
        int delegateLength = 32;
        int length = 16;
        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[delegateLength]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegate, length);

        int total = 0;
        while (total != delegateLength && is.read() != -1) {
            ++total;
        }

        assertThat(total).isEqualTo(length);
    }

    @Test
    public void skip_thenReadByteUntilEof_doesNotThrowLengthMismatch() throws IOException {
        int delegateLength = 32;

        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[delegateLength]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegate, delegateLength);

        int bytesToSkip = 8;
        int skippedBytes = 0;
        while (skippedBytes != bytesToSkip) {
            long skipped = is.skip(1);
            if (skipped > 0) {
                skippedBytes += skipped;
            }
        }

        int totalRead = 0;
        while (is.read() != -1) {
            ++totalRead;
        }

        assertThat(totalRead + skippedBytes).isEqualTo(delegateLength);
    }

    @Test
    public void skip_thenReadArrayUntilEof_doesNotThrowLengthMismatch() throws IOException {
        int delegateLength = 32;

        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[delegateLength]);

        SdkLengthAwareInputStream is = new SdkLengthAwareInputStream(delegate, delegateLength);

        int bytesToSkip = 8;
        int skippedBytes = 0;
        while (skippedBytes != bytesToSkip) {
            long skipped = is.skip(1);
            if (skipped > 0) {
                skippedBytes += skipped;
            }
        }

        int totalRead = 0;
        byte[] buff = new byte[8];
        int read;

        while ((read = is.read(buff, 0, buff.length)) != -1) {
            totalRead += read;
        }


        assertThat(totalRead + skippedBytes).isEqualTo(delegateLength);
    }
}