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

package software.amazon.awssdk.services.s3.functionaltests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Tests that when a {@link SimplePublisher} is used as the putObject request body
 * and putObject fails with a retryable error, the SDK retry completes promptly instead of hanging.
 */
@WireMockTest
public class PutObjectRequestBodyFromSimplePublisherRetryTest {

    private static final String BUCKET = "test-bucket";
    private static final String WRITE_KEY = "dest-object";
    private static final byte[] BODY = "hello world test data".getBytes(StandardCharsets.UTF_8);

    private S3AsyncClient s3Client;

    @BeforeEach
    void setup(WireMockRuntimeInfo wiremock) {
        URI endpoint = URI.create("http://localhost:" + wiremock.getHttpPort());

        s3Client = S3AsyncClient.builder()
                                .region(Region.US_EAST_1)
                                .endpointOverride(endpoint)
                                .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("key", "secret")))
                                .forcePathStyle(true)
                                .build();
    }

    @AfterEach
    void tearDown() {
        s3Client.close();
    }

    @Test
    void putObjectRetry_withSimplePublisherAsRequestBody_shouldFailFast() {
        stubFor(put(urlPathEqualTo("/" + BUCKET + "/" + WRITE_KEY))
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code>"
                                              + "<Message>Internal Server Error</Message></Error>")));

        // Simulate a ResponsePublisher backed by SimplePublisher (e.g., from getObject)
        SimplePublisher<ByteBuffer> sourcePublisher = new SimplePublisher<>();
        sourcePublisher.send(ByteBuffer.wrap(BODY));
        sourcePublisher.complete();

        AsyncRequestBody requestBody = new AsyncRequestBody() {
            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                sourcePublisher.subscribe(subscriber);
            }

            @Override
            public Optional<Long> contentLength() {
                return Optional.of((long) BODY.length);
            }
        };

        CompletableFuture<?> putFuture =
            s3Client.putObject(r -> r.bucket(BUCKET).key(WRITE_KEY), requestBody);

        // Should fail fast, not hang for 30+ seconds.
        assertThatThrownBy(() -> putFuture.get(5, TimeUnit.SECONDS))
            .isNotInstanceOf(TimeoutException.class)
            .hasCauseInstanceOf(SdkClientException.class);
    }
}
