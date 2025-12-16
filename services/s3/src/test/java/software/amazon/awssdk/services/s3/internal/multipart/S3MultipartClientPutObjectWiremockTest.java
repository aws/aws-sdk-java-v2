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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.lessThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThan;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.reactivex.rxjava3.core.Flowable;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.async.BufferedSplittableAsyncRequestBody;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WireMockTest
@Timeout(120)
public class S3MultipartClientPutObjectWiremockTest {

    private static final String BUCKET = "Example-Bucket";
    private static final String KEY = "Example-Object";
    private static final String CREATE_MULTIPART_PAYLOAD = "<InitiateMultipartUploadResult>\n"
                                                           + "   <Bucket>string</Bucket>\n"
                                                           + "   <Key>string</Key>\n"
                                                           + "   <UploadId>string</UploadId>\n"
                                                           + "</InitiateMultipartUploadResult>";
    private S3AsyncClient s3AsyncClient;

    public static Stream<Arguments> retryableErrorTestCase() {
        return Stream.of(
            Arguments.of("unknownContentLength_failOfConnectionReset", null,
                         aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)),
            Arguments.of("unknownContentLength_failOf500", null,
                         aResponse().withStatus(500)),
            Arguments.of("knownContentLength_failOfConnectionReset", 20L,
                         aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)),
            Arguments.of("knownContentLength_failOf500", 20L,
                         aResponse().withStatus(500))
        );
    }

    @BeforeEach
    public void setup(WireMockRuntimeInfo wiremock) {
        s3AsyncClient = S3AsyncClient.builder()
                                     .region(Region.US_EAST_1)
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .credentialsProvider(
                                         StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                     .multipartEnabled(true)
                                     .multipartConfiguration(b -> b.minimumPartSizeInBytes(10L).apiCallBufferSizeInBytes(20L))
                                     .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                                     .build();
    }

    private void stubPutObject404Calls() {
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody(CREATE_MULTIPART_PAYLOAD)));
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(404)));
        stubFor(put(urlEqualTo("/Example-Bucket/Example-Object?partNumber=1&uploadId=string")).willReturn(aResponse().withStatus(200)));
        stubFor(delete(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    // https://github.com/aws/aws-sdk-java-v2/issues/4801
    @Test
    void uploadWithUnknownContentLength_onePartFails_shouldCancelUpstream() {
        stubPutObject404Calls();
        BlockingInputStreamAsyncRequestBody blockingInputStreamAsyncRequestBody = AsyncRequestBody.forBlockingInputStream(null);
        CompletableFuture<PutObjectResponse> putObjectResponse = s3AsyncClient.putObject(
            r -> r.bucket(BUCKET).key(KEY), blockingInputStreamAsyncRequestBody);

        assertThatThrownBy(() -> {
            try (InputStream inputStream = createUnlimitedInputStream()) {
                blockingInputStreamAsyncRequestBody.writeInputStream(inputStream);
            }
        }).isInstanceOf(CancellationException.class);

        assertThatThrownBy(() -> putObjectResponse.join()).hasRootCauseInstanceOf(S3Exception.class);
    }

    @Test
    void uploadWithKnownContentLength_onePartFails_shouldCancelUpstream() {
        stubPutObject404Calls();
        BlockingInputStreamAsyncRequestBody blockingInputStreamAsyncRequestBody =
            AsyncRequestBody.forBlockingInputStream(1024L * 20); // must be larger than the buffer used in
        // InputStreamConsumingPublisher to trigger the error
        CompletableFuture<PutObjectResponse> putObjectResponse = s3AsyncClient.putObject(
            r -> r.bucket(BUCKET).key(KEY), blockingInputStreamAsyncRequestBody);

        assertThatThrownBy(() -> {
            try (InputStream inputStream = createUnlimitedInputStream()) {
                blockingInputStreamAsyncRequestBody.writeInputStream(inputStream);
            }
        }).isInstanceOf(CancellationException.class);

        assertThatThrownBy(() -> putObjectResponse.join()).hasRootCauseInstanceOf(S3Exception.class);
    }

    @ParameterizedTest
    @MethodSource("retryableErrorTestCase")
    void mpuWithBufferedSplittableAsyncRequestBody_partsFailOfRetryableError_shouldRetry(String description,
                                                   Long contentLength,
                                                   ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubUploadPartFailsInitialAttemptSucceedsUponRetryCalls(responseDefinitionBuilder);
        List<ByteBuffer> buffers = new ArrayList<>();
        buffers.add(SdkBytes.fromUtf8String(RandomStringUtils.randomAscii(10)).asByteBuffer());
        buffers.add(SdkBytes.fromUtf8String(RandomStringUtils.randomAscii(10)).asByteBuffer());
        AsyncRequestBody asyncRequestBody = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.ofNullable(contentLength);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromIterable(buffers).subscribe(s);
            }
        };

        s3AsyncClient.putObject(b -> b.bucket(BUCKET).key(KEY), BufferedSplittableAsyncRequestBody.create(asyncRequestBody))
                     .join();

        verify(moreThan(1), putRequestedFor(anyUrl()).withQueryParam("partNumber", matching(String.valueOf(1))));
        verify(lessThanOrExactly(3), putRequestedFor(anyUrl()).withQueryParam("partNumber", matching(String.valueOf(1))));

        verify(moreThan(1), putRequestedFor(anyUrl()).withQueryParam("partNumber", matching(String.valueOf(2))));
        verify(lessThanOrExactly(3), putRequestedFor(anyUrl()).withQueryParam("partNumber", matching(String.valueOf(2))));
    }

    @ParameterizedTest
    @MethodSource("retryableErrorTestCase")
    void mpuDefaultSplitImpl_partsFailOfRetryableError_shouldFail(String description,
                                                                  Long contentLength,
                                                                  ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubUploadPartFailsInitialAttemptSucceedsUponRetryCalls(responseDefinitionBuilder);
        List<ByteBuffer> buffers = new ArrayList<>();
        buffers.add(SdkBytes.fromUtf8String(RandomStringUtils.randomAscii(10)).asByteBuffer());
        buffers.add(SdkBytes.fromUtf8String(RandomStringUtils.randomAscii(10)).asByteBuffer());
        AsyncRequestBody asyncRequestBody = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.ofNullable(contentLength);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromIterable(buffers).subscribe(s);
            }
        };

        assertThatThrownBy(() -> s3AsyncClient.putObject(b -> b.bucket(BUCKET).key(KEY), asyncRequestBody)
                     .join())
            .hasCauseInstanceOf(NonRetryableException.class)
            .hasMessageContaining("Multiple subscribers detected.");

        verify(moreThan(0), putRequestedFor(anyUrl()).withQueryParam("partNumber", matching(String.valueOf(1))));
        verify(lessThanOrExactly(2), putRequestedFor(anyUrl()).withQueryParam("partNumber", matching(String.valueOf(1))));

        verify(moreThan(0), putRequestedFor(anyUrl()).withQueryParam("partNumber", matching(String.valueOf(2))));
        verify(lessThanOrExactly(2), putRequestedFor(anyUrl()).withQueryParam("partNumber", matching(String.valueOf(2))));
    }


    private void stubUploadPartFailsInitialAttemptSucceedsUponRetryCalls(ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody(CREATE_MULTIPART_PAYLOAD)));
        stubUploadFailsInitialAttemptCalls(1, responseDefinitionBuilder);
        stubUploadFailsInitialAttemptCalls(2, responseDefinitionBuilder);
    }

    private void stubUploadFailsInitialAttemptCalls(int partNumber, ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(put(anyUrl())
                    .withQueryParam("partNumber", matching(String.valueOf(partNumber)))
                    .inScenario(String.valueOf(partNumber))
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(responseDefinitionBuilder)
                    .willSetStateTo("SecondAttempt" + partNumber));

        stubFor(put(anyUrl())
                    .withQueryParam("partNumber", matching(String.valueOf(partNumber)))
                    .inScenario(String.valueOf(partNumber))
                    .whenScenarioStateIs("SecondAttempt" + partNumber)
                    .willReturn(aResponse().withStatus(200)));
    }


    private InputStream createUnlimitedInputStream() {
        return new InputStream() {
            @Override
            public int read() {
                return 1;
            }
        };
    }
}

