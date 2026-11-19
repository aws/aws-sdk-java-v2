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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

@WireMockTest
public class DownloadDirectoryTempFileWireMockTest {
    private static final String BUCKET = "bucket";

    private Path destination;
    private S3AsyncClient s3;
    private S3TransferManager tm;

    @BeforeEach
    void setUp() throws IOException {
        destination = Files.createTempDirectory("tm-temp-file");
    }

    @AfterEach
    void tearDown() {
        if (tm != null) {
            tm.close();
        }
        if (s3 != null) {
            s3.close();
        }
        FileUtils.cleanUpTestDirectory(destination);
    }

    @ParameterizedTest(name = "client={0}")
    @ValueSource(strings = {"java", "java-multipart", "crt"})
    void downloadDirectory_withPerFileListener_listenerSeesRealDestinationAndNoTempFileRemains(String clientType,
                                                                                              WireMockRuntimeInfo wm)
        throws Exception {
        s3 = s3Client(clientType, URI.create(wm.getHttpBaseUrl()));
        tm = S3TransferManager.builder().s3Client(s3).build();
        stubListObjects("a.txt", "sub/b.txt");
        stubObject("a.txt", "content-a");
        stubObject("sub/b.txt", "content-b");

        List<Path> initiated = new CopyOnWriteArrayList<>();
        List<Path> completed = new CopyOnWriteArrayList<>();
        List<String> contentAtCompletion = new CopyOnWriteArrayList<>();
        TransferListener listener = new TransferListener() {
            @Override
            public void transferInitiated(Context.TransferInitiated context) {
                initiated.add(((DownloadFileRequest) context.request()).destination());
            }

            @Override
            public void transferComplete(Context.TransferComplete context) {
                Path completedDestination = ((DownloadFileRequest) context.request()).destination();
                contentAtCompletion.add(readOrMissing(completedDestination));
                completed.add(completedDestination);
            }
        };

        CompletedDirectoryDownload result =
            tm.downloadDirectory(r -> r.bucket(BUCKET)
                                       .destination(destination)
                                       .downloadFileRequestTransformer(f -> f.addTransferListener(listener)))
              .completionFuture().get(30, TimeUnit.SECONDS);

        assertThat(result.failedTransfers()).isEmpty();
        Path a = destination.resolve("a.txt");
        Path b = destination.resolve("sub").resolve("b.txt");
        assertThat(initiated).containsExactlyInAnyOrder(a, b);
        // The multipart client does not deliver transferComplete against these stubs, for downloadFile as well.
        if (!"java-multipart".equals(clientType)) {
            // transferComplete can be delivered after the directory download's future completes, but only once the file
            // is in place.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (completed.size() < 2 && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertThat(completed).containsExactlyInAnyOrder(a, b);
            assertThat(contentAtCompletion).containsExactlyInAnyOrder("content-a", "content-b");
        }
        assertThat(a).hasContent("content-a");
        assertThat(b).hasContent("content-b");
        try (Stream<Path> files = Files.walk(destination)) {
            assertThat(files.filter(Files::isRegularFile).map(destination::relativize).map(Path::toString)
                            .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("a.txt", "sub" + destination.getFileSystem().getSeparator() + "b.txt");
        }
    }

    @ParameterizedTest(name = "client={0}")
    @ValueSource(strings = {"java", "java-multipart", "crt"})
    void downloadDirectory_keysResolvingToSameFile_fileHoldsOneCompleteObject(String clientType, WireMockRuntimeInfo wm)
        throws Exception {
        s3 = s3Client(clientType, URI.create(wm.getHttpBaseUrl()));
        tm = S3TransferManager.builder().s3Client(s3).build();
        String longBody = "C2:x//y.txt";
        String shortBody = "C2:x/y.txt";
        stubListObjects("x//y.txt", "x/y.txt");
        // Both responses send their headers at once and their bodies later, the longer one first, so every client has
        // opened its file for both keys before either body is written.
        stubObject("x//y.txt", longBody.getBytes(StandardCharsets.UTF_8), 300);
        stubObject("x/y.txt", shortBody.getBytes(StandardCharsets.UTF_8), 900);

        CompletedDirectoryDownload result = tm.downloadDirectory(r -> r.bucket(BUCKET).destination(destination))
                                              .completionFuture().get(30, TimeUnit.SECONDS);

        assertThat(result.failedTransfers()).isEmpty();
        assertThat(readOrMissing(destination.resolve("x").resolve("y.txt"))).isIn(longBody, shortBody);
        assertThat(regularFiles()).containsExactly("x" + destination.getFileSystem().getSeparator() + "y.txt");
    }

    @ParameterizedTest(name = "client={0}")
    @ValueSource(strings = {"java", "java-multipart", "crt"})
    void downloadDirectory_cancelledMidDownload_leavesNoFiles(String clientType, WireMockRuntimeInfo wm) throws Exception {
        s3 = s3Client(clientType, URI.create(wm.getHttpBaseUrl()));
        tm = S3TransferManager.builder().s3Client(s3).build();
        stubListObjects("big.bin");
        stubObject("big.bin", new byte[2 * 1024 * 1024], 10_000);

        CompletableFuture<CompletedDirectoryDownload> download =
            tm.downloadDirectory(r -> r.bucket(BUCKET).destination(destination)).completionFuture();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (regularFiles().isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(regularFiles()).as("the download started writing").isNotEmpty();

        download.cancel(true);
        // Gives an in-flight write the chance to land after the cancel.
        Thread.sleep(1_000);

        assertThat(regularFiles()).isEmpty();
    }

    @ParameterizedTest(name = "client={0}")
    @ValueSource(strings = {"java", "java-multipart", "crt"})
    void downloadFileTo_beforeCompletionRunsBeforeDownloadAndListenerComplete(String clientType, WireMockRuntimeInfo wm)
        throws Exception {
        s3 = s3Client(clientType, URI.create(wm.getHttpBaseUrl()));
        tm = S3TransferManager.builder().s3Client(s3).build();
        stubObject("a.txt", "content-a");
        Path writePath = destination.resolve("a.txt.tmp");
        AtomicBoolean ran = new AtomicBoolean();
        List<Boolean> ranAtTransferComplete = new CopyOnWriteArrayList<>();
        TransferListener listener = new TransferListener() {
            @Override
            public void transferComplete(Context.TransferComplete context) {
                ranAtTransferComplete.add(ran.get());
            }
        };
        DownloadFileRequest request = DownloadFileRequest.builder()
                                                         .getObjectRequest(g -> g.bucket(BUCKET).key("a.txt"))
                                                         .destination(destination.resolve("a.txt"))
                                                         .addTransferListener(listener)
                                                         .build();

        ((GenericS3TransferManager) tm).downloadFileTo(request, writePath, () -> {
            assertThat(writePath).hasContent("content-a");
            ran.set(true);
        }).completionFuture().get(30, TimeUnit.SECONDS);

        assertThat(ran).isTrue();
        // The multipart client does not deliver transferComplete against these stubs, for downloadFile as well.
        if (!"java-multipart".equals(clientType)) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (ranAtTransferComplete.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertThat(ranAtTransferComplete).containsExactly(true);
        }
    }

    @ParameterizedTest(name = "client={0}")
    @ValueSource(strings = {"java", "java-multipart", "crt"})
    void downloadFileTo_beforeCompletionThrows_downloadAndListenerFail(String clientType, WireMockRuntimeInfo wm)
        throws Exception {
        s3 = s3Client(clientType, URI.create(wm.getHttpBaseUrl()));
        tm = S3TransferManager.builder().s3Client(s3).build();
        stubObject("a.txt", "content-a");
        RuntimeException moveFailure = new RuntimeException("move failed");
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();
        TransferListener listener = new TransferListener() {
            @Override
            public void transferComplete(Context.TransferComplete context) {
                completed.set(true);
            }

            @Override
            public void transferFailed(Context.TransferFailed context) {
                failures.add(context.exception());
            }
        };
        DownloadFileRequest request = DownloadFileRequest.builder()
                                                         .getObjectRequest(g -> g.bucket(BUCKET).key("a.txt"))
                                                         .destination(destination.resolve("a.txt"))
                                                         .addTransferListener(listener)
                                                         .build();

        CompletableFuture<CompletedFileDownload> download =
            ((GenericS3TransferManager) tm).downloadFileTo(request, destination.resolve("a.txt.tmp"), () -> {
                throw moveFailure;
            }).completionFuture();

        assertThatThrownBy(() -> download.get(30, TimeUnit.SECONDS)).hasCause(moveFailure);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (failures.isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(failures).containsExactly(moveFailure);
        assertThat(completed).isFalse();
    }

    @ParameterizedTest(name = "client={0}")
    @ValueSource(strings = {"java", "java-multipart", "crt"})
    void downloadDirectory_emptyObject_createsEmptyFile(String clientType, WireMockRuntimeInfo wm) throws Exception {
        s3 = s3Client(clientType, URI.create(wm.getHttpBaseUrl()));
        tm = S3TransferManager.builder().s3Client(s3).build();
        stubListObjects("empty.bin");
        String objectPath = "/" + BUCKET + "/empty.bin";
        stubFor(head(urlPathEqualTo(objectPath))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Length", "0").withHeader("ETag", "\"e\"")));
        // S3 rejects a ranged GET of an empty object, and the CRT-based client retries such a request without the range.
        stubFor(get(urlPathEqualTo(objectPath)).withHeader("Range", matching(".+")).atPriority(1)
                    .willReturn(aResponse().withStatus(416)
                                           .withHeader("Content-Type", "application/xml")
                                           .withHeader("Content-Range", "bytes */0")
                                           .withBody("<Error><Code>InvalidRange</Code><ActualObjectSize>0</ActualObjectSize></Error>")));
        stubFor(get(urlPathEqualTo(objectPath)).atPriority(2)
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Length", "0").withHeader("ETag", "\"e\"")));

