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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WireMockTest
@Timeout(10)
public class S3MultipartClientPutObjectWiremockTest {

    private static final String BUCKET = "Example-Bucket";
    private static final String KEY = "Example-Object";
    private static final String CREATE_MULTIPART_PAYLOAD = "<InitiateMultipartUploadResult>\n"
                                                           + "   <Bucket>string</Bucket>\n"
                                                           + "   <Key>string</Key>\n"
                                                           + "   <UploadId>string</UploadId>\n"
                                                           + "</InitiateMultipartUploadResult>";
    private S3AsyncClient s3AsyncClient;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wiremock) {
        stubPutObjectCalls();
        s3AsyncClient = S3AsyncClient.builder()
                                     .region(Region.US_EAST_1)
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .credentialsProvider(
                                         StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                     .multipartEnabled(true)
                                     .multipartConfiguration(b -> b.minimumPartSizeInBytes(10L).apiCallBufferSizeInBytes(10L))
            .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
                                     .build();
    }

    private void stubPutObjectCalls() {
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody(CREATE_MULTIPART_PAYLOAD)));
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(404)));
        stubFor(put(urlEqualTo("/Example-Bucket/Example-Object?partNumber=1&uploadId=string")).willReturn(aResponse().withStatus(200)));
        stubFor(delete(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    // https://github.com/aws/aws-sdk-java-v2/issues/4801
    @Test
    void uploadWithUnknownContentLength_onePartFails_shouldCancelUpstream() {
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

    private InputStream createUnlimitedInputStream() {
        return new InputStream() {
            @Override
            public int read() {
                return 1;
            }
        };
    }
}
