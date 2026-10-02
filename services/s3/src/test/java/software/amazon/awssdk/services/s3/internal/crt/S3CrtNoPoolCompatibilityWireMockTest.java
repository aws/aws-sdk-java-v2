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

package software.amazon.awssdk.services.s3.internal.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncResponseTransformer;

@WireMockTest
@Timeout(15)
class S3CrtNoPoolCompatibilityWireMockTest {
    private static final byte[] CONTENT = "old-crt-compatible".getBytes(StandardCharsets.UTF_8);

    @Test
    void noPoolClient_shouldKeepOrdinaryTransfersWorking(WireMockRuntimeInfo wireMock) {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("Content-Length", Integer.toString(CONTENT.length))
                                                    .withHeader("ETag", "\"etag\"")));
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)
                                                   .withHeader("Content-Length", Integer.toString(CONTENT.length))
                                                   .withHeader("ETag", "\"etag\"")
                                                   .withBody(CONTENT)));
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(200)
                                                   .withHeader("ETag", "\"etag\"")));

        try (S3AsyncClient client = S3AsyncClient.crtBuilder()
                                                 .region(Region.US_EAST_1)
                                                 .endpointOverride(URI.create("http://localhost:"
                                                                              + wireMock.getHttpPort()))
                                                 .credentialsProvider(StaticCredentialsProvider.create(
                                                     AwsBasicCredentials.create("key", "secret")))
                                                 .build()) {
            assertThat(client.getObject(r -> r.bucket("bucket").key("key"),
                                        AsyncResponseTransformer.toBytes())
                             .join()
                             .asByteArray())
                .containsExactly(CONTENT);

            client.putObject(r -> r.bucket("bucket").key("key"), AsyncRequestBody.fromBytes(CONTENT)).join();

            assertThatThrownBy(() -> client.getObject(
                r -> r.bucket("bucket").key("key"),
                S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers()).join())
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("A direct buffer pool must be configured on S3CrtAsyncClientBuilder before using the "
                                     + "borrowed buffer response transformer");
        }
    }
}
