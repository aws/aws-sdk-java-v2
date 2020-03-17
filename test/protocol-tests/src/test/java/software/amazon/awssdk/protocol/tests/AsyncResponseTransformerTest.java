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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryAsyncClient;
import software.amazon.awssdk.services.protocolquery.model.ProtocolQueryException;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class AsyncResponseTransformerTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonAsyncClient jsonClient;
    private ProtocolQueryAsyncClient xmlClient;

    @Before
    public void setupClient() {
        jsonClient = ProtocolRestJsonAsyncClient.builder()
                                                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                .region(Region.US_EAST_1)
                                                .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                .build();
        xmlClient = ProtocolQueryAsyncClient.builder()
                                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                                "akid", "skid")))
                                            .region(Region.US_EAST_1)
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .build();
    }

    @Test
    public void jsonClient_nonRetriableError_shouldNotifyAsyncResponseTransformer() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(400)));
        TestAsyncResponseTransformer<StreamingOutputOperationResponse> responseTransformer = new TestAsyncResponseTransformer<>();
        assertThatThrownBy(() -> jsonClient.streamingOutputOperation(SdkBuilder::build, responseTransformer).join())
            .hasCauseExactlyInstanceOf(ProtocolRestJsonException.class);
        assertThat(responseTransformer.exceptionOccurred).isEqualTo(true);
    }

    @Test
    public void xmlClient_nonRetriableError_shouldNotifyAsyncResponseTransformer() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(400)));
        TestAsyncResponseTransformer<software.amazon.awssdk.services.protocolquery.model.StreamingOutputOperationResponse> responseTransformer =
            new TestAsyncResponseTransformer<>();
        assertThatThrownBy(() -> xmlClient.streamingOutputOperation(SdkBuilder::build, responseTransformer).join())
            .hasCauseExactlyInstanceOf(ProtocolQueryException.class);
        assertThat(responseTransformer.exceptionOccurred).isEqualTo(true);
    }

    private class TestAsyncResponseTransformer<T extends AwsResponse> implements AsyncResponseTransformer<T, Void> {
        private boolean exceptionOccurred = false;

        @Override
        public CompletableFuture prepare() {
            return new CompletableFuture<Void>();
        }

        @Override
        public void onResponse(T response) {

        }

        @Override
        public void exceptionOccurred(Throwable error) {
            exceptionOccurred = true;
        }

        @Override
        public void onStream(SdkPublisher publisher) {

        }
    }
}
