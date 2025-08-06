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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WireMockTest
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class S3MultipartClientGetObjectWiremockTest {
    public static final String BUCKET = "Example-Bucket";
    public static final String KEY = "Key";
    private static final int MAX_ATTEMPTS = 7;
    private S3AsyncClient multipartClient;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wm) {
        multipartClient = S3AsyncClient.builder()
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .multipartEnabled(true)
                                       .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                                                 .maxConcurrency(100)
                                                                                 .connectionAcquisitionTimeout(Duration.ofSeconds(100)))
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                       .build();
    }

    @Test
    public void getObject_single500WithinMany200s_shouldNotRetryError() {
        List<CompletableFuture<ResponseBytes<GetObjectResponse>>> futures = new ArrayList<>();

        int numRuns = 1000;
        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<ResponseBytes<GetObjectResponse>> resp = mock200Response(multipartClient, i);
            futures.add(resp);
        }

        String errorKey = "ErrorKey";
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, errorKey)))
                    .inScenario("RetryableError")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                                    .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                    .withStatus(500)
                                    .withBody(internalErrorBody())
                    )
                    .willSetStateTo("RetryAttempt"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, errorKey)))
                    .inScenario("RetryableError")
                    .whenScenarioStateIs("RetryAttempt")
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                           .withBody("Hello World")));

        CompletableFuture<ResponseBytes<GetObjectResponse>> requestWithRetryableError =
            multipartClient.getObject(r -> r.bucket(BUCKET).key(errorKey), AsyncResponseTransformer.toBytes());
        futures.add(requestWithRetryableError);

        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<ResponseBytes<GetObjectResponse>> resp = mock200Response(multipartClient, i + 1000);
            futures.add(resp);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            fail("Expecting 500 error to fail request.");
        } catch (CompletionException e) {
            assertThat(e.getCause()).isInstanceOf(S3Exception.class);
        }

        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, errorKey))));
    }

    private String errorBody(String errorCode, String errorMessage) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
               + "<Error>\n"
               + "  <Code>" + errorCode + "</Code>\n"
               + "  <Message>" + errorMessage + "</Message>\n"
               + "</Error>";
    }

    private String internalErrorBody() {
        return errorBody("InternalError", "We encountered an internal error. Please try again.");
    }

    private CompletableFuture<ResponseBytes<GetObjectResponse>> mock200Response(S3AsyncClient s3Client, int runNumber) {
        String runId = runNumber + " success";

        stubFor(any(anyUrl())
                    .withHeader("RunNum", matching(runId))
                    .inScenario(runId)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                           .withBody("Hello World")));

        return s3Client.getObject(r -> r.bucket(BUCKET).key("key")
                                        .overrideConfiguration(c -> c.putHeader("RunNum", runId)),
                                  AsyncResponseTransformer.toBytes());
    }
}
