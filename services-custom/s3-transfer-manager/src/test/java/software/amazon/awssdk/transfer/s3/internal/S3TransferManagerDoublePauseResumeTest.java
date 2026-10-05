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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.fail;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;

/**
 * Downloads an object, pauses, resumes, pauses the resumed download, and resumes again, with every
 * {@link ResumableFileDownload} produced by a real {@code pause()} rather than built by the test.
 * <p>
 * The download is never paused "somewhere in the middle": {@link GatedObjectServer} sends nothing beyond a fixed offset,
 * so each pause happens once the destination file holds exactly the bytes up to that offset. The only waiting is for that
 * fixed state to be reached, which cannot be overshot or raced, so the test does not depend on timing.
 * <p>
 * For the CRT-based transfer manager the offsets are part boundaries and only one connection is used, because CRT only
 * writes whole parts, in order, to the destination file.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class S3TransferManagerDoublePauseResumeTest {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String E_TAG = "\"0123456789abcdef\"";
    private static final Instant LAST_MODIFIED = Instant.parse("2026-09-28T12:00:00Z");

    private static final int PART_SIZE = 8 * 1024;
    private static final int OBJECT_SIZE = 16 * PART_SIZE;
    private static final byte[] CONTENT = randomContent(OBJECT_SIZE);

    // Bytes on disk at the first and second pause. Part multiples, so they also line up with CRT's parts.
    private static final long FIRST_PAUSE = 2L * PART_SIZE;
    private static final long SECOND_PAUSE = 5L * PART_SIZE;

    private static final long RANGE_START = 20_000;
    private static final long RANGE_END = RANGE_START + 10L * PART_SIZE - 1;
    private static final long SUFFIX_LENGTH = 90_000;

    private static final long STATE_TIMEOUT_MILLIS = 30_000;

    // Shared across cases: closing the Netty-based client waits for its event loops to shut down, which takes seconds.
    private static GatedObjectServer server;
    private static S3AsyncClient javaClient;
    private static S3AsyncClient crtClient;

    private final List<FileDownload> downloads = new ArrayList<>();
    private S3TransferManager tm;
    private Path destination;

    enum Client {
        JAVA,
        CRT
    }

    @BeforeAll
    static void setUpServerAndClients() throws IOException {
        server = new GatedObjectServer(CONTENT, E_TAG, LAST_MODIFIED);
        // Multipart is off, so a download that is not ranged is resumed with a ranged GET too, rather than by part.
        javaClient = S3AsyncClient.builder()
                                  .region(Region.US_EAST_1)
                                  .endpointOverride(server.endpoint())
                                  .forcePathStyle(true)
                                  .credentialsProvider(credentials())
                                  .build();
        crtClient = S3AsyncClient.crtBuilder()
                                 .region(Region.US_EAST_1)
                                 .endpointOverride(server.endpoint())
                                 .forcePathStyle(true)
                                 .credentialsProvider(credentials())
                                 .minimumPartSizeInBytes((long) PART_SIZE)
                                 .maxConcurrency(1)
                                 .build();
    }

    @AfterAll
    static void tearDownServerAndClients() {
        javaClient.close();
        crtClient.close();
        server.close();
    }

    @BeforeEach
    void setUp() {
        server.reset();
        destination = RandomTempFile.randomUncreatedFile().toPath();
    }

    @AfterEach
    void tearDown() throws IOException {
        // A failed case can leave a download running; stop it before it can retry against the next case's server state.
        downloads.forEach(d -> d.completionFuture().cancel(true));
        if (tm != null) {
            tm.close();
        }
        server.reset();
        Files.deleteIfExists(destination);
    }

    static Stream<Arguments> cases() {
        Stream.Builder<Arguments> cases = Stream.builder();
        for (Client client : Client.values()) {
            // range, offset in the object of the first byte of the destination file, inclusive end of what is downloaded
            cases.add(Arguments.of(client, null, 0L, OBJECT_SIZE - 1L));
            cases.add(Arguments.of(client, "bytes=" + RANGE_START + "-" + RANGE_END, RANGE_START, RANGE_END));
            cases.add(Arguments.of(client, "bytes=" + RANGE_START + "-", RANGE_START, OBJECT_SIZE - 1L));
            cases.add(Arguments.of(client, "bytes=-" + SUFFIX_LENGTH, OBJECT_SIZE - SUFFIX_LENGTH, OBJECT_SIZE - 1L));
        }
        return cases.build();
    }

    @ParameterizedTest(name = "{0} client, range={1}")
    @MethodSource("cases")
    void pauseResumePauseResume_continuesEachTimeAndWritesExactlyTheRequestedBytes(Client client,
                                                                                   String range,
                                                                                   long fileStart,
                                                                                   long lastByte) throws Exception {
        tm = S3TransferManager.builder().s3Client(client == Client.JAVA ? javaClient : crtClient).build();

        // 1. Fresh download, paused with FIRST_PAUSE bytes on disk.
        server.holdAt(fileStart + FIRST_PAUSE);
        FileDownload download = track(tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket(BUCKET).key(KEY)
                                                                                     .range(range))
                                                            .destination(destination)));
        awaitFileLength(FIRST_PAUSE, "before the first pause");
        ResumableFileDownload firstPause = download.pause();

        // 2. Resume, paused again with SECOND_PAUSE bytes on disk.
        int firstGetOfFirstResume = server.getRanges().size();
        server.holdAt(fileStart + SECOND_PAUSE);
        FileDownload firstResume = track(tm.resumeDownloadFile(firstPause));
        awaitFileLength(SECOND_PAUSE, "before the second pause");
        ResumableFileDownload secondPause = firstResume.pause();

        // 3. Resume again and let it finish.
        int firstGetOfSecondResume = server.getRanges().size();
        server.holdNothing();
        track(tm.resumeDownloadFile(secondPause)).completionFuture().get(STATE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        List<String> ranges = server.getRanges();
        byte[] actual = Files.readAllBytes(destination);
        byte[] expected = Arrays.copyOfRange(CONTENT, (int) fileStart, (int) lastByte + 1);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(firstPause.bytesTransferred())
              .as("bytesTransferred of the first pause").isEqualTo(FIRST_PAUSE);
        softly.assertThat(secondPause.bytesTransferred())
              .as("bytesTransferred of the second pause").isEqualTo(SECOND_PAUSE);
        softly.assertThat(requestedOffset(ranges, firstGetOfFirstResume))
              .as("offset the first resume continued from (GETs: %s)", ranges).isEqualTo(fileStart + FIRST_PAUSE);
        softly.assertThat(requestedOffset(ranges, firstGetOfSecondResume))
              .as("offset the second resume continued from (GETs: %s)", ranges).isEqualTo(fileStart + SECOND_PAUSE);
        softly.assertThat(firstDifference(actual, expected))
              .as("first offset at which the destination file (%d bytes) differs from the requested bytes (%d bytes)",
                  actual.length, expected.length)
              .isEqualTo(-1);
        softly.assertAll();
    }

    private FileDownload track(FileDownload download) {
        downloads.add(download);
        return download;
    }

    /**
     * Waits for the destination file to hold exactly {@code expected} bytes. The server sends nothing past the current
     * limit, so this state is final once reached; a file that grows past it means the download resumed from the wrong
     * offset.
     */
    private void awaitFileLength(long expected, String when) throws InterruptedException {
        File file = destination.toFile();
        long deadline = System.currentTimeMillis() + STATE_TIMEOUT_MILLIS;
        long length = file.length();
        while (length != expected) {
            if (length > expected) {
                fail("Destination file holds %d bytes %s but at most %d may have been written to it (GETs: %s)",
                     length, when, expected, server.getRanges());
            }
            if (System.currentTimeMillis() > deadline) {
                fail("Destination file holds %d bytes %s, expected %d (GETs: %s)",
                     length, when, expected, server.getRanges());
            }
            Thread.sleep(1);
            length = file.length();
        }
    }

    /**
     * The object offset requested by the GET at the given index, i.e. by the first GET of a resume.
     */
    private static Long requestedOffset(List<String> ranges, int index) {
        if (index >= ranges.size()) {
            return null;
        }
        String range = ranges.get(index);
        if (range == null) {
            return 0L;
        }
        String spec = range.substring("bytes=".length());
        int dash = spec.indexOf('-');
        return dash == 0 ? OBJECT_SIZE - Long.parseLong(spec.substring(1)) : Long.parseLong(spec.substring(0, dash));
    }

    private static int firstDifference(byte[] actual, byte[] expected) {
        int common = Math.min(actual.length, expected.length);
        for (int i = 0; i < common; i++) {
            if (actual[i] != expected[i]) {
                return i;
            }
        }
        return actual.length == expected.length ? -1 : common;
    }

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }

    private static byte[] randomContent(int size) {
        byte[] bytes = new byte[size];
        new Random(42).nextBytes(bytes);
        return bytes;
    }

    /**
     * A minimal S3-like HTTP server serving a single object, which only ever sends the bytes of the object that lie below a
     * movable limit. A response that reaches the limit flushes what it has sent and then blocks, so a client can never get
     * further than the limit no matter how fast it is. That turns "pause the download somewhere in the middle" from a race
     * into a fixed state: once the client has written everything up to the limit, nothing else can arrive.
     * <p>
     * Responses held at the limit are abandoned (their connection is closed without sending the rest of the body) when the
     * limit is moved, which tests must only do after the client has paused, i.e. after the client gave up on them.
     * <p>
     * Supports HEAD and GET with no Range, {@code bytes=a-b}, {@code bytes=a-} and {@code bytes=-n}, and records the Range
     * header of every GET it receives.
     */
    private static final class GatedObjectServer implements AutoCloseable {
        private static final int CHUNK_SIZE = 1024;
        private static final long HOLD_TIMEOUT_SECONDS = 60;
        private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

        private final byte[] content;
        private final String eTag;
        private final String lastModified;
        private final HttpServer server;
        private final ExecutorService executor;
        private final List<String> getRanges = Collections.synchronizedList(new ArrayList<>());

        private volatile Hold hold = new Hold(Long.MAX_VALUE);

        GatedObjectServer(byte[] content, String eTag, Instant lastModified) throws IOException {
            this.content = content;
            this.eTag = eTag;
            this.lastModified = HTTP_DATE.format(lastModified);
            this.executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "gated-object-server");
                t.setDaemon(true);
                return t;
            });
            this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            // Handlers block, so they must not run on the server's dispatcher thread.
            this.server.setExecutor(executor);
            this.server.createContext("/", this::handle);
            this.server.start();
        }

        URI endpoint() {
            return URI.create("http://localhost:" + server.getAddress().getPort());
        }

        /**
         * Only send bytes below {@code objectOffset} from now on. Abandons every response held at the previous limit.
         */
        void holdAt(long objectOffset) {
            Hold previous = hold;
            hold = new Hold(objectOffset);
            previous.abandon.countDown();
        }

        /**
         * Send everything from now on. Abandons every response held at the previous limit.
         */
        void holdNothing() {
            holdAt(Long.MAX_VALUE);
        }

        /**
         * Forgets the recorded GETs and sends everything from now on, abandoning any held response.
         */
        void reset() {
            holdNothing();
            getRanges.clear();
        }

        /**
         * The Range header of every GET received so far, in order, with {@code null} for a GET without one.
         */
        List<String> getRanges() {
            synchronized (getRanges) {
                return new ArrayList<>(getRanges);
            }
        }

        @Override
        public void close() {
            hold.abandon.countDown();
            server.stop(0);
            executor.shutdownNow();
        }

        private void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                String range = exchange.getRequestHeaders().getFirst("Range");
                if ("GET".equals(method)) {
                    getRanges.add(range);
                } else if (!"HEAD".equals(method)) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                long size = content.length;
                long start = 0;
                long end = size - 1;
                if (range != null) {
                    long[] resolved = resolve(range, size);
                    if (resolved == null) {
                        exchange.getResponseHeaders().set("Content-Range", "bytes */" + size);
                        exchange.sendResponseHeaders(416, -1);
                        return;
                    }
                    start = resolved[0];
                    end = resolved[1];
                }
                long length = end - start + 1;
                int status = range == null ? 200 : 206;

                Headers headers = exchange.getResponseHeaders();
                headers.set("ETag", eTag);
                headers.set("Last-Modified", lastModified);
                headers.set("Accept-Ranges", "bytes");
                if (range != null) {
                    headers.set("Content-Range", "bytes " + start + "-" + end + "/" + size);
                }

                if ("HEAD".equals(method)) {
                    // The JDK server does not add Content-Length for HEAD, so a manually set one is sent as-is.
                    headers.set("Content-Length", Long.toString(length));
                    exchange.sendResponseHeaders(status, -1);
                    return;
                }

                Hold current = hold;
                if (start >= current.limit) {
                    // Nothing of this response may be sent yet, not even the headers.
                    current.awaitAbandon();
                    return;
                }

                exchange.sendResponseHeaders(status, length);
                OutputStream body = exchange.getResponseBody();
                long position = start;
                while (position <= end) {
                    current = hold;
                    if (position >= current.limit) {
                        body.flush();
                        current.awaitAbandon();
                        return;
                    }
                    int count = (int) Math.min(CHUNK_SIZE, Math.min(end + 1, current.limit) - position);
                    body.write(content, (int) position, count);
                    position += count;
                }
                body.flush();
            } catch (IOException e) {
                // The client went away, e.g. because it paused. Nothing to do.
            } finally {
                closeQuietly(exchange);
            }
        }

        /**
         * Resolves a single "bytes=" range to inclusive [start, end] within an object of the given size, the way S3 does, or
         * returns null if it is not satisfiable.
         */
        private static long[] resolve(String range, long size) {
            String spec = range.substring("bytes=".length());
            int dash = spec.indexOf('-');
            long start;
            long end;
            if (dash == 0) {
                long suffix = Long.parseLong(spec.substring(1));
                start = Math.max(0, size - suffix);
                end = size - 1;
            } else {
                start = Long.parseLong(spec.substring(0, dash));
                String endPart = spec.substring(dash + 1);
                end = endPart.isEmpty() ? size - 1 : Math.min(Long.parseLong(endPart), size - 1);
            }
            return start > end ? null : new long[] {start, end};
        }

        private static void closeQuietly(HttpExchange exchange) {
            try {
                exchange.close();
            } catch (RuntimeException e) {
                // Closing a response that was cut short complains about the missing bytes, which is the point.
            }
        }

        private static final class Hold {
            private final long limit;
            private final CountDownLatch abandon = new CountDownLatch(1);

            private Hold(long limit) {
                this.limit = limit;
            }

            private void awaitAbandon() {
                try {
                    abandon.await(HOLD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
