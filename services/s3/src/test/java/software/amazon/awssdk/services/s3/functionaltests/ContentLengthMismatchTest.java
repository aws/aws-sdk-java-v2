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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class ContentLengthMismatchTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private S3AsyncClientBuilder getAsyncClientBuilder() {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(endpoint())
                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    private URI endpoint() {
        return URI.create("http://localhost:" + wireMock.port());
    }

    @Test
    public void checksumDoesNotExceedContentLengthHeaderForPuts() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String content = "Hello, World!";
        String eTag = "65A8E27D8879283831B664BD8B7F0AD4";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", eTag)));

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

        PutObjectResponse response =
            s3Client.putObject(r -> r.bucket(bucket).key(key).contentLength((long) content.length()),
                               AsyncRequestBody.fromString(content + " Extra stuff!"))
                    .join();

        verify(putRequestedFor(anyUrl()).withRequestBody(equalTo(content)));
        assertThat(response.eTag()).isEqualTo(eTag);
    }
    @Test
    public void checksumDoesNotExceedAsyncRequestBodyLengthForPuts() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String content = "Hello, World!";
        String eTag = "65A8E27D8879283831B664BD8B7F0AD4";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", eTag)));

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

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

        verify(putRequestedFor(anyUrl()).withRequestBody(equalTo(content)));
        assertThat(response.eTag()).isEqualTo(eTag);
    }

    @Test
    public void contentShorterThanContentLengthHeaderFails() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

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

    @Test
    public void contentShorterThanRequestBodyLengthFails() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

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

}
