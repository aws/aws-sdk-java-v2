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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.AbortableInputStream;

/**
 * Verifies that {@link ResponseInputStream} correctly supports reading concatenated gzip streams
 * via {@link GZIPInputStream}. This is a regression test for the issue where {@code available()}
 * returning 0 from the underlying network stream caused GZIPInputStream to stop reading at
 * gzip member boundaries, silently truncating multi-member gzip files.
 */
class ResponseInputStreamGzipTest {

    @Test
    void gzipInputStream_readsAllConcatenatedMembers_whenUnderlyingStreamReportsZeroAvailable() throws IOException {
        String member1 = "first gzip member content\n";
        String member2 = "second gzip member content\n";
        String member3 = "third gzip member content\n";

        byte[] concatenatedGzip = createConcatenatedGzip(member1, member2, member3);

        // Simulate a network stream that always returns 0 from available()
        InputStream zeroAvailableStream = new ZeroAvailableInputStream(new ByteArrayInputStream(concatenatedGzip));
        AbortableInputStream abortableStream = AbortableInputStream.create(zeroAvailableStream);
        ResponseInputStream<Object> responseStream = new ResponseInputStream<>(new Object(), abortableStream);

        GZIPInputStream gzipStream = new GZIPInputStream(responseStream);
        String result = new String(readAllBytes(gzipStream));
        gzipStream.close();

        assertThat(result).isEqualTo(member1 + member2 + member3);
    }

    @Test
    void gzipInputStream_readsLargeMultiMemberStream_withoutTruncation() throws IOException {
        String largeMember1 = repeatChar('A', 100_000) + "\n";
        String largeMember2 = repeatChar('B', 100_000) + "\n";
        String largeMember3 = repeatChar('C', 100_000) + "\n";

        byte[] concatenatedGzip = createConcatenatedGzip(largeMember1, largeMember2, largeMember3);

        InputStream zeroAvailableStream = new ZeroAvailableInputStream(new ByteArrayInputStream(concatenatedGzip));
        AbortableInputStream abortableStream = AbortableInputStream.create(zeroAvailableStream);
        ResponseInputStream<Object> responseStream = new ResponseInputStream<>(new Object(), abortableStream);

        GZIPInputStream gzipStream = new GZIPInputStream(responseStream);
        String result = new String(readAllBytes(gzipStream));
        gzipStream.close();

        assertThat(result).isEqualTo(largeMember1 + largeMember2 + largeMember3);
        assertThat(result.length()).isEqualTo(largeMember1.length() + largeMember2.length() + largeMember3.length());
    }

    @Test
    void gzipInputStream_readsManySmallMembers_withoutTruncation() throws IOException {
        int memberCount = 500;
        StringBuilder expected = new StringBuilder();
        String[] members = new String[memberCount];
        for (int i = 0; i < memberCount; i++) {
            members[i] = "member " + i + "\n";
            expected.append(members[i]);
        }

        byte[] concatenatedGzip = createConcatenatedGzip(members);

        InputStream zeroAvailableStream = new ZeroAvailableInputStream(new ByteArrayInputStream(concatenatedGzip));
        AbortableInputStream abortableStream = AbortableInputStream.create(zeroAvailableStream);
        ResponseInputStream<Object> responseStream = new ResponseInputStream<>(new Object(), abortableStream);

        GZIPInputStream gzipStream = new GZIPInputStream(responseStream);
        String result = new String(readAllBytes(gzipStream));
        gzipStream.close();

        assertThat(result).isEqualTo(expected.toString());
    }

    private static byte[] createConcatenatedGzip(String... members) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String member : members) {
            GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            gzipOut.write(member.getBytes());
            gzipOut.finish();
            gzipOut.flush();
        }
        return baos.toByteArray();
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private static String repeatChar(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    /**
     * A stream wrapper that always returns 0 from {@code available()}, simulating
     * the behavior of network-backed streams (e.g., URL connection client) where
     * no bytes are reported as immediately available even though data remains.
     */
    private static class ZeroAvailableInputStream extends FilterInputStream {
        ZeroAvailableInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int available() {
            return 0;
        }
    }
}
