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

package software.amazon.awssdk.services.s3.checksums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Tests to ensure that checksum values are not recomputed between retries.
 */
public class ChecksumReuseTest {
    private static final String BUCKET = "test-bucket";
    private static final String KEY = "test-key";
    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER = StaticCredentialsProvider.create(
        AwsBasicCredentials.create("akid", "skid"));

    @Test
    public void putObject_serverResponds500_usesSameChecksumOnRetries() {
        MockHttpClient httpClient = new MockHttpClient();

        S3Client s3 = S3Client.builder()
                              .region(Region.US_WEST_2)
                              .credentialsProvider(CREDENTIALS_PROVIDER)
                              .requestChecksumCalculation(RequestChecksumCalculation.WHEN_SUPPORTED)
                              .httpClient(httpClient)
                              .overrideConfiguration(o -> o.retryStrategy(StandardRetryStrategy.builder()
                                                                                               .maxAttempts(4)
                                                                                               .backoffStrategy(BackoffStrategy.retryImmediately())
                                                                                               .build()))
                              .build();

        RequestBody requestBody = RequestBody.fromInputStream(new RandomInputStream(), 4096);

        assertThatThrownBy(() -> s3.putObject(r -> r.bucket(BUCKET).key(KEY).checksumAlgorithm(ChecksumAlgorithm.CRC32),
                                              requestBody))
            .isInstanceOf(S3Exception.class)
            // Ensure we actually retried
            .matches(e -> ((SdkException) e).numAttempts() == 4);

        List<String> trailingChecksumHeaders = new ArrayList<>();
        for (byte[] requestPayload : httpClient.requestPayloads) {
            String payloadAsString = new String(requestPayload, StandardCharsets.UTF_8);
            for (String part : payloadAsString.split("\r\n")) {
                if (part.startsWith("x-amz-checksum-crc32")) {
                    trailingChecksumHeaders.add(part);
                    break;
                }
            }
        }

        // sanity check, ensure each request has a trailing checksum header
        assertThat(trailingChecksumHeaders).hasSize(4);
        // All checksum trailers should be the same
        assertThat(trailingChecksumHeaders.stream().distinct().count()).isEqualTo(1);
    }

    private static class MockHttpClient implements SdkHttpClient {
        private List<byte[]> requestPayloads = new ArrayList<>();

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            return new MockExecutableHttpRequest(request.contentStreamProvider().get().newStream(), requestPayloads);
        }

        @Override
        public void close() {
        }
    }

    private static class MockExecutableHttpRequest implements ExecutableHttpRequest {
        private final InputStream content;
        private final List<byte[]> requestPayloads;

        private MockExecutableHttpRequest(InputStream content, List<byte[]> requestPayloads) {
            this.content = content;
            this.requestPayloads = requestPayloads;
        }

        @Override
        public HttpExecuteResponse call() throws IOException {
            requestPayloads.add(IoUtils.toByteArray(content));

            return HttpExecuteResponse.builder()
                                      .response(SdkHttpFullResponse.builder().statusCode(503)
                                                                   .build())
                                      .responseBody(AbortableInputStream.create(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))))
                                      .build();
        }

        @Override
        public void abort() {
        }
    }

    /**
     * A stream that randomly returns other 'a' or 'b' from each read() invocation.
     */
    private static class RandomInputStream extends InputStream {
        private final Random rng = new Random();

        @Override
        public int read() throws IOException {
            if (rng.nextBoolean()) {
                return 'a';
            }
            return 'b';
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public synchronized void mark(int readlimit) {
        }

        @Override
        public synchronized void reset() throws IOException {
        }
    }
}
