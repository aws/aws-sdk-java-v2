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

package software.amazon.awssdk.protocol.tests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Verify the end-to-end functionality of the SDK-provided {@link ResponseTransformer} implementations.
 */
public class ResponseTransformerTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private static final String STREAMING_OUTPUT_PATH = "/2016-03-11/streamingOutputOperation";

    @Test
    public void bytesMethodConvertsCorrectly() {
        stubForSuccess();

        ResponseBytes<StreamingOutputOperationResponse> response =
                testClient().streamingOutputOperationAsBytes(StreamingOutputOperationRequest.builder().build());

        byte[] arrayCopy = response.asByteArray();
        assertThat(arrayCopy).containsExactly('t', 'e', 's', 't', ' ', -16, -97, -104, -126);
        arrayCopy[0] = 'X'; // Mutate the returned byte array to make sure it's a copy

        ByteBuffer buffer = response.asByteBuffer();
        assertThat(buffer.isReadOnly()).isTrue();
        assertThat(BinaryUtils.copyAllBytesFrom(buffer)).containsExactly('t', 'e', 's', 't', ' ', -16, -97, -104, -126);

        assertThat(response.asString(StandardCharsets.UTF_8)).isEqualTo("test \uD83D\uDE02");
        assertThat(response.asUtf8String()).isEqualTo("test \uD83D\uDE02");
    }

    @Test
    public void byteMethodDownloadFailureRetries() {
        stubForRetriesTimeoutReadingFromStreams();

        ResponseBytes<StreamingOutputOperationResponse> response =
                testClient().streamingOutputOperationAsBytes(StreamingOutputOperationRequest.builder().build());

        assertThat(response.asUtf8String()).isEqualTo("retried");
    }

    @Test
    public void downloadToFileRetriesCorrectly() throws IOException {
        stubForRetriesTimeoutReadingFromStreams();

        Path tmpDirectory = Files.createTempDirectory("streaming-response-handler-test");
        tmpDirectory.toFile().deleteOnExit();

        Path tmpFile = tmpDirectory.resolve(UUID.randomUUID().toString());
        tmpFile.toFile().deleteOnExit();
        
        testClient().streamingOutputOperation(StreamingOutputOperationRequest.builder().build(), tmpFile);

        assertThat(Files.readAllLines(tmpFile)).containsExactly("retried");
    }

    @Test
    public void downloadToExistingFileDoesNotRetry() throws IOException {
        stubForRetriesTimeoutReadingFromStreams();

        assertThatThrownBy(() -> testClient().streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
            ResponseTransformer
                .toFile(new File(".."))))
            .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void downloadToOutputStreamDoesNotRetry() throws IOException {
        stubForRetriesTimeoutReadingFromStreams();

        assertThatThrownBy(() -> testClient().streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                                                       ResponseTransformer
                                                                           .toOutputStream(new ByteArrayOutputStream())))
            .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void streamingCloseActuallyCloses() throws IOException {
        stubForSuccess();

        ProtocolRestJsonClient client = testClientBuilder()
                .httpClientBuilder(ApacheHttpClient.builder()
                                                   .connectionAcquisitionTimeout(Duration.ofSeconds(1))
                                                   .maxConnections(1))
                .build();


        // Two successful requests with a max of one connection means that closing the connection worked.
        client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build()).close();
        client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build()).close();
    }

    @Test
    public void streamingAbortActuallyAborts() {
        stubForSuccess();

        ProtocolRestJsonClient client = testClientBuilder()
                .httpClientBuilder(ApacheHttpClient.builder()
                                                   .connectionAcquisitionTimeout(Duration.ofSeconds(1))
                                                   .maxConnections(1))
                .build();


        // Two successful requests with a max of one connection means that closing the connection worked.
        client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build()).abort();
        client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build()).abort();
    }

    private void stubForRetriesTimeoutReadingFromStreams() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).inScenario("retries")
                                                           .whenScenarioStateIs(STARTED)
                                                           .willReturn(aResponse().withStatus(200).withBody("first")
                                                                                  .withHeader("Content-Length", "100"))
                                                           .willSetStateTo("Retry"));

        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).inScenario("retries")
                                                           .whenScenarioStateIs("Retry")
                                                           .willReturn(aResponse().withStatus(200).withBody("retried")));
    }

    private ProtocolRestJsonClient testClient() {
        return testClientBuilder().build();
    }

    private ProtocolRestJsonClientBuilder testClientBuilder() {
        return ProtocolRestJsonClient.builder()
                                     .region(Region.US_WEST_1)
                                     .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                     .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                     .httpClientBuilder(ApacheHttpClient.builder().socketTimeout(Duration.ofSeconds(1)));
    }

    private StubMapping stubForSuccess() {
        return stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH)).willReturn(aResponse().withStatus(200).withBody("test \uD83D\uDE02")));
    }
}
