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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumType;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

class MultipartClientChecksumTest {
    private static final WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    private static final long FILE_SIZE = 16 * 1024 * 1024L;
    private static Path testFile;
    private ChecksumCapturingInterceptor checksumCapturingInterceptor;
    private S3AsyncClient multipartS3;

    @BeforeAll
    public static void setup() throws IOException {
        testFile = Files.createTempFile("16mib", ".dat");
        writeTestFile(testFile, FILE_SIZE);
        wireMock.start();
    }

    public static Stream<ChecksumAlgorithm> checksumAlgorithmParams() {
        List<ChecksumAlgorithm> checksumAlgorithms = new ArrayList<>(ChecksumAlgorithm.knownValues());
        checksumAlgorithms.add(null);
        return checksumAlgorithms.stream();
    }

    @BeforeEach
    void init() {
        this.checksumCapturingInterceptor = new ChecksumCapturingInterceptor();
        multipartS3 = S3AsyncClient.builder()
                                   .credentialsProvider(StaticCredentialsProvider.create(
                                       AwsBasicCredentials.create("akid", "skid")))
                                   .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                   .overrideConfiguration(c -> c.addExecutionInterceptor(checksumCapturingInterceptor))
                                   .region(Region.US_EAST_1)
                                   .multipartEnabled(true)
                                   .forcePathStyle(true)
                                   .build();
    }

    @AfterEach
    void reset() {
        multipartS3.close();
    }

    @AfterAll
    public static void teardown() throws IOException {
        Files.deleteIfExists(testFile);
        wireMock.stop();
    }

    @ParameterizedTest
    @MethodSource("checksumAlgorithmParams")
    public void multipartUpload_withChecksumAlgorithmAndNoChecksumValueProvided_shouldNotAddChecksumType(ChecksumAlgorithm checksumAlgorithm) {
        stubSuccessfulResponses();
        PutObjectRequest putObjectRequest = putObjectRequestBuilder().checksumAlgorithm(checksumAlgorithm).build();

        String expectedChecksumAlgo = checksumAlgorithm == null ? "CRC32" : checksumAlgorithm.toString();
        multipartS3.putObject(putObjectRequest, testFile).join();
        assertThat(checksumCapturingInterceptor.createMpuChecksumAlgorithm).isEqualTo(expectedChecksumAlgo);
        assertThat(checksumCapturingInterceptor.uploadPartChecksumAlgorithm).isEqualTo(expectedChecksumAlgo);
        assertThat(checksumCapturingInterceptor.createMpuChecksumType).isNull();
        assertThat(checksumCapturingInterceptor.completeMpuChecksumType).isNull();
        assertThat(checksumCapturingInterceptor.completeMpuMpObjectSize).isEqualTo(FILE_SIZE);
    }

    @ParameterizedTest
    @MethodSource("checksumAlgorithmParams")
    public void multipartUpload_withChecksumValueProvided_shouldUseSameAlgorithmForUploadAndAddChecksumTypeFullObject(ChecksumAlgorithm checksumAlgorithm) {
        stubSuccessfulResponses();

        PutObjectRequest.Builder requestBuilder = putObjectRequestBuilder();
        if (checksumAlgorithm != null) {
            switch (checksumAlgorithm) {
                case CRC32:
                    requestBuilder.checksumCRC32("checksumVal");
                    break;
                case SHA256:
                    requestBuilder.checksumSHA256("checksumVal");
                    break;
                case CRC32_C:
                    requestBuilder.checksumCRC32C("checksumVal");
                    break;
                case SHA1:
                    requestBuilder.checksumSHA1("checksumVal");
                    break;
                case CRC64_NVME:
                    requestBuilder.checksumCRC64NVME("checksumVal");
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported checksum algorithm: " + checksumAlgorithm);
            }
        }

        String expectedChecksumAlgo = checksumAlgorithm == null ? "CRC32" : checksumAlgorithm.toString();

        multipartS3.putObject(requestBuilder.build(), testFile).join();
        assertThat(checksumCapturingInterceptor.createMpuChecksumAlgorithm).isEqualTo(expectedChecksumAlgo);
        assertThat(checksumCapturingInterceptor.uploadPartChecksumAlgorithm).isEqualTo(expectedChecksumAlgo);
        if (checksumAlgorithm != null) {
            assertThat(checksumCapturingInterceptor.completeMpuHeaders.get("x-amz-checksum-" + expectedChecksumAlgo.toLowerCase(Locale.US))).contains(
                "checksumVal");

            assertThat(checksumCapturingInterceptor.createMpuChecksumType).isEqualTo(ChecksumType.FULL_OBJECT.toString());
            assertThat(checksumCapturingInterceptor.completeMpuChecksumType).isEqualTo(ChecksumType.FULL_OBJECT.toString());
        }
        assertThat(checksumCapturingInterceptor.completeMpuMpObjectSize).isEqualTo(FILE_SIZE);
    }

