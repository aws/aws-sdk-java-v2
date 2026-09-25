/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;

class ResponseTransformerToInputStreamTest {

    @Test
    void toInputStream_whenCompatibilityNotEnabled_preservesZeroAvailable() throws Exception {
        assertThat(availableAfterHeader(ResponseTransformer.toInputStream(), gzipHeader())).isZero();
    }

    @Test
    void toInputStreamWithTimeout_whenCompatibilityNotEnabled_preservesZeroAvailable() throws Exception {
        assertThat(availableAfterHeader(ResponseTransformer.toInputStream(Duration.ZERO), gzipHeader())).isZero();
    }

    @Test
    void toInputStream_whenCompatibilityEnabled_coercesAvailableForGzip() throws Exception {
        assertThat(availableAfterHeader(ResponseTransformer.toInputStream(true), gzipHeader())).isEqualTo(1);
    }

    @Test
    void toInputStreamWithTimeout_whenCompatibilityEnabled_coercesAvailableForGzip() throws Exception {
        assertThat(availableAfterHeader(ResponseTransformer.toInputStream(Duration.ZERO, true), gzipHeader())).isEqualTo(1);
    }

    @Test
    void toInputStream_whenCompatibilityEnabled_preservesZeroAvailableForNonGzip() throws Exception {
        assertThat(availableAfterHeader(ResponseTransformer.toInputStream(true), new byte[] {1, 2, 3})).isZero();
    }

    @Test
    void toInputStream_whenCompatibilityEnabled_preservesAbort() throws Exception {
        AtomicBoolean aborted = new AtomicBoolean();
        AbortableInputStream content = AbortableInputStream.create(zeroAvailableStream(gzipHeader()),
                                                                   () -> aborted.set(true));
        ResponseInputStream<String> result = ResponseTransformer.<String>toInputStream(true).transform("response", content);

        result.abort();

        assertThat(aborted).isTrue();
    }

    @Test
    void toInputStream_whenCompatibilityEnabled_decodesAllConcatenatedGzipMembers() throws Exception {
        byte[] content = concatenatedGzip("member-1", "member-2", "member-3");
        AbortableInputStream body = AbortableInputStream.create(zeroAvailableStream(content));
        ResponseInputStream<String> result = ResponseTransformer.<String>toInputStream(true).transform("response", body);

        assertThat(readAllGzip(result)).isEqualTo("member-1member-2member-3");
    }

    private static int availableAfterHeader(
        ResponseTransformer<String, ResponseInputStream<String>> transformer, byte[] header) throws Exception {
        AbortableInputStream content = AbortableInputStream.create(zeroAvailableStream(header));
        try (ResponseInputStream<String> result = transformer.transform("response", content)) {
            for (int i = 0; i < header.length; i++) {
                assertThat(result.read()).isNotEqualTo(-1);
            }
            return result.available();
        }
    }

    private static byte[] gzipHeader() {
        return new byte[] {0x1f, (byte) 0x8b, 0x08};
    }

    private static byte[] concatenatedGzip(String... members) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (String member : members) {
            ByteArrayOutputStream compressedMember = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(compressedMember)) {
                gzip.write(member.getBytes(StandardCharsets.UTF_8));
            }
            output.write(compressedMember.toByteArray());
        }
        return output.toByteArray();
    }

    private static String readAllGzip(InputStream inputStream) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(inputStream);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[64];
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static InputStream zeroAvailableStream(byte[] data) {
        return new InputStream() {
            private int position;

            @Override
            public int read() {
                return position < data.length ? data[position++] & 0xff : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (len == 0) {
                    return 0;
                }
                if (position >= data.length) {
                    return -1;
                }
                b[off] = data[position++];
                return 1;
            }

            @Override
            public int available() throws IOException {
                return 0;
            }
        };
    }
}
