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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
import software.amazon.awssdk.transfer.s3.model.FailedFileDownload;

/**
 * Exercises a directory download that contains a key resolving outside the destination, against both the Java-based and the
 * CRT-based S3 clients. The refused key is reported against a placeholder destination, and retrying that request verbatim
 * or deleting its destination must not touch any file under the destination directory.
 */
@WireMockTest
public class DownloadDirectoryRefusedKeyWireMockTest {
    private static final String BUCKET = "bucket";
    private static final String REFUSED_KEY = "../escape.txt";

    private Path workDirectory;
    private Path destination;
    private S3AsyncClient s3;
    private S3TransferManager tm;

    @BeforeEach
    void setUp() throws IOException {
        workDirectory = Files.createTempDirectory("tm-refused-key");
        destination = workDirectory.resolve("dest");
        Files.createDirectories(destination);
    }

    @AfterEach
    void tearDown() {
        if (tm != null) {
            tm.close();
        }
        if (s3 != null) {
            s3.close();
        }
        FileUtils.cleanUpTestDirectory(workDirectory);
    }

    @ParameterizedTest(name = "client={0}")
    @ValueSource(strings = {"java", "crt"})
    void downloadDirectory_refusedKey_retryAndCleanupOfFailedRequestTouchNothing(String clientType,
                                                                               WireMockRuntimeInfo wm) throws Exception {
        s3 = s3Client(clientType, URI.create(wm.getHttpBaseUrl()));
        tm = S3TransferManager.builder().s3Client(s3).transferDirectoryMaxConcurrency(1).build();

        stubListObjects("a.txt", REFUSED_KEY, "b.txt");
        stubObject("a.txt", "content-a");
        stubObject("b.txt", "content-b");
        // Serve any other object too, so a request that reached the server for the refused key could write a file.
        stubFor(head(anyUrl()).atPriority(10).willReturn(aResponse().withStatus(200)
                                                                    .withHeader("Content-Length", "7")
                                                                    .withHeader("ETag", "\"refused\"")));
        stubFor(get(anyUrl()).atPriority(10).willReturn(aResponse().withStatus(200)
                                                                   .withHeader("ETag", "\"refused\"")
                                                                   .withBody("refused")));

        CompletedDirectoryDownload completed =
            tm.downloadDirectory(r -> r.bucket(BUCKET).destination(destination))
              .completionFuture().get(30, TimeUnit.SECONDS);

        assertThat(completed.failedTransfers()).hasSize(1);
        FailedFileDownload failed = completed.failedTransfers().get(0);
        assertThat(failed.request().getObjectRequest().key()).isEqualTo(REFUSED_KEY);
        assertThat(failed.exception()).hasMessageContaining("Cannot download key " + REFUSED_KEY);
        assertDownloadedFilesIntact();

        Path placeholder = failed.request().destination();
        assertThatThrownBy(() -> tm.downloadFile(failed.request()).completionFuture().get(30, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class);
        assertThat(Files.exists(placeholder.getParent())).isFalse();
        assertDownloadedFilesIntact();

        assertThat(Files.deleteIfExists(placeholder)).isFalse();
        assertDownloadedFilesIntact();
    }

    private void assertDownloadedFilesIntact() throws IOException {
        try (Stream<Path> underDestination = Files.list(destination);
             Stream<Path> underWorkDirectory = Files.list(workDirectory)) {
            List<String> names = underDestination.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
            assertThat(names).containsExactly("a.txt", "b.txt");
            assertThat(underWorkDirectory.map(p -> p.getFileName().toString())).containsExactly("dest");
        }
        assertThat(destination.resolve("a.txt")).hasContent("content-a");
        assertThat(destination.resolve("b.txt")).hasContent("content-b");
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
                    .atPriority(1)
                    .willReturn(aResponse().withStatus(200).withBody(body)));
    }

    private static void stubObject(String key, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        stubFor(head(urlPathEqualTo("/" + BUCKET + "/" + key))
                    .atPriority(1)
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("Content-Length", String.valueOf(bytes.length))
                                           .withHeader("ETag", "\"" + key + "\"")));
        stubFor(get(urlPathEqualTo("/" + BUCKET + "/" + key))
                    .atPriority(1)
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("ETag", "\"" + key + "\"")
                                           .withBody(bytes)));
    }
}
