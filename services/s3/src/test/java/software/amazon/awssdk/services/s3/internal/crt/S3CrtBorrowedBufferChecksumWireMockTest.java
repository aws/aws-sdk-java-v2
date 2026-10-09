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

package software.amazon.awssdk.services.s3.internal.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@WireMockTest
@Timeout(20)
class S3CrtBorrowedBufferChecksumWireMockTest {
    private static final int PART_SIZE = 256 * 1024;
    private static final int OBJECT_SIZE = 2 * PART_SIZE;
    private static final byte[] FIRST_PART = filled(PART_SIZE, (byte) 'a');
    private static final byte[] SECOND_PART = filled(PART_SIZE, (byte) 'b');
    private static final byte[] CONTENT = concatenate(FIRST_PART, SECOND_PART);
    private static final String FIRST_PART_CRC32 = "uo2NxA==";
    private static final String WRONG_FULL_OBJECT_CRC32 = "+NNMYw==";
    private static final String E_TAG = "\"checksum-etag\"";

    @Test
    void perPartChecksumMismatch_shouldNotDeliverBadPart(WireMockRuntimeInfo wireMock) throws Exception {
        stubHead(null);
        stubRange(0, FIRST_PART, FIRST_PART_CRC32);
        stubRange(1, SECOND_PART, FIRST_PART_CRC32);

        try (S3AsyncClient client = newClient(wireMock);
             ResponseInputStream<GetObjectResponse> stream = borrowedStream(client)) {
            byte[] delivered = new byte[PART_SIZE];
            readExactly(stream, delivered);
            assertThat(delivered).containsExactly(FIRST_PART);

            Throwable failure = catchThrowable(stream::read);
            assertThat(failure).isInstanceOf(SdkClientException.class)
                               .hasMessageContaining("checksum");
        }
    }

    @Test
    void fullObjectChecksumMismatch_shouldFailFinalReadWithOrdinaryExceptionType(
        WireMockRuntimeInfo wireMock) throws Exception {
        stubHead(WRONG_FULL_OBJECT_CRC32);
        stubRange(0, FIRST_PART, null);
        stubRange(1, SECOND_PART, null);

        Throwable borrowedFailure;
        try (S3AsyncClient client = newClient(wireMock);
             ResponseInputStream<GetObjectResponse> stream = borrowedStream(client)) {
            byte[] delivered = new byte[OBJECT_SIZE];
            readExactly(stream, delivered);
            assertThat(delivered).containsExactly(CONTENT);
            borrowedFailure = catchThrowable(stream::read);
        }

        Throwable ordinaryFailure;
        try (S3AsyncClient client = newClient(wireMock);
             ResponseInputStream<GetObjectResponse> stream = ordinaryStream(client)) {
            ordinaryFailure = catchThrowable(() -> drain(stream));
        }

        assertThat(borrowedFailure).isInstanceOf(SdkClientException.class)
                                   .hasMessageContaining("checksum");
        assertThat(ordinaryFailure).isExactlyInstanceOf(borrowedFailure.getClass())
                                   .hasMessage(borrowedFailure.getMessage());
    }

    private static S3AsyncClient newClient(WireMockRuntimeInfo wireMock) {
        return S3AsyncClient.crtBuilder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(URI.create("http://localhost:" + wireMock.getHttpPort()))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("key", "secret")))
                            .minimumPartSizeInBytes((long) PART_SIZE)
                            .initialReadBufferSizeInBytes((long) PART_SIZE)
                            .maxConcurrency(1)
                            .responseChecksumValidation(ResponseChecksumValidation.WHEN_SUPPORTED)
                            .directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(PART_SIZE))
                            .build();
    }

    private static ResponseInputStream<GetObjectResponse> borrowedStream(S3AsyncClient client) throws Exception {
        return client.getObject(request(), S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers())
                     .get();
    }

    private static ResponseInputStream<GetObjectResponse> ordinaryStream(S3AsyncClient client) throws Exception {
        return client.getObject(request(), AsyncResponseTransformer.toBlockingInputStream()).get();
    }

    private static GetObjectRequest request() {
        return GetObjectRequest.builder().bucket("bucket").key("key").checksumMode(ChecksumMode.ENABLED).build();
    }

    private static void stubHead(String checksum) {
        com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder response =
            aResponse().withStatus(200)
                       .withHeader("Content-Length", Integer.toString(OBJECT_SIZE))
                       .withHeader("ETag", E_TAG);
        if (checksum != null) {
            response.withHeader("x-amz-checksum-crc32", checksum)
                    .withHeader("x-amz-checksum-type", "FULL_OBJECT");
        }
        stubFor(head(anyUrl()).withHeader("x-amz-checksum-mode", equalTo("enabled")).willReturn(response));
    }

    private static void stubRange(int part, byte[] body, String checksum) {
        int start = part * PART_SIZE;
        int end = start + body.length - 1;
        com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder response =
            aResponse().withStatus(206)
                       .withHeader("Content-Length", Integer.toString(body.length))
                       .withHeader("Content-Range", "bytes " + start + "-" + end + "/" + OBJECT_SIZE)
                       .withHeader("ETag", E_TAG)
                       .withBody(body);
        if (checksum != null) {
            response.withHeader("x-amz-checksum-crc32", checksum);
        }
        stubFor(get(anyUrl()).withHeader("x-amz-checksum-mode", equalTo("enabled"))
                             .withHeader("Range", equalTo("bytes=" + start + "-" + end))
                             .willReturn(response));
    }

    private static void readExactly(InputStream stream, byte[] destination) throws IOException {
        int offset = 0;
        while (offset < destination.length) {
            int read = stream.read(destination, offset, destination.length - offset);
            if (read < 0) {
                throw new IOException("Unexpected end of stream");
            }
            offset += read;
        }
    }

    private static void drain(InputStream stream) throws IOException {
        byte[] buffer = new byte[8192];
        while (stream.read(buffer) >= 0) {
        }
    }

    private static byte[] filled(int length, byte value) {
        byte[] result = new byte[length];
        Arrays.fill(result, value);
        return result;
    }

    private static byte[] concatenate(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
