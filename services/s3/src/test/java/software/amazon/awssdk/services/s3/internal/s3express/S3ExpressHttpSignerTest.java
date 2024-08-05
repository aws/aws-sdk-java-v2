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

package software.amazon.awssdk.services.s3.internal.s3express;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.StringInputStream;

class S3ExpressHttpSignerTest {

    private HttpSigner<AwsCredentialsIdentity> noopDelegateMockSigner;

    private HttpSigner<S3ExpressSessionCredentials> signer;

    @BeforeEach
    void init() {
        mockNoopSigner();
        this.signer = DefaultS3ExpressHttpSigner.create(noopDelegateMockSigner);
    }

    @AfterEach
    void reset() {
        Mockito.reset(noopDelegateMockSigner);
    }

    @Test
    void signSyncRequest_doesSignRequest() {
        ContentStreamProvider payload = () -> new StringInputStream("this-is\nthe-sync-full-request-body");
        SignRequest<S3ExpressSessionCredentials> signRequest =
            SignRequest.builder(S3ExpressSessionCredentials
                                    .create("access-key", "secret", "session-token"))
                       .request(SdkHttpFullRequest.builder()
                                                  .protocol("https")
                                                  .host("sync.dummy.host")
                                                  .method(SdkHttpMethod.GET)
                                                  .contentStreamProvider(payload)
                                                  .build())
                       .payload(payload)
                       .build();

        SignedRequest signedRequest = signer.sign(signRequest);

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-s3session-token"))
            .isNotEmpty()
            .contains("session-token");
        verify(noopDelegateMockSigner, times(1))
            .sign(any(SignRequest.class));
        assertThat(signedRequest.payload()).isPresent();
        signedRequest.payload().ifPresent(
            c -> assertThat(c.newStream()).hasSameContentAs(new StringInputStream("this-is\nthe-sync-full-request-body")));

        SdkHttpFullRequest httpRequest = (SdkHttpFullRequest) signedRequest.request();
        assertThat(httpRequest.protocol()).isEqualTo("https");
        assertThat(httpRequest.host()).isEqualTo("sync.dummy.host");
        assertThat(httpRequest.method()).isEqualTo(SdkHttpMethod.GET);
        assertThat(httpRequest.contentStreamProvider()).isPresent();
        httpRequest.contentStreamProvider().ifPresent(
            c -> assertThat(c.newStream()).hasSameContentAs(new StringInputStream("this-is\nthe-sync-full-request-body")));
    }

    @Test
    void signAsyncRequest_doesSignRequest() {
        StringTestSubscriber subscriber = new StringTestSubscriber();
        AsyncRequestBody payload = AsyncRequestBody.fromString("this-is\nthe-async-full-request-body");
        AsyncSignRequest<S3ExpressSessionCredentials> signRequest =
            AsyncSignRequest.builder(S3ExpressSessionCredentials
                                         .create("access-key", "secret", "session-token"))
                            .request(SdkHttpFullRequest.builder()
                                                       .protocol("https")
                                                       .host("async.dummy.host")
                                                       .method(SdkHttpMethod.GET)
                                                       .contentStreamProvider(() -> new StringInputStream(
                                                           "this-is\nthe-async-full-request-body"))
                                                       .build())
                            .payload(payload)
                            .build();

        AsyncSignedRequest signedRequest = CompletableFutureUtils.joinLikeSync(signer.signAsync(signRequest));

        assertThat(signedRequest.request().firstMatchingHeader("x-amz-s3session-token"))
            .isNotEmpty()
            .contains("session-token");
        verify(noopDelegateMockSigner, times(1)).signAsync(any(AsyncSignRequest.class));
        assertThat(signedRequest.payload()).isNotEmpty();
        signedRequest.payload().ifPresent(p -> {
            p.subscribe(subscriber);
            assertThat(subscriber.getStr()).isEqualTo("this-is\nthe-async-full-request-body");
        });

        SdkHttpFullRequest httpRequest = (SdkHttpFullRequest) signedRequest.request();
        assertThat(httpRequest.protocol()).isEqualTo("https");
        assertThat(httpRequest.host()).isEqualTo("async.dummy.host");
        assertThat(httpRequest.method()).isEqualTo(SdkHttpMethod.GET);
        assertThat(httpRequest.contentStreamProvider()).isPresent();
        httpRequest.contentStreamProvider().ifPresent(
            c -> assertThat(c.newStream()).hasSameContentAs(new StringInputStream("this-is\nthe-async-full-request-body")));
    }

    @Test
    void signSyncRequest_delegateThrowException_shouldRethrow() {
        when(noopDelegateMockSigner.sign(any(SignRequest.class)))
            .thenThrow(new RuntimeException("Poof!"));
        SignRequest<S3ExpressSessionCredentials> signRequest =
            SignRequest.builder(S3ExpressSessionCredentials
                                    .create("access-key", "secret", "session-token"))
                       .request(SdkHttpFullRequest.builder()
                                                  .protocol("https")
                                                  .host("sync.dummy.host")
                                                  .method(SdkHttpMethod.GET)
                                                  .build())
                       .build();
        assertThatThrownBy(() -> signer.sign(signRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Poof!");
    }

    @Test
    void signAsyncRequest_delegateFails_shouldFailsReturnFuture() {
        CompletableFuture<Identity> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Poof!"));
        when(noopDelegateMockSigner.signAsync(any(AsyncSignRequest.class)))
            .thenReturn(failedFuture);

        AsyncSignRequest<S3ExpressSessionCredentials> signRequest =
            AsyncSignRequest.builder(S3ExpressSessionCredentials
                                         .create("access-key", "secret", "session-token"))
                            .request(SdkHttpFullRequest.builder()
                                                       .protocol("https")
                                                       .host("async.dummy.host")
                                                       .method(SdkHttpMethod.GET)
                                                       .build())
                            .build();

        CompletableFuture<?> signedFuture = signer.signAsync(signRequest);

        assertThat(signedFuture).isCompletedExceptionally();
        assertThatThrownBy(signedFuture::get).hasMessage("java.lang.RuntimeException: Poof!");
    }

    private void mockNoopSigner() {
        this.noopDelegateMockSigner = mock(HttpSigner.class);
        when(noopDelegateMockSigner.sign(any(SignRequest.class)))
            .thenAnswer(invocation -> {
                SignRequest<? extends AwsCredentialsIdentity> request = invocation.getArgument(0);
                SignedRequest.Builder b = SignedRequest.builder().request(request.request());
                request.payload().ifPresent(b::payload);
                return b.build();
            });
        when(noopDelegateMockSigner.signAsync(any(AsyncSignRequest.class)))
            .thenAnswer(invocation -> {
                AsyncSignRequest<? extends AwsCredentialsIdentity> request = invocation.getArgument(0);
                AsyncSignedRequest.Builder b = AsyncSignedRequest.builder().request(request.request());
                request.payload().ifPresent(b::payload);
                return CompletableFuture.completedFuture(b.build());
            });
    }

    static class StringTestSubscriber implements Subscriber<ByteBuffer> {
        StringBuilder str = new StringBuilder();

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer bb) {
            byte[] bytes = new byte[bb.remaining()];
            bb.get(bytes);
            str.append(new String(bytes));
        }

        @Override
        public void onError(Throwable t) {
            // does nothing, for test only
        }

        @Override
        public void onComplete() {
            // does nothing, for test only
        }

        public String getStr() {
            return str.toString();
        }
    }
}
