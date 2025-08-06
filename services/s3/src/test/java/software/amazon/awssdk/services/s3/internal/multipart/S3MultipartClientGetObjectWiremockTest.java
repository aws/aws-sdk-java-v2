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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WireMockTest
@Timeout(value = 45, unit = TimeUnit.SECONDS)
public class S3MultipartClientGetObjectWiremockTest {
    private static final String BUCKET = "Example-Bucket";
    private static final String KEY = "Key";
    private static int fileCounter = 0;
    private S3AsyncClient multipartClient;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wm) {
        multipartClient = S3AsyncClient.builder()
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .multipartEnabled(true)
                                       .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                                                 .maxConcurrency(100)
                                                                                 .connectionAcquisitionTimeout(Duration.ofSeconds(60)))
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                       .build();
    }

    private static Stream<TransformerFactory> responseTransformerFactories() {
        return Stream.of(
            AsyncResponseTransformer::toBytes,
            AsyncResponseTransformer::toBlockingInputStream,
            AsyncResponseTransformer::toPublisher,
            () -> {
                try {
                    Path tempDir = Files.createTempDirectory("s3-test");
                    Path tempFile = tempDir.resolve("testFile" + fileCounter + ".txt");
                    fileCounter++;
                    tempFile.toFile().deleteOnExit();
                    return AsyncResponseTransformer.toFile(tempFile);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        );
    }

    interface TransformerFactory {
        AsyncResponseTransformer<GetObjectResponse, ?> create();
    }

    @ParameterizedTest
    @MethodSource("responseTransformerFactories")
    public void getObject_single500WithinMany200s_shouldNotRetryError(TransformerFactory transformerFactory) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        int numRuns = 100;
        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<?> resp = mock200Response(multipartClient, i, transformerFactory);
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

        CompletableFuture<?> requestWithRetryableError =
            multipartClient.getObject(r -> r.bucket(BUCKET).key(errorKey), transformerFactory.create());
        futures.add(requestWithRetryableError);

        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<?> resp = mock200Response(multipartClient, i + 1000, transformerFactory);
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

    private CompletableFuture<?> mock200Response(S3AsyncClient s3Client, int runNumber, TransformerFactory transformerFactory) {
        String runId = runNumber + " success";

        stubFor(any(anyUrl())
                    .withHeader("RunNum", matching(runId))
                    .inScenario(runId)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("x-amz-request-id", String.valueOf(UUID.randomUUID()))
                                           .withBody("Hello World")));

        return s3Client.getObject(r -> r.bucket(BUCKET).key(KEY)
                                        .overrideConfiguration(c -> c.putHeader("RunNum", runId)),
                                  transformerFactory.create());
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
}