    private PutObjectRequest.Builder putObjectRequestBuilder() {
        return PutObjectRequest.builder().bucket("bucket").key("key");
    }

    private void stubSuccessfulResponses() {
        stubCreateMpuSuccessfulResponse();
        stubSuccessfulUploadParts(2); // 16 MB File Size / 8 MB Default Part Size
        stubCompleteMpuSuccessfulResponse();
    }

    private void stubCreateMpuSuccessfulResponse() {
        String mpuInitBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                             + "<InitiateMultipartUploadResult>\n"
                             + "   <Bucket>bucket</Bucket>\n"
                             + "   <Key>key</Key>\n"
                             + "   <UploadId>uploadId</UploadId>\n"
                             + "</InitiateMultipartUploadResult>";

        wireMock.stubFor(post(urlEqualTo("/bucket/key?uploads"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody(mpuInitBody)));
    }

    private void stubCompleteMpuSuccessfulResponse() {
        String mpuCompleteBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<CompleteMultipartUploadResult>\n"
                                 + "   <Bucket>bucket</Bucket>\n"
                                 + "   <Key>key</Key>\n"
                                 + "   <ETag>etag</ETag>\n"
                                 + "</CompleteMultipartUploadResult>";

        wireMock.stubFor(post(urlEqualTo("/bucket/key?uploadId=uploadId"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody(mpuCompleteBody)));
    }

    private void stubSuccessfulUploadParts(int numParts) {
        for (int i = 1; i <= numParts; i++) {
            wireMock.stubFor(put(urlEqualTo("/bucket/key?partNumber=" + i + "&uploadId=uploadId"))
                                 .willReturn(aResponse()
                                                 .withStatus(200)
                                                 .withBody("<Part><PartNumber>" + i +
                                                           "</PartNumber><ETag>\"etag\"</ETag></Part>")));
        }

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

    private static final class ChecksumCapturingInterceptor implements ExecutionInterceptor {
        private static final String CHECKSUM_ALGORITHM_HEADER = "x-amz-checksum-algorithm";
        private static final String CHECKSUM_TYPE_HEADER = "x-amz-checksum-type";
        private static final String MP_OBJECT_SIZE_HEADER = "x-amz-mp-object-size";
        private static final String SDK_CHECKSUM_ALGORITHM_HEADER = "x-amz-sdk-checksum-algorithm";
        Map<String, List<String>> completeMpuHeaders;
        String createMpuChecksumType;
        String createMpuChecksumAlgorithm;
        String uploadPartChecksumAlgorithm;
        String completeMpuChecksumType;
        Long completeMpuMpObjectSize;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            Map<String, List<String>> headers = context.httpRequest().headers();
            if (isCreateMpuRequest(context) && headers.containsKey(CHECKSUM_ALGORITHM_HEADER)) {
                createMpuChecksumAlgorithm = headers.get(CHECKSUM_ALGORITHM_HEADER).get(0);
            }

            if (isUploadPartRequest(context) && headers.containsKey(SDK_CHECKSUM_ALGORITHM_HEADER)) {
                uploadPartChecksumAlgorithm = headers.get(SDK_CHECKSUM_ALGORITHM_HEADER).get(0);
            }

            if (headers.containsKey(CHECKSUM_TYPE_HEADER)) {
                if (isCreateMpuRequest(context)) {
                    createMpuChecksumType = headers.get(CHECKSUM_TYPE_HEADER).get(0);
                } else if (isCompleteMpuRequest(context)) {
                    completeMpuChecksumType = headers.get(CHECKSUM_TYPE_HEADER).get(0);
                }
            }

            if (isCompleteMpuRequest(context) && headers.containsKey(MP_OBJECT_SIZE_HEADER)) {
                completeMpuMpObjectSize = Long.valueOf(headers.get(MP_OBJECT_SIZE_HEADER).get(0));
                completeMpuHeaders = headers;
            }
        }

        private static boolean isCreateMpuRequest(Context.BeforeTransmission context) {
            return context.request() instanceof CreateMultipartUploadRequest;
        }

        private static boolean isCompleteMpuRequest(Context.BeforeTransmission context) {
            return context.request() instanceof CompleteMultipartUploadRequest;
        }

        private static boolean isUploadPartRequest(Context.BeforeTransmission context) {
            return context.request() instanceof UploadPartRequest;
        }
    }
}