        CompletedDirectoryDownload result = tm.downloadDirectory(r -> r.bucket(BUCKET).destination(destination))
                                              .completionFuture().get(30, TimeUnit.SECONDS);

        assertThat(result.failedTransfers()).isEmpty();
        assertThat(destination.resolve("empty.bin")).isEmptyFile();
        assertThat(regularFiles()).containsExactly("empty.bin");
    }

    private List<String> regularFiles() throws IOException {
        try (Stream<Path> files = Files.walk(destination)) {
            return files.filter(Files::isRegularFile).map(destination::relativize).map(Path::toString)
                        .collect(Collectors.toList());
        }
    }

    private static String readOrMissing(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<missing>";
        }
    }

    private static S3AsyncClient s3Client(String clientType, URI endpoint) {
        StaticCredentialsProvider credentials =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret"));
        if ("crt".equals(clientType)) {
            return S3AsyncClient.crtBuilder()
                                .region(Region.US_EAST_1)
                                .endpointOverride(endpoint)
                                .forcePathStyle(true)
                                .credentialsProvider(credentials)
                                .build();
        }
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(endpoint)
                            .forcePathStyle(true)
                            .multipartEnabled("java-multipart".equals(clientType))
                            .credentialsProvider(credentials)
                            .build();
    }

    private static void stubListObjects(String... keys) {
        StringBuilder contents = new StringBuilder();
        for (String key : keys) {
            contents.append("<Contents><Key>").append(key).append("</Key><Size>9</Size></Contents>");
        }
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                      + "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                      + "<Name>" + BUCKET + "</Name><Prefix></Prefix><KeyCount>" + keys.length + "</KeyCount>"
                      + "<MaxKeys>1000</MaxKeys><IsTruncated>false</IsTruncated>"
                      + contents
                      + "</ListBucketResult>";
        stubFor(get(urlPathMatching("/" + BUCKET + "/?"))
                    .withQueryParam("list-type", equalTo("2"))
                    .willReturn(aResponse().withStatus(200).withBody(body)));
    }

    private static void stubObject(String key, String content) {
        stubObject(key, content.getBytes(StandardCharsets.UTF_8), 0);
    }

    private static void stubObject(String key, byte[] bytes, int bodyDelayMillis) {
        stubFor(head(urlPathEqualTo("/" + BUCKET + "/" + key))
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("Content-Length", String.valueOf(bytes.length))
                                           .withHeader("ETag", "\"" + key.replace('/', '-') + "\"")));
        stubFor(get(urlPathEqualTo("/" + BUCKET + "/" + key))
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("ETag", "\"" + key.replace('/', '-') + "\"")
                                           .withHeader("Content-Length", String.valueOf(bytes.length))
                                           .withHeader("Content-Range", "bytes 0-" + (bytes.length - 1) + "/" + bytes.length)
                                           .withBody(bytes)
                                           .withChunkedDribbleDelay(bodyDelayMillis > 0 ? 2 : 1, bodyDelayMillis)));
    }
}
