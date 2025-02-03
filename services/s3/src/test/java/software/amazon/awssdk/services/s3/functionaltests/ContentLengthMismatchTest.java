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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@WireMockTest
public class ContentLengthMismatchTest {

    private S3AsyncClientBuilder s3AsyncClientBuilder;

    private S3AsyncClient s3AsyncClient(RequestChecksumCalculation requestChecksumCalculation) {
        return requestChecksumCalculation == null
               ? s3AsyncClientBuilder.build()
               : s3AsyncClientBuilder.requestChecksumCalculation(requestChecksumCalculation).build();
    }

    @BeforeEach
    void init(WireMockRuntimeInfo wm) {
        s3AsyncClientBuilder = S3AsyncClient.builder()
                                            .region(Region.US_EAST_1)
                                            .endpointOverride(endpoint(wm))
                                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                                "key", "secret")));
    }

    static Stream<RequestChecksumCalculation> requestChecksumCalculations() {
        return Stream.of(
            null,
            RequestChecksumCalculation.WHEN_REQUIRED,
            RequestChecksumCalculation.WHEN_SUPPORTED
        );
    }

    private URI endpoint(WireMockRuntimeInfo wiremock) {
        return URI.create("http://localhost:" + wiremock.getHttpPort());
    }

    @ParameterizedTest(name = "RequestChecksumCalculation: {arguments}")
    @MethodSource("requestChecksumCalculations")
    public void checksumDoesNotExceedContentLengthHeaderForPuts(RequestChecksumCalculation requestChecksumCalculation) {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String content = "Hello, World!";
        String eTag = "65A8E27D8879283831B664BD8B7F0AD4";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", eTag)));

        S3AsyncClient s3Client = s3AsyncClient(requestChecksumCalculation);

        PutObjectResponse response =
            s3Client.putObject(r -> r.bucket(bucket).key(key).contentLength((long) content.length()),
                               AsyncRequestBody.fromString(content + " Extra stuff!"))
                    .join();

        verify(putRequestedFor(anyUrl()).withRequestBody(notContaining("stuff!")));
        assertThat(response.eTag()).isEqualTo(eTag);
    }

    @ParameterizedTest(name = "RequestChecksumCalculation: {arguments}")
    @MethodSource("requestChecksumCalculations")
    public void checksumDoesNotExceedAsyncRequestBodyLengthForPuts(RequestChecksumCalculation requestChecksumCalculation) {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String content = "Hello, World!";
        String eTag = "65A8E27D8879283831B664BD8B7F0AD4";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", eTag)));

        S3AsyncClient s3Client = s3AsyncClient(requestChecksumCalculation);

        PutObjectResponse response =
            s3Client.putObject(r -> r.bucket(bucket).key(key),
                               new AsyncRequestBody() {
                                   @Override
                                   public Optional<Long> contentLength() {
                                       return Optional.of((long) content.length());
                                   }

                                   @Override
                                   public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                                       AsyncRequestBody.fromString(content + " Extra stuff!").subscribe(subscriber);
                                   }
                               })
                    .join();

        verify(putRequestedFor(anyUrl()).withRequestBody(notContaining("stuff!")));
        assertThat(response.eTag()).isEqualTo(eTag);
    }

    @ParameterizedTest(name = "RequestChecksumCalculation: {arguments}")
    @MethodSource("requestChecksumCalculations")
    public void contentShorterThanContentLengthHeaderFails(RequestChecksumCalculation requestChecksumCalculation) {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String eTag = "65A8E27D8879283831B664BD8B7F0AD4";

        S3AsyncClient s3Client = s3AsyncClient(requestChecksumCalculation);
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", eTag)));

        AsyncRequestBody requestBody = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                AsyncRequestBody.fromString("A").subscribe(subscriber);
            }
        };

        assertThatThrownBy(() -> s3Client.putObject(r -> r.bucket(bucket).key(key).contentLength(2L), requestBody)
                                         .get(10, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("content-length");
    }

    @ParameterizedTest(name = "RequestChecksumCalculation: {arguments}")
    @MethodSource("requestChecksumCalculations")
    public void contentShorterThanRequestBodyLengthFails(RequestChecksumCalculation requestChecksumCalculation) {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String eTag = "65A8E27D8879283831B664BD8B7F0AD4";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", eTag)));
        S3AsyncClient s3Client = s3AsyncClient(requestChecksumCalculation);

        AsyncRequestBody requestBody = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(2L);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                AsyncRequestBody.fromString("A").subscribe(subscriber);
            }
        };

        assertThatThrownBy(() -> s3Client.putObject(r -> r.bucket(bucket).key(key), requestBody)
                                         .get(10, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("content-length");
    }

    static StringValuePattern notContaining(String expected) {
        return new NotContainsPattern(expected);
    }

    private static final class NotContainsPattern extends StringValuePattern {
        private NotContainsPattern(@JsonProperty("expectedValue") String expectedValue) {
            super(expectedValue);
        }
        @Override
        public MatchResult match(String value) {
            return MatchResult.of(value != null && !value.contains(expectedValue));
        }
    }

}
