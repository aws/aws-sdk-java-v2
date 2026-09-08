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

package software.amazon.awssdk.services.s3.internal.multipart;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Covers the paths where a download destination is rejected while the response transformer is split, rather than while it is
 * prepared: the multipart client and the presigned URL multipart helper.
 */
@WireMockTest
public class FileDestinationPreflightSplitTest {

    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String PRE_EXISTING = "pre-existing contents";

    @TempDir
    Path tempDir;

    private Path destination;
    private URI endpoint;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMock) throws IOException {
        endpoint = URI.create(wireMock.getHttpBaseUrl());
        destination = tempDir.resolve("destination.bin");
        Files.write(destination, PRE_EXISTING.getBytes(StandardCharsets.UTF_8));
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(206)
                                                    .withHeader("Content-Range", "bytes 0-3/8")
                                                    .withHeader("ETag", "\"etag\"")
                                                    .withBody("abcd")));
    }

    @Test
    void multipartClient_existingDestination_failsWithoutSendingAnyPartRequest() {
        S3AsyncClient client = multipartClient();

        CompletableFuture<GetObjectResponse> future =
            client.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build(),
                             AsyncResponseTransformer.toFile(destination));

        assertThat(catchThrowable(future::join)).hasRootCauseInstanceOf(FileAlreadyExistsException.class);
        assertThat(requestCount()).isZero();
        assertThat(contentsOf(destination)).isEqualTo(PRE_EXISTING);
    }

    @Test
    void presignedUrlMultipart_existingDestination_failsWithoutSendingAnyRequest() throws Exception {
        S3AsyncClient client = multipartClient();
        URL url = presignedUrl();

        CompletableFuture<GetObjectResponse> future =
            client.presignedUrlExtension()
                  .getObject(r -> r.presignedUrl(url), AsyncResponseTransformer.toFile(destination));

        assertThat(catchThrowable(future::join)).hasRootCauseInstanceOf(FileAlreadyExistsException.class);
        assertThat(requestCount()).isZero();
        assertThat(contentsOf(destination)).isEqualTo(PRE_EXISTING);
    }

    @Test
    void multipartClient_freshDestination_stillDownloads() throws IOException {
        Files.delete(destination);
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("Content-Length", "4")
                                                    .withHeader("ETag", "\"etag\"")
                                                    .withBody("abcd")));
        S3AsyncClient client = multipartClient();

        client.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build(),
                         AsyncResponseTransformer.toFile(destination)).join();

        assertThat(contentsOf(destination)).isEqualTo("abcd");
    }

    private URL presignedUrl() throws MalformedURLException {
        return new URL(endpoint + "/" + BUCKET + "/" + KEY + "?X-Amz-Signature=stub");
    }

    private S3AsyncClient multipartClient() {
        return S3AsyncClient.builder()
                            .credentialsProvider(
                                StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                            .region(Region.US_EAST_1)
                            .endpointOverride(endpoint)
                            .forcePathStyle(true)
                            .multipartEnabled(true)
                            .build();
    }

    private static int requestCount() {
        return WireMock.findAll(anyRequestedFor(urlMatching(".*"))).size();
    }

    private static String contentsOf(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unreadable: " + e + ">";
        }
    }
}
