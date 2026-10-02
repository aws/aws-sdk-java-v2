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

package software.amazon.awssdk.core.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.core.internal.sync.BufferingContentStreamProvider;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.testutils.RandomInputStream;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;


public class RequestBodyTest {
    private static final SdkChecksum CRC32 = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void stringConstructorUsesUTF8ByteLength() {
        // U+03A9 U+03C9
        final String multibyteChars = "Ωω";
        RequestBody rb = RequestBody.fromString(multibyteChars);
        assertThat(rb.contentLength()).isEqualTo(4L);
    }

    @Test
    public void stringConstructorHasCorrectContentType() {
        RequestBody requestBody = RequestBody.fromString("hello world");
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_TEXT_PLAIN + "; charset=UTF-8");
    }

    @Test
    public void stringConstructorWithCharsetHasCorrectContentType() {
        RequestBody requestBody = RequestBody.fromString("hello world", StandardCharsets.US_ASCII);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_TEXT_PLAIN + "; charset=US-ASCII");
    }

    @Test
    public void fileConstructorHasCorrectContentType() throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path path = fs.getPath("./test");
        Files.write(path, "hello world".getBytes());
        RequestBody requestBody = RequestBody.fromFile(path);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void streamConstructorHasCorrectContentType() {
        StringInputStream inputStream = new StringInputStream("hello world");
        RequestBody requestBody = RequestBody.fromInputStream(inputStream, 11);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
        IoUtils.closeQuietly(inputStream, null);
    }


    @Test
    public void nonMarkSupportedInputStreamContentType() throws IOException {
        File file = folder.newFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("hello world");
        }
        InputStream inputStream = Files.newInputStream(file.toPath());
        RequestBody requestBody = RequestBody.fromInputStream(inputStream, 11);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
        assertThat(requestBody.contentStreamProvider().newStream()).isNotNull();
        IoUtils.closeQuietly(inputStream, null);
    }

    @Test
    public void bytesArrayConstructorHasCorrectContentType() {
        RequestBody requestBody = RequestBody.fromBytes("hello world".getBytes());
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void bytesBufferConstructorHasCorrectContentType() {
        ByteBuffer byteBuffer = ByteBuffer.wrap("hello world".getBytes());
        RequestBody requestBody = RequestBody.fromByteBuffer(byteBuffer);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void emptyBytesConstructorHasCorrectContentType() {
        RequestBody requestBody = RequestBody.empty();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void contentProviderConstuctorWithNullContentLength_NoContentLength() {
        byte[] bytes = new byte[0];
        RequestBody requestBody = RequestBody.fromContentProvider(() -> new ByteArrayInputStream(bytes),
                                                                  Mimetype.MIMETYPE_OCTET_STREAM);
        assertThat(requestBody.optionalContentLength().isPresent()).isFalse();
        assertThatThrownBy(() -> requestBody.contentLength()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void remainingByteBufferConstructorOnlyRemainingBytesCopied() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put(new byte[]{1, 2, 3, 4});
        bb.flip();

        bb.get();
        bb.get();

        int originalRemaining = bb.remaining();

        RequestBody requestBody = RequestBody.fromRemainingByteBuffer(bb);

        assertThat(requestBody.contentLength()).isEqualTo(originalRemaining);

        byte[] requestBodyBytes = IoUtils.toByteArray(requestBody.contentStreamProvider().newStream());
        assertThat(ByteBuffer.wrap(requestBodyBytes)).isEqualTo(bb);
    }

    @Test
    public void fromInputStream_streamSupportsReset_resetsTheStream() {
        byte[] newData = new byte[16536];
        new Random().nextBytes(newData);

        String streamCrc32 = getCrc32(new ByteArrayInputStream(newData));

        ByteArrayInputStream stream = new ByteArrayInputStream(newData);
        assertThat(stream.markSupported()).isTrue();
        RequestBody requestBody = RequestBody.fromInputStream(stream, newData.length);

        assertThat(getCrc32(requestBody.contentStreamProvider().newStream())).isEqualTo(streamCrc32);
        assertThat(getCrc32(requestBody.contentStreamProvider().newStream())).isEqualTo(streamCrc32);
    }

    @Test
    public void fromInputStream_streamNotSupportReset_shouldThrowException() {
        RandomInputStream stream = new RandomInputStream(100);
        assertThat(stream.markSupported()).isFalse();
        RequestBody requestBody = RequestBody.fromInputStream(stream, 100);
        IoUtils.drainInputStream(requestBody.contentStreamProvider().newStream());
        assertThatThrownBy(() -> requestBody.contentStreamProvider().newStream())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Content input stream does not support mark/reset");
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
