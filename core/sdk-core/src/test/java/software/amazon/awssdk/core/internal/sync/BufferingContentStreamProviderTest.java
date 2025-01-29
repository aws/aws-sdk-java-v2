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

package software.amazon.awssdk.core.internal.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;

class BufferingContentStreamProviderTest {
    private static final SdkChecksum CRC32 = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32);
    private static final byte[] TEST_DATA = "BufferingContentStreamProviderTest".getBytes(StandardCharsets.UTF_8);
    private static final String TEST_DATA_CHECKSUM = "f9ed1825";

    private RequestBody requestBody;

    @BeforeEach
    void setup() {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_DATA);
        requestBody = RequestBody.fromContentProvider(() -> stream, "text/plain");
    }

    @Test
    void newStream_alwaysStartsAtBeginning() {
        String stream1Crc32 = getCrc32(requestBody.contentStreamProvider().newStream());
        String stream2Crc32 = getCrc32(requestBody.contentStreamProvider().newStream());

        assertThat(stream1Crc32).isEqualTo(TEST_DATA_CHECKSUM);
        assertThat(stream2Crc32).isEqualTo(TEST_DATA_CHECKSUM);
    }

    @Test
    void newStream_buffersSkippedBytes() throws IOException {
        InputStream stream1 = requestBody.contentStreamProvider().newStream();

        assertThat(stream1.skip(Long.MAX_VALUE)).isEqualTo(TEST_DATA.length);

        String stream2Crc32 = getCrc32(requestBody.contentStreamProvider().newStream());

        assertThat(stream2Crc32).isEqualTo(TEST_DATA_CHECKSUM);
    }

    @Test
    void newStream_oneByteReads_dataBufferedCorrectly() throws IOException {
        InputStream stream = requestBody.contentStreamProvider().newStream();
        int read;
        do {
            read = stream.read();
        } while (read != -1);

        assertThat(getCrc32(requestBody.contentStreamProvider().newStream())).isEqualTo(TEST_DATA_CHECKSUM);
    }

    @Test
    void newStream_wholeArrayReads_dataBufferedCorrectly() throws IOException {
        InputStream stream = requestBody.contentStreamProvider().newStream();
        int read;
        byte[] buff = new byte[32];
        do {
            read = stream.read(buff);
        } while (read != -1);

        assertThat(getCrc32(requestBody.contentStreamProvider().newStream())).isEqualTo(TEST_DATA_CHECKSUM);
    }

    @Test
    void newStream_offsetArrayReads_dataBufferedCorrectly() throws IOException {
        InputStream stream = requestBody.contentStreamProvider().newStream();
        int read;
        byte[] buff = new byte[32];
        do {
            read = stream.read(buff, 0, 32);
        } while (read != -1);

        assertThat(getCrc32(requestBody.contentStreamProvider().newStream())).isEqualTo(TEST_DATA_CHECKSUM);
    }

    @Test
    void newStream_closeClosesDelegateStream() throws IOException {
        InputStream stream = Mockito.spy(new ByteArrayInputStream(TEST_DATA));
        requestBody = RequestBody.fromContentProvider(() -> stream, "text/plain");
        requestBody.contentStreamProvider().newStream().close();

        Mockito.verify(stream).close();
    }

    @Test
    void newStream_allDataBuffered_doesNotCloseDelegate() throws IOException {
        InputStream delegateStream = Mockito.spy(new ByteArrayInputStream(TEST_DATA));

        requestBody = RequestBody.fromContentProvider(() -> delegateStream, "text/plain");

        IoUtils.drainInputStream(requestBody.contentStreamProvider().newStream());
        Mockito.verify(delegateStream, Mockito.atLeast(1)).read(any(), anyInt(), anyInt());
        Mockito.verifyNoMoreInteractions(delegateStream);
    }

    @Test
    public void newStream_delegateStreamClosedOnBufferingStreamClose() throws IOException {
        InputStream delegateStream = Mockito.spy(new ByteArrayInputStream(TEST_DATA));

        requestBody = RequestBody.fromContentProvider(() -> delegateStream, "text/plain");

        InputStream stream = requestBody.contentStreamProvider().newStream();
        IoUtils.drainInputStream(stream);
        stream.close();

        Mockito.verify(delegateStream).close();
    }

    private static String getCrc32(InputStream inputStream) {
        byte[] buff = new byte[1024];
        int read;

        CRC32.reset();
        try {
            while ((read = inputStream.read(buff)) != -1) {
                CRC32.update(buff, 0, read);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return BinaryUtils.toHex(CRC32.getChecksumBytes());
    }
}
