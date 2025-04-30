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

package software.amazon.awssdk.services.s3;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.utils.Logger;

/**
 * Base class for testing sync and async request bodies under retries.
 * <p>
 * All the test data is built up using the same random ASCII string so that regardless of form (e.g. String, byte array, file),
 * the binary data is the same.
 */
public class BaseRequestBodyRetryTest {
    private static final Logger LOG = Logger.loggerFor(BaseRequestBodyRetryTest.class);

    private static byte[] random1KbAsciiData;

    protected static final int KB = 1024;
    protected static final int MB = KB * 1024;
    protected static final Map<BodySize, Path> testFiles = new EnumMap<>(BodySize.class);
    protected static final SdkChecksum crc32 = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32);

    private static final ChecksumServlet checksumServlet = new ChecksumServlet(DefaultChecksumAlgorithm.CRC32);
    private static final TestServer testServer = new TestServer(checksumServlet);

    @BeforeAll
    public static void setup() throws Exception {
        testServer.start();
        createTestFiles();
    }

    @AfterAll
    public static void teardown() throws Exception {
        testFiles.values().forEach(tf -> {
            try {
                Files.delete(tf);
            } catch (IOException e) {
                LOG.warn(() -> "Could not delete test file " + tf.toAbsolutePath());
            }
        });
        testServer.stop();
    }

    @BeforeEach
    public void resetServlet() {
        checksumServlet.clearChecksums();
    }

    protected int serverHttpsPort() {
        return testServer.getHttpsPort();
    }

    protected byte[] getDataSegment() {
        return random1KbAsciiData;
    }

    protected static String calculateCrc32(InputStream inputStream) throws IOException {
        crc32.reset();

        byte[] buff = new byte[4096];
        int read;
        while ((read = inputStream.read(buff)) != -1) {
            crc32.update(buff, 0, read);
        }

        inputStream.close();
        return base64Encode(crc32.getChecksumBytes());
    }

    protected byte[] makeArrayOfSize(int size) {
        byte[] segmentData = getDataSegment();
        int sourceLen = segmentData.length;
        if (size % sourceLen != 0) {
            throw new IllegalArgumentException("Must be multiple of " + sourceLen + " bytes");
        }

        byte[] array = new byte[size];
        int segments = size / sourceLen;
        for (int i = 0; i < segments; i++) {
            System.arraycopy(segmentData, 0, array, i * sourceLen, sourceLen);
        }

        return array;
    }

    protected InputStream getMarkSupportedStreamOfSize(BodySize size) throws IOException {
        Path p = testFiles.get(size);
        if (p == null) {
            throw new RuntimeException("No file for size " + size);
        }

        return new BufferedInputStream(Files.newInputStream(p));
    }

    protected String makeStringOfSize(int size) {
        return new String(makeArrayOfSize(size), StandardCharsets.UTF_8);
    }

    protected List<String> getRequestChecksums() {
        return checksumServlet.requestChecksums();
    }

    protected static byte[] getDecodedChecksum(byte[] chunkEncoded) {
        crc32.reset();

        ChunkedDecoder decoder = new ChunkedDecoder(crc32);
        int updateLen = MB;

        int fullSegments = chunkEncoded.length / updateLen;
        int offset = 0;

        for (; fullSegments > 0; fullSegments--, offset += updateLen) {
            decoder.update(chunkEncoded, offset, updateLen);
        }
        decoder.update(chunkEncoded, offset, chunkEncoded.length - offset);

        return decoder.checksumBytes();
    }

    protected static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static void createTestFiles() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(KB);
        // ASCII A-Z
        new Random().ints(KB, 'A', (byte) 'Z' + 1)
                    .forEach(i -> baos.write((byte) i));

        random1KbAsciiData = baos.toByteArray();
        for (BodySize size : BodySize.values()) {
            Path tempFile = Files.createTempFile(null, null);
            OutputStream os = Files.newOutputStream(tempFile);

            for (int i = 0; i < size.getNumBytes() / KB; ++i) {
                os.write(random1KbAsciiData);
            }

            int remainder = size.getNumBytes() % KB;

            if (remainder > 0) {
                os.write(random1KbAsciiData, 0, remainder);
            }

            os.flush();
            os.close();

            testFiles.put(size, tempFile);
        }
    }

    protected enum BodySize {
        SZ_0B(0),

        SZ_128KB(128 * KB),

        SZ_4MB(4 * MB),

        SZ_8MB(8 * MB),

        SZ_16MB(16 * MB),

        SZ_32MB(32 * MB)
        ;

        private final int bytes;

        BodySize(int bytes) {
            this.bytes = bytes;
        }

        public int getNumBytes() {
            return bytes;
        }
    }

    private static class ChecksumServlet extends HttpServlet {
        private final List<String> checksums = new ArrayList<>();
        private final SdkChecksum checksum;

        public ChecksumServlet(ChecksumAlgorithm checksumAlgorithm) {
            checksum = SdkChecksum.forAlgorithm(checksumAlgorithm);
        }

        public List<String> requestChecksums() {
            return checksums;
        }

        public void clearChecksums() {
            checksums.clear();
        }

        @Override
        public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
            ServletInputStream inputStream = request.getInputStream();

            byte[] buff = new byte[4096];
            int read;

            checksum.reset();
            ChunkedDecoder decoder = new ChunkedDecoder(checksum);
            while ((read = inputStream.read(buff)) != -1) {
                decoder.update(buff, 0, read);
            }
            checksums.add(base64Encode(decoder.checksumBytes()));

            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            response.setContentLength(0);
        }
    }
}
