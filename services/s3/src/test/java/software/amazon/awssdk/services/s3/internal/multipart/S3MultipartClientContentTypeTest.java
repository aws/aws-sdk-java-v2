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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Functional tests to verify content-type propagation from AsyncRequestBody for multipart uploads.
 * <p>
 * These tests verify that when using S3AsyncClient with multipartEnabled(true), the content-type
 * from AsyncRequestBody is correctly propagated to the CreateMultipartUpload request, ensuring
 * consistent behavior between single-part and multipart uploads.
 */
@WireMockTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3MultipartClientContentTypeTest {

    private static final String BUCKET = "test-bucket";
    private static final String KEY = "test-key";
    private static final long MULTIPART_THRESHOLD = 10 * 1024; // 10KB
    private static final long PART_SIZE = 5 * 1024; // 5KB

    private static final String CREATE_MULTIPART_RESPONSE =
        "<InitiateMultipartUploadResult><Bucket>" + BUCKET + "</Bucket>"
        + "<Key>" + KEY + "</Key><UploadId>upload-id</UploadId></InitiateMultipartUploadResult>";

    private static final String COMPLETE_MULTIPART_RESPONSE =
        "<CompleteMultipartUploadResult><Bucket>" + BUCKET + "</Bucket>"
        + "<Key>" + KEY + "</Key><ETag>\"etag\"</ETag></CompleteMultipartUploadResult>";

    private S3AsyncClient s3Client;

    private static File createTempFile(String prefix, String suffix, int size) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        Files.write(file.toPath(), new byte[size]);
        return file;
    }

    static Stream<Arguments> uploadScenarios() throws IOException {
        File singlePartHtmlFile = createTempFile("single", ".html", 5 * 1024);
        File multipartHtmlFile = createTempFile("multi", ".html", 15 * 1024);

        return Stream.of(
            Arguments.of("singlePart_htmlFile", singlePartHtmlFile, false, "text/html"),
            Arguments.of("multipart_htmlFile", multipartHtmlFile, true, "text/html")
        );
    }

    @BeforeEach
    void setup(WireMockRuntimeInfo wiremock) {
        s3Client = S3AsyncClient.builder()
                                .region(Region.US_EAST_1)
                                .endpointOverride(URI.create(wiremock.getHttpBaseUrl()))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("key", "secret")))
                                .forcePathStyle(true)
                                .multipartEnabled(true)
                                .multipartConfiguration(c -> c.thresholdInBytes(MULTIPART_THRESHOLD)
                                                              .minimumPartSizeInBytes(PART_SIZE))
                                .build();
    }

    @AfterEach
    void teardown() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("uploadScenarios")
    void putObject_withContentTypeFromRequestBody_shouldPropagateContentType(String scenario, File file,
                                                                              boolean isMultipart,
                                                                              String expectedContentType) {
        stubSuccessfulResponses(isMultipart);

        s3Client.putObject(r -> r.bucket(BUCKET).key(KEY), AsyncRequestBody.fromFile(file)).join();

        if (isMultipart) {
            assertCreateMultipartUploadContentType(expectedContentType);
        } else {
            assertPutObjectContentType(expectedContentType);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("uploadScenarios")
    void putObject_withExplicitContentType_shouldNotOverride(String scenario, File file,
                                                              boolean isMultipart, String ignoredContentType) {
        stubSuccessfulResponses(isMultipart);
        String explicitContentType = "custom/type";

        s3Client.putObject(r -> r.bucket(BUCKET).key(KEY).contentType(explicitContentType),
                          AsyncRequestBody.fromFile(file)).join();

        if (isMultipart) {
            assertCreateMultipartUploadContentType(explicitContentType);
        } else {
            assertPutObjectContentType(explicitContentType);
        }
    }

    @Test
    void putObject_withNullContentTypeFromRequestBody_shouldUseDefaultContentType() {
        stubSuccessfulResponses(true);
        byte[] content = new byte[15 * 1024]; // 15KB - above threshold

        AsyncRequestBody bodyWithNullContentType = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of((long) content.length);
            }

            @Override
            public String contentType() {
                return null;
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                AsyncRequestBody.fromBytes(content).subscribe(subscriber);
            }
        };

        s3Client.putObject(r -> r.bucket(BUCKET).key(KEY), bodyWithNullContentType).join();

        // When AsyncRequestBody.contentType() is null, should use SDK default
        assertCreateMultipartUploadContentType("binary/octet-stream");
    }

    private void stubSuccessfulResponses(boolean isMultipart) {
        if (isMultipart) {
            stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody(CREATE_MULTIPART_RESPONSE)));
            stubFor(put(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", "\"etag\"")));
            stubFor(post(urlPathEqualTo("/" + BUCKET + "/" + KEY))
                        .withQueryParam("uploadId", containing("upload-id"))
                        .willReturn(aResponse().withStatus(200).withBody(COMPLETE_MULTIPART_RESPONSE)));
        } else {
            stubFor(put(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", "\"etag\"")));
        }
    }

    private void assertCreateMultipartUploadContentType(String expectedContentType) {
        List<LoggedRequest> requests = findAll(postRequestedFor(urlPathEqualTo("/" + BUCKET + "/" + KEY))
                                                   .withQueryParam("uploads", containing("")));
        assertThat(requests)
            .as("Expected CreateMultipartUpload request")
            .hasSize(1);
        assertThat(requests.get(0).getHeader("Content-Type"))
            .as("Content-Type header in CreateMultipartUpload request")
            .isEqualTo(expectedContentType);
    }

    private void assertPutObjectContentType(String expectedContentType) {
        List<LoggedRequest> requests = findAll(putRequestedFor(urlPathEqualTo("/" + BUCKET + "/" + KEY)));
        assertThat(requests)
            .as("Expected PutObject request")
            .hasSize(1);
        assertThat(requests.get(0).getHeader("Content-Type"))
            .as("Content-Type header in PutObject request")
            .isEqualTo(expectedContentType);
    }
}
