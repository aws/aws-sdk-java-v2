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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Tests to ensure different {@link AsyncRequestBody} implementations return the same data for every retry.
 */
public class AsyncRequestBodyRetryTest extends BaseRequestBodyRetryTest {
    private static ExecutorService requestBodyExecutor;
    private static SdkAsyncHttpClient netty;
    private S3AsyncClient s3;

    @BeforeAll
    public static void setup() throws Exception {
        BaseRequestBodyRetryTest.setup();
        requestBodyExecutor = Executors.newSingleThreadExecutor();
        netty = NettyNioAsyncHttpClient.builder()
                                       .buildWithDefaults(AttributeMap.builder()
                                                                      .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES,
                                                                           true)
                                                                      .build());
    }

    @BeforeEach
    public void methodSetup() {
        s3 = S3AsyncClient.builder()
                          .overrideConfiguration(o -> o.retryStrategy(StandardRetryStrategy.builder()
                                                                                           .maxAttempts(3)
                                                                                           .backoffStrategy(BackoffStrategy.retryImmediately())
                                                                                           .build()))
                          .region(Region.US_WEST_2)
                          .endpointOverride(URI.create("https://localhost:" + serverHttpsPort()))
                          .httpClient(netty)
                          .forcePathStyle(true)
                          .build();
    }

    @AfterAll
    public static void teardown() throws Exception {
        BaseRequestBodyRetryTest.teardown();
        netty.close();
        requestBodyExecutor.shutdown();
    }

    @ParameterizedTest
    @MethodSource("retryTestCases")
    void test_retries_allAttemptsSendSameBody(TestCase tc) throws IOException {
        Assumptions.assumeFalse(tc.type == BodyType.BLOCKING_INPUTSTREAM,
                                "forBlockingInputStream does not support retrying");
        // all content is created the same way so this data should match what's in the RequestBody
        byte[] referenceData = makeArrayOfSize(tc.size.getNumBytes());
        String expectedCrc32 = calculateCrc32(new ByteArrayInputStream(referenceData));

        AsyncRequestBody body = makeRequestBody(tc);

        assertThatThrownBy(s3.putObject(r -> r.bucket("my-bucket").key("my-obj"), body)::join)
            .hasCauseInstanceOf(S3Exception.class)
            .matches(e -> {
                S3Exception s3e = (S3Exception) e.getCause();
                return s3e.numAttempts() == 3 && s3e.statusCode() == 500;
            }, "Should attempt total of 3 times");

        List<String> requestBodyChecksums = getRequestChecksums();
        assertThat(requestBodyChecksums.size()).isEqualTo(3);

        requestBodyChecksums.forEach(bodyChecksum -> assertThat(bodyChecksum).isEqualTo(expectedCrc32));
    }

    private static List<TestCase> retryTestCases() {
        List<TestCase> testCases = new ArrayList<>();

        for (BodyType type : BodyType.values()) {
            for (BodySize size : BodySize.values()) {
                testCases.add(new TestCase().type(type).size(size));
            }
        }

        return testCases;
    }

    private AsyncRequestBody makeRequestBody(TestCase tc) throws IOException {
        int nBytes = tc.size.getNumBytes();
        switch (tc.type) {
            case STRING:
                return AsyncRequestBody.fromString(makeStringOfSize(nBytes), StandardCharsets.UTF_8);
            case BYTES:
                return AsyncRequestBody.fromBytes(makeArrayOfSize(nBytes));
            case BYTES_UNSAFE:
                return AsyncRequestBody.fromBytesUnsafe(makeArrayOfSize(nBytes));
            case FILE:
                return AsyncRequestBody.fromFile(testFiles.get(tc.size));
            case INPUTSTREAM: {
                InputStream is = getMarkSupportedStreamOfSize(tc.size);
                return AsyncRequestBody.fromInputStream(cfg -> cfg.inputStream(is)
                                                                  .contentLength((long) nBytes)
                                                                  // read limit has to be positive
                                                                  .maxReadLimit(nBytes == 0 ? 1 : nBytes)
                                                                  .executor(requestBodyExecutor));
            }
            case REMAINING_BYTE_BUFFER:
                // fall through
            case REMAINING_BYTE_BUFFER_UNSAFE:
                // fall through
            case BYTE_BUFFER_UNSAFE:
                // fall through
            case BYTE_BUFFER: {
                ByteBuffer byteBuffer = ByteBuffer.wrap(makeArrayOfSize(nBytes));
                switch (tc.type) {
                    case REMAINING_BYTE_BUFFER:
                        return AsyncRequestBody.fromRemainingByteBuffer(byteBuffer);
                    case REMAINING_BYTE_BUFFER_UNSAFE:
                        return AsyncRequestBody.fromRemainingByteBufferUnsafe(byteBuffer);
                    case BYTE_BUFFER_UNSAFE:
                        return AsyncRequestBody.fromByteBufferUnsafe(byteBuffer);
                    case BYTE_BUFFER:
                        return AsyncRequestBody.fromByteBuffer(byteBuffer);
                    default:
                        throw new RuntimeException("Unexpected type: " + tc.type);
                }
            }
            case REMAINING_BYTE_BUFFERS:
                // fall through
            case REMAINING_BYTE_BUFFERS_UNSAFE:
                // fall through
            case BYTE_BUFFERS_UNSAFE:
                // fall through
            case BYTE_BUFFERS: {
                ByteBuffer[] buffers;
                if (tc.size.getNumBytes() > 0) {
                    byte[] bbContent = getDataSegment();
                    int nSegments = nBytes / bbContent.length;
                    buffers = IntStream.range(0, nSegments)
                                       .mapToObj(i -> ByteBuffer.wrap(bbContent))
                                       .toArray(ByteBuffer[]::new);
                } else {
                    // TODO: This is a workaround because you can't do AsyncRequestBody.fromByteBuffers(new ByteBuffer[0]); the
                    //  subscriber is never signaled onComplete. See issue JAVA-8215.
                    buffers = new ByteBuffer[]{ ByteBuffer.allocate(0) };
                }

                switch (tc.type) {
                    case REMAINING_BYTE_BUFFERS:
                        return AsyncRequestBody.fromRemainingByteBuffers(buffers);
                    case REMAINING_BYTE_BUFFERS_UNSAFE:
                        return AsyncRequestBody.fromRemainingByteBuffersUnsafe(buffers);
                    case BYTE_BUFFERS_UNSAFE:
                        return AsyncRequestBody.fromByteBuffersUnsafe(buffers);
                    case BYTE_BUFFERS:
                        return AsyncRequestBody.fromByteBuffers(buffers);
                    default:
                        throw new RuntimeException("Unexpected type: " + tc.type);
                }
            }
            default:
                throw new RuntimeException("Unsupported body type: " + tc.type);
        }
    }

    private enum BodyType {
        STRING,

        BYTES,
        BYTES_UNSAFE,

        INPUTSTREAM,
        BLOCKING_INPUTSTREAM, // Note: doesn't support retries, left out for testing

        BYTE_BUFFER,
        BYTE_BUFFER_UNSAFE,
        REMAINING_BYTE_BUFFER,
        REMAINING_BYTE_BUFFER_UNSAFE,


        BYTE_BUFFERS,
        BYTE_BUFFERS_UNSAFE,
        REMAINING_BYTE_BUFFERS,
        REMAINING_BYTE_BUFFERS_UNSAFE,

        FILE;
    }


    private static class TestCase {
        private BodyType type;
        private BodySize size;

        public TestCase type(BodyType type) {
            this.type = type;
            return this;
        }

        public TestCase size(BodySize size) {
            this.size = size;
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
}
