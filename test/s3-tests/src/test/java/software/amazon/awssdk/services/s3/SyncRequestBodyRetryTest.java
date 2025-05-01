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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.FunctionalUtils;

/**
 * Tests to ensure different {@link RequestBody} implementations return the same data for every retry.
 // */
public class SyncRequestBodyRetryTest extends BaseRequestBodyRetryTest {
    private static SdkHttpClient apache;
    private S3Client s3;

    @BeforeAll
    public static void setup() throws Exception {
        BaseRequestBodyRetryTest.setup();
        apache = ApacheHttpClient.builder()
                                 .buildWithDefaults(AttributeMap.builder()
                                                                .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                                                                .build());
    }

    @BeforeEach
    public void methodSetup() {
        s3 = S3Client.builder()
                     .overrideConfiguration(o -> o.retryStrategy(StandardRetryStrategy.builder()
                                                                                      .maxAttempts(3)
                                                                                      .backoffStrategy(BackoffStrategy.retryImmediately())
                                                                                      .build()))
                     .region(Region.US_WEST_2)
                     .endpointOverride(URI.create("https://localhost:" + serverHttpsPort()))
                     .forcePathStyle(true)
                     .httpClient(apache)
                     .build();
    }

    @AfterAll
    public static void teardown() throws Exception {
        apache.close();
        BaseRequestBodyRetryTest.teardown();
    }

    @AfterEach
    public void methodTeardown() {
        s3.close();
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void test_retries_allAttemptsSendSameBody(TestCase tc) throws IOException {
        // fromInputStream sets an unconfigurable read limit of 128KiB so anything larger will fail to reset().
        Assumptions.assumeFalse(tc.type == BodyType.INPUTSTREAM && tc.size.getNumBytes() > 128 * KB,
                                "RequestBody.fromInputStream does not support retries for content larger than 128 KiB");

        // all content is created the same way so this data should match what's in the RequestBody
        byte[] referenceData = makeArrayOfSize(tc.size.getNumBytes());
        String expectedCrc32 = calculateCrc32(new ByteArrayInputStream(referenceData));

        RequestBody testBody = makeRequestBody(tc);

        assertThatThrownBy(() -> {
            s3.putObject(r -> r.bucket("my-bucket").key("my-obj"), testBody);
        }).isInstanceOf(S3Exception.class)
          .matches(e -> {
              S3Exception s3e = (S3Exception) e;
              return s3e.numAttempts() == 3 && s3e.statusCode() == 500;
          }, "Should attempt total of 3 times");

        List<String> bodies = getRequestChecksums();
        assertThat(bodies.size()).isEqualTo(3);

        bodies.forEach(checksum -> assertThat(checksum).isEqualTo(expectedCrc32));
    }

    private static List<TestCase> testCases() {
        List<TestCase> testCases = new ArrayList<>();
        for (BodyType type : BodyType.values()) {
            for (BodySize size : BodySize.values()) {
                testCases.add(new TestCase().size(size).type(type));
            }
        }

        return testCases;
    }

    private RequestBody makeRequestBody(TestCase tc) throws IOException {
        switch (tc.type) {
            case STRING:
                return RequestBody.fromString(makeStringOfSize(tc.size.getNumBytes()), StandardCharsets.UTF_8);
            case BYTES:
                return RequestBody.fromBytes(makeArrayOfSize(tc.size.getNumBytes()));
            case BYTE_BUFFER:
                return RequestBody.fromByteBuffer(ByteBuffer.wrap(makeArrayOfSize(tc.size.getNumBytes())));
            case REMAINING_BYTE_BUFFER:
                return RequestBody.fromRemainingByteBuffer(ByteBuffer.wrap(makeArrayOfSize(tc.size.getNumBytes())));
            case INPUTSTREAM: {
                InputStream fileStream = getMarkSupportedStreamOfSize(tc.size);
                return RequestBody.fromInputStream(fileStream, tc.size.getNumBytes());
            }
            case CONTENT_PROVIDER: {
                Path file = testFiles.get(tc.size);
                return RequestBody.fromContentProvider(new TestContentSteamProvider(file), "text/plain");
            }
            case CONTENT_PROVIDER_NO_LENGTH: {
                Path file = testFiles.get(tc.size);
                return RequestBody.fromContentProvider(new TestContentSteamProvider(file), tc.size.getNumBytes(), "text/plain");
            }
            case FILE:
                return RequestBody.fromFile(testFiles.get(tc.size));
            default:
                throw new RuntimeException("Unsupported body type: " + tc.type);
        }
    }

    private static class TestCase {
        private BodyType type;
        private BodySize size;

        public TestCase size(BodySize size) {
            this.size = size;
            return this;
        }

        public TestCase type(BodyType type) {
            this.type = type;
            return this;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                   "type=" + type +
                   ", size=" + size +
                   '}';
        }
    }

    private enum BodyType {
        BYTE_BUFFER,

        BYTES,

        CONTENT_PROVIDER,

        CONTENT_PROVIDER_NO_LENGTH,

        FILE,

        INPUTSTREAM,

        REMAINING_BYTE_BUFFER,

        STRING
    }

    private static class TestContentSteamProvider implements ContentStreamProvider {
        private final Path file;
        private InputStream lastStream;

        public TestContentSteamProvider(Path file) {
            this.file = file;
        }

        @Override
        public InputStream newStream() {
            if (lastStream != null) {
                FunctionalUtils.invokeSafely(lastStream::close);
            }
            lastStream = FunctionalUtils.invokeSafely(() -> Files.newInputStream(file));
            return lastStream;
        }
    }
}
