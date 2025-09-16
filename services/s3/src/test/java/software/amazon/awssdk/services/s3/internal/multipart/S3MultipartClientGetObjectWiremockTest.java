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
import static com.github.tomakehurst.wiremock.client.WireMock.lessThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThan;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartDownloadTestUtils.internalErrorBody;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartDownloadTestUtils.transformersSuppliers;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.InputStreamResponseTransformer;
import software.amazon.awssdk.core.internal.async.PublisherAsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.SplittingTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartDownloadTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.utils.AsyncResponseTransformerTestSupplier;
import software.amazon.awssdk.utils.Pair;

@WireMockTest
@Timeout(value = 45, unit = TimeUnit.SECONDS)
public class S3MultipartClientGetObjectWiremockTest {
    private static final String BUCKET = "Example-Bucket";
    private static final String KEY = "Key";
    private static final int MAX_ATTEMPTS = 3;
    private S3AsyncClient multipartClient;
    private MultipartDownloadTestUtils util;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wm)
    {
        wm.getWireMock().resetRequests();
        wm.getWireMock().resetScenarios();
        wm.getWireMock().resetMappings();
        multipartClient = S3AsyncClient.builder()
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .multipartEnabled(true)
                                       .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                                                 .maxConcurrency(100)
                                                                                 .connectionAcquisitionTimeout(Duration.ofSeconds(60)))
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                       .overrideConfiguration(o -> o.retryStrategy(b -> b.maxAttempts(MAX_ATTEMPTS)))
                                       .build();
        util = new MultipartDownloadTestUtils(BUCKET, KEY, UUID.randomUUID().toString());
    }

    @ParameterizedTest
    @MethodSource("partSizeAndTransformerParams")
    public <T> void happyPath_shouldReceiveAllBodyPartInCorrectOrder(AsyncResponseTransformerTestSupplier<T> supplier,
                                                              int amountOfPartToTest,
                                                              int partSize) {
        byte[] expectedBody = util.stubAllParts(BUCKET, KEY, amountOfPartToTest, partSize);
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();

        T response = multipartClient.getObject(b -> b.bucket(BUCKET).key(KEY), transformer).join();

        byte[] body = supplier.body(response);
        assertArrayEquals(expectedBody, body);
        util.verifyCorrectAmountOfRequestsMade(amountOfPartToTest);
    }

    @ParameterizedTest
    @MethodSource("partSizeAndTransformerParams")
    public <T> void errorOnThirdPart_shouldCompleteExceptionallyOnlyPartsGreaterThanTwo(
        AsyncResponseTransformerTestSupplier<T> supplier,
        int amountOfPartToTest,
        int partSize) {
        util.stubForPart(BUCKET, KEY, 1, 3, partSize);
        util.stubForPart(BUCKET, KEY, 2, 3, partSize);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=3", BUCKET, KEY))).willReturn(
            aResponse()
                .withStatus(400)
                .withBody("<Error><Code>400</Code><Message>test error message</Message></Error>")));
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());

        if (partSize > 1) {
            assertThatThrownBy(() -> {
                T res = multipartClient.getObject(b -> b.bucket(BUCKET).key(KEY), transformer).join();
                supplier.body(res);
            }).hasMessageContaining("test error message");
        } else {
            T res = split.resultFuture().join();
            assertNotNull(supplier.body(res));
        }
    }

    @ParameterizedTest
    @MethodSource("partSizeAndTransformerParams")
    public <T> void partCountValidationFailure_shouldThrowException(
        AsyncResponseTransformerTestSupplier<T> supplier,
        int partSize) {

        // To trigger the partCount failure, the resumeContext is used to initialize the actualGetCount larger than the
        // totalPart number set in the response. This won't happen in real scenario, just to test if the error can be surfaced
        // to the user if the validation fails.
        MultipartDownloadResumeContext resumeContext = new MultipartDownloadResumeContext();
        resumeContext.addCompletedPart(1);
        resumeContext.addCompletedPart(2);
        resumeContext.addCompletedPart(3);
        resumeContext.addToBytesToLastCompletedParts(3 * partSize);

        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(BUCKET)
                                                   .key(KEY)
                                                   .overrideConfiguration(config -> config
                                                       .putExecutionAttribute(
                                                           MULTIPART_DOWNLOAD_RESUME_CONTEXT,
                                                           resumeContext))
                                                   .build();

        util.stubForPart(BUCKET, KEY, 4, 2, partSize);

        // Skip the lazy transformer since the error won't surface unless the content is consumed
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        if (transformer instanceof InputStreamResponseTransformer || transformer instanceof PublisherAsyncResponseTransformer) {
            return;
        }

        assertThatThrownBy(() -> {
            T res = multipartClient.getObject(request, transformer).join();
            supplier.body(res);
        }).isInstanceOf(CompletionException.class)
          .hasCauseInstanceOf(SdkClientException.class)
          .hasMessageContaining("PartsCount validation failed. Expected 2, downloaded 4 parts");

    }

    @ParameterizedTest
    @MethodSource("nonRetryableResponseTransformers")
    public <T> void errorOnFirstPart_shouldFail(AsyncResponseTransformerTestSupplier<T> supplier) {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY))).willReturn(
            aResponse()
                .withStatus(400)
                .withBody("<Error><Code>400</Code><Message>test error message</Message></Error>")));
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();

        assertThatThrownBy(() -> multipartClient.getObject(b -> b.bucket(BUCKET).key(KEY), transformer).join())
            .hasMessageContaining("test error message");
    }

    @ParameterizedTest
    @MethodSource("nonRetryableResponseTransformers")
    public <T> void nonRetryableResponseTransformers_ioErrorOnFirstPart_shouldFailAndNotRetry(AsyncResponseTransformerTestSupplier<T> supplier) {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY)))
                    .willReturn(aResponse()
                                    .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> multipartClient.getObject(b -> b.bucket(BUCKET).key(KEY),
                                                           supplier.transformer()).join())
            .satisfiesAnyOf(
                throwable -> assertThat(throwable)
                    .hasMessageContaining("The connection was closed during the request"),

                throwable -> assertThat(throwable)
                    .hasMessageContaining("Connection reset")
            );

        verify(1, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, KEY))));
    }

    @ParameterizedTest
    @MethodSource("nonRetryableResponseTransformers")
    public void getObject_single500WithinMany200s_shouldNotRetryError(AsyncResponseTransformerTestSupplier<?> transformerSupplier) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        int numRuns = 50;
        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<?> resp = mock200Response(multipartClient, i, transformerSupplier);
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
            multipartClient.getObject(r -> r.bucket(BUCKET).key(errorKey), transformerSupplier.transformer());
        futures.add(requestWithRetryableError);

        for (int i = 0; i < numRuns; i++) {
            CompletableFuture<?> resp = mock200Response(multipartClient, i + 1000, transformerSupplier);
            futures.add(resp);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            fail("Expecting 500 error to fail request.");
        } catch (CompletionException e) {
            assertThat(e.getCause()).isInstanceOf(S3Exception.class);
        }

        verify(moreThan(0), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, errorKey))));
        verify(lessThanOrExactly(2), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", BUCKET, errorKey))));
    }

    private static Stream<Arguments> partSizeAndTransformerParams() {
        // amount of part, individual part size
        List<Pair<Integer, Integer>> partSizes = Arrays.asList(
            Pair.of(4, 16),
            Pair.of(1, 1024),
            Pair.of(31, 1243),
            Pair.of(16, 16 * 1024),
            Pair.of(1, 1024 * 1024),
            Pair.of(4, 1024 * 1024),
            Pair.of(1, 4 * 1024 * 1024),
            Pair.of(4, 6 * 1024 * 1024),
            Pair.of(7, 5 * 3752)
        );

        Stream.Builder<Arguments> sb = Stream.builder();
        transformersSuppliers().forEach(tr -> partSizes.forEach(p -> sb.accept(arguments(tr, p.left(), p.right()))));
        return sb.build();
    }


    /**
     * Testing response transformers that are not retryable when
     * {@link AsyncResponseTransformer#split(SplittingTransformerConfiguration)} is invoked and used with
     * {@link SplittingTransformer} - {@link PublisherAsyncResponseTransformer}, {@link InputStreamResponseTransformer}, and
     * {@link FileAsyncResponseTransformer}
     * <p>
     *
     * Retry for multipart download is supported for {@link ByteArrayAsyncResponseTransformer}, tested in
     * {@link S3MultipartClientGetObjectRetryBehaviorWiremockTest}.
     */
    private static Stream<AsyncResponseTransformerTestSupplier<?>> nonRetryableResponseTransformers() {
        return Stream.of(new AsyncResponseTransformerTestSupplier.InputStreamArtSupplier(),
                         new AsyncResponseTransformerTestSupplier.PublisherArtSupplier(),
                         new AsyncResponseTransformerTestSupplier.FileArtSupplier());
    }

    private CompletableFuture<?> mock200Response(S3AsyncClient s3Client, int runNumber,
                                                 AsyncResponseTransformerTestSupplier<?> transformerSupplier) {
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
                                  transformerSupplier.transformer());
    }
}
