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
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

/**
 * Verifies that {@link S3TransferManager} uploads propagate the request-body business metric ({@code md/rb#<code>}) onto the user
 * agent of the request that carries the body, on the wire, across both TM flavors (Java-based and CRT-based) and the file, bytes,
 * and stream body types.
 */
public class TransferManagerUploadUserAgentBusinessMetricWireMockTest {
    private static final WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    private static final long PART_SIZE = 100L;
    private static Path smallFile;
    private static Path largeFile;

    @BeforeAll
    public static void setup() throws IOException {
        smallFile = Files.createTempFile("single-part", ".dat");
        writeTestFile(smallFile, PART_SIZE / 2);
        largeFile = Files.createTempFile("multi-part", ".dat");
        writeTestFile(largeFile, PART_SIZE * 2);
        wireMock.start();
    }

    @AfterAll
    public static void teardown() throws IOException {
        Files.deleteIfExists(smallFile);
        Files.deleteIfExists(largeFile);
        wireMock.stop();
    }

    private static Stream<Arguments> uploadMatrix() {
        return Stream.of(
            Arguments.of("JAVA / single-part file -> PutObject", TmFlavor.JAVA, Scenario.SINGLE_PART_FILE, "md/rb#f"),
            Arguments.of("JAVA / single-part bytes -> PutObject", TmFlavor.JAVA, Scenario.SINGLE_PART_BYTES, "md/rb#b"),
            Arguments.of("JAVA / single-part stream -> PutObject", TmFlavor.JAVA, Scenario.SINGLE_PART_STREAM, "md/rb#s"),
            Arguments.of("JAVA / multipart file -> UploadPart", TmFlavor.JAVA, Scenario.MULTIPART_FILE, "md/rb#f"),
            Arguments.of("CRT / single-part file -> PutObject", TmFlavor.CRT, Scenario.SINGLE_PART_FILE, "md/rb#f"),
            Arguments.of("CRT / single-part bytes -> PutObject", TmFlavor.CRT, Scenario.SINGLE_PART_BYTES, "md/rb#b"),
            Arguments.of("CRT / single-part stream -> PutObject", TmFlavor.CRT, Scenario.SINGLE_PART_STREAM, "md/rb#s"),
            Arguments.of("CRT / multipart file -> UploadPart", TmFlavor.CRT, Scenario.MULTIPART_FILE, "md/rb#f"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("uploadMatrix")
    void upload_userAgentCarriesExpectedBodyMetric(String description, TmFlavor flavor, Scenario scenario,
                                                   String expectedMetric) {
        wireMock.resetAll();
        scenario.stub();

        try (S3TransferManager tm = flavor.create(scenario.multipartEnabled())) {
            scenario.runUpload(tm);
        }

        wireMock.verify(scenario.verification().withHeader("User-Agent", containing(expectedMetric)));
    }

    private static void uploadFile(S3TransferManager tm, Path source) {
        tm.uploadFile(u -> u.source(source).putObjectRequest(put -> put.bucket("bucket").key("key")))
          .completionFuture().join();
    }

    private static void upload(S3TransferManager tm, AsyncRequestBody body) {
        tm.upload(UploadRequest.builder()
                               .requestBody(body)
                               .putObjectRequest(put -> put.bucket("bucket").key("key"))
                               .build())
          .completionFuture().join();
    }

    private static S3AsyncClientBuilder javaClientBuilder() {
        return S3AsyncClient.builder()
                            .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("akid", "skid")))
                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                            .region(Region.US_EAST_1)
                            .forcePathStyle(true);
    }

    private static S3CrtAsyncClientBuilder crtClientBuilder() {
        return S3AsyncClient.crtBuilder()
                            .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("akid", "skid")))
                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                            .region(Region.US_EAST_1)
                            .forcePathStyle(true);
    }

    private static void stubSinglePutSuccess() {
        wireMock.stubFor(put(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    private static void stubSuccessfulMultipartUpload() {
        String mpuInitBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                             + "<InitiateMultipartUploadResult>\n"
                             + "   <Bucket>bucket</Bucket>\n"
                             + "   <Key>key</Key>\n"
                             + "   <UploadId>uploadId</UploadId>\n"
                             + "</InitiateMultipartUploadResult>";
        wireMock.stubFor(post(urlEqualTo("/bucket/key?uploads"))
                             .willReturn(aResponse().withStatus(200).withBody(mpuInitBody)));

        for (int i = 1; i <= 2; i++) {
            // The CRT native client reads the part ETag from the response header; the Java client reads it from the body.
            // Provide both so the one stub serves both flavors.
            wireMock.stubFor(put(urlEqualTo("/bucket/key?partNumber=" + i + "&uploadId=uploadId"))
                                 .willReturn(aResponse()
                                                 .withStatus(200)
                                                 .withHeader("ETag", "\"etag" + i + "\"")
                                                 .withBody("<Part><PartNumber>" + i +
                                                           "</PartNumber><ETag>\"etag" + i + "\"</ETag></Part>")));
        }

        String mpuCompleteBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<CompleteMultipartUploadResult>\n"
                                 + "   <Bucket>bucket</Bucket>\n"
                                 + "   <Key>key</Key>\n"
                                 + "   <ETag>etag</ETag>\n"
                                 + "</CompleteMultipartUploadResult>";
        wireMock.stubFor(post(urlEqualTo("/bucket/key?uploadId=uploadId"))
                             .willReturn(aResponse().withStatus(200).withBody(mpuCompleteBody)));
    }

    private static void writeTestFile(Path file, long size) {
        try (OutputStream os = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
            byte[] buff = new byte[4096];
            long remaining = size;
            while (remaining != 0) {
                int writeLen = (int) Math.min(remaining, buff.length);
                os.write(buff, 0, writeLen);
                remaining -= writeLen;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    enum TmFlavor {
        JAVA {
            @Override
            S3TransferManager create(boolean multipartEnabled) {
                S3AsyncClientBuilder builder = javaClientBuilder();
                if (multipartEnabled) {
                    builder.multipartEnabled(true)
                           .multipartConfiguration(c -> c.thresholdInBytes(PART_SIZE)
                                                         .minimumPartSizeInBytes(PART_SIZE)
                                                         .apiCallBufferSizeInBytes(PART_SIZE * 4));
                }
                return S3TransferManager.builder().s3Client(builder.build()).build();
            }
        },
        CRT {
            @Override
            S3TransferManager create(boolean multipartEnabled) {
                S3CrtAsyncClientBuilder builder = crtClientBuilder();
                if (multipartEnabled) {
                    // The CRT client always chunks natively; a small part size forces >1 UploadPart against WireMock.
                    builder.minimumPartSizeInBytes(PART_SIZE).thresholdInBytes(PART_SIZE);
                }
                return S3TransferManager.builder().s3Client(builder.build()).build();
            }
        };

        abstract S3TransferManager create(boolean multipartEnabled);
    }

    enum Scenario {
        SINGLE_PART_FILE(false) {
            @Override
            void stub() {
                stubSinglePutSuccess();
            }

            @Override
            void runUpload(S3TransferManager tm) {
                uploadFile(tm, smallFile);
            }

            @Override
            RequestPatternBuilder verification() {
                return putRequestedFor(urlPathEqualTo("/bucket/key"));
            }
        },
        SINGLE_PART_BYTES(false) {
            @Override
            void stub() {
                stubSinglePutSuccess();
            }

            @Override
            void runUpload(S3TransferManager tm) {
                upload(tm, AsyncRequestBody.fromString("hello world"));
            }

            @Override
            RequestPatternBuilder verification() {
                return putRequestedFor(urlPathEqualTo("/bucket/key"));
            }
        },
        SINGLE_PART_STREAM(false) {
            @Override
            void stub() {
                stubSinglePutSuccess();
            }

            @Override
            void runUpload(S3TransferManager tm) {
                upload(tm, AsyncRequestBody.fromInputStream(new ByteArrayInputStream(new byte[16]), 16L,
                                                            Executors.newSingleThreadExecutor()));
            }

            @Override
            RequestPatternBuilder verification() {
                return putRequestedFor(urlPathEqualTo("/bucket/key"));
            }
        },
        MULTIPART_FILE(true) {
            @Override
            void stub() {
                stubSuccessfulMultipartUpload();
            }

            @Override
            void runUpload(S3TransferManager tm) {
                uploadFile(tm, largeFile);
            }

            @Override
            RequestPatternBuilder verification() {
                return putRequestedFor(urlPathEqualTo("/bucket/key")).withQueryParam("partNumber", containing("1"));
            }
        };

        private final boolean multipartEnabled;

        Scenario(boolean multipartEnabled) {
            this.multipartEnabled = multipartEnabled;
        }

        boolean multipartEnabled() {
            return multipartEnabled;
        }

        abstract void stub();

        abstract void runUpload(S3TransferManager tm);

        abstract RequestPatternBuilder verification();
    }
}
