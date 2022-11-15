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

package software.amazon.awssdk.services.s3.model;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@WireMockTest
abstract class S3RequestTestBase <T extends S3Request> {

    private int wireMockPort;

    private S3Client s3Client;

    private S3AsyncClient s3AsyncClient;

    abstract T s3RequestWithUploadId(String uploadId);
    abstract S3Response performRequest(S3Client client, T request);
    abstract CompletableFuture<? extends S3Response> performRequestAsync(S3AsyncClient client, T request);

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        wireMockPort = wmRuntimeInfo.getHttpPort();

        s3Client = S3Client.builder()
                           .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                                            "skid")))
                           .region(Region.US_WEST_2)
                           .endpointOverride(URI.create("http://localhost:" + wireMockPort))
                           .serviceConfiguration(S3Configuration.builder()
                                                                .checksumValidationEnabled(false)
                                                                .pathStyleAccessEnabled(true)
                                                                .build())
                           .build();

        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                                                      "skid")))
                                     .region(Region.US_WEST_2)
                                     .endpointOverride(URI.create("http://localhost:" + wireMockPort))
                                     .serviceConfiguration(S3Configuration.builder()
                                                                          .checksumValidationEnabled(false)
                                                                          .pathStyleAccessEnabled(true)
                                                                          .build())
                                     .build();
    }

    @Test
    void marshall_syncMissingUploadId_ThrowsException() {
        stubAndRespondWith(200,"<xml></xml>");

        T request = s3RequestWithUploadId(null);

        assertThatThrownBy(() -> performRequest(s3Client, request))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Parameter 'uploadId' must not be null");
    }

    @Test
    void marshall_syncEmptyUploadId_encodesAsEmptyValue() {
        stubAndRespondWith(200,"<xml></xml>");

        T request = s3RequestWithUploadId("");
        performRequest(s3Client, request);

        verify(anyRequestedFor(anyUrl()).withQueryParam("uploadId", equalTo("")));
    }

    @Test
    void marshall_syncNonEmptyUploadId_encodesValue() {
        stubAndRespondWith(200,"<xml></xml>");

        T request = s3RequestWithUploadId("123");
        performRequest(s3Client, request);

        verify(anyRequestedFor(anyUrl()).withQueryParam("uploadId", equalTo("123")));
    }

    @Test
    void marshall_asyncMissingUploadId_ThrowsException() {
        stubAndRespondWith(200,"<xml></xml>");

        T request = s3RequestWithUploadId(null);

        ThrowableAssert.ThrowingCallable throwingCallable = () -> performRequestAsync(s3AsyncClient, request).join();
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(CompletionException.class)
            .hasMessageContaining("Parameter 'uploadId' must not be null");
    }

    @Test
    void marshall_asyncEmptyUploadId_encodesAsEmptyValue() {
        stubAndRespondWith(200,"<xml></xml>");

        T request = s3RequestWithUploadId("");
        performRequestAsync(s3AsyncClient, request).join();

        verify(anyRequestedFor(anyUrl()).withQueryParam("uploadId", equalTo("")));
    }

    @Test
    void marshall_asyncNonEmptyUploadId_encodesValue() {
        stubAndRespondWith(200,"<xml></xml>");

        T request = s3RequestWithUploadId("123");
        performRequestAsync(s3AsyncClient, request).join();

        verify(anyRequestedFor(anyUrl()).withQueryParam("uploadId", equalTo("123")));
    }

    private static void stubAndRespondWith(int status, String body) {
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse().withStatus(status).withBody(body)));
    }

}
