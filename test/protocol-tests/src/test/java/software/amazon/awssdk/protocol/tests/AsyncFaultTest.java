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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;


public class AsyncFaultTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);


    private ProtocolRestJsonAsyncClient client;

    @Before
    public void setup() {
        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_1)
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                            .build();

    }

    @Test
    public void subscriberCancel_correctExceptionThrown() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200)));
        assertThatThrownBy(() -> client.streamingOutputOperation(SdkBuilder::build, new CancelSubscriptionTransformer()).join())
            .hasRootCauseExactlyInstanceOf(SelfCancelException.class);
    }

    private static class CancelSubscriptionTransformer
        implements AsyncResponseTransformer<StreamingOutputOperationResponse, ResponseBytes<StreamingOutputOperationResponse>> {

        private volatile CompletableFuture<byte[]> cf;
        private volatile StreamingOutputOperationResponse response;

        @Override
        public CompletableFuture<ResponseBytes<StreamingOutputOperationResponse>> prepare() {
            cf = new CompletableFuture<>();
            return cf.thenApply(arr -> ResponseBytes.fromByteArray(response, arr));
        }

        @Override
        public void onResponse(StreamingOutputOperationResponse response) {
            this.response = response;
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            publisher.subscribe(new CancelSubscriber(cf));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            cf.completeExceptionally(error);

        }

        private static class CancelSubscriber implements Subscriber<ByteBuffer> {
            private final CompletableFuture<byte[]> resultFuture;

            private Subscription subscription;

            CancelSubscriber(CompletableFuture<byte[]> resultFuture) {
                this.resultFuture = resultFuture;
            }

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                subscription.cancel();
                resultFuture.completeExceptionally(new SelfCancelException());
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
            }

            @Override
            public void onError(Throwable t) {
                resultFuture.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                resultFuture.complete("hello world".getBytes());
            }
        }
    }

    private static class SelfCancelException extends RuntimeException {

    }
}
