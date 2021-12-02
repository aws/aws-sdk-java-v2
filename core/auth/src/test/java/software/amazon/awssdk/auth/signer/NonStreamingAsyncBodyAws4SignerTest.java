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

package software.amazon.awssdk.auth.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

public class NonStreamingAsyncBodyAws4SignerTest {
    @Test
    public void test_sign_computesCorrectSignature() {
        Aws4Signer aws4Signer = Aws4Signer.create();
        AsyncAws4Signer asyncAws4Signer = AsyncAws4Signer.create();

        byte[] content = "Hello AWS!".getBytes(StandardCharsets.UTF_8);
        ContentStreamProvider syncBody = () -> new ByteArrayInputStream(content);
        AsyncRequestBody asyncBody = AsyncRequestBody.fromBytes(content);

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                .protocol("https")
                .host("my-cool-aws-service.us-west-2.amazonaws.com")
                .method(SdkHttpMethod.GET)
                .putHeader("header1", "headerval1")
                .contentStreamProvider(syncBody)
                .build();

        AwsCredentials credentials = AwsBasicCredentials.create("akid", "skid");

        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(credentials)
                .signingClockOverride(Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")))
                .signingName("my-cool-aws-service")
                .signingRegion(Region.US_WEST_2)
                .build();

        List<String> syncSignature = aws4Signer.sign(httpRequest, signerParams).headers().get("Authorization");
        List<String> asyncSignature = asyncAws4Signer.signWithBody(httpRequest, asyncBody, signerParams).join()
                .headers().get("Authorization");

        assertThat(asyncSignature).isEqualTo(syncSignature);
    }

    @Test
    public void test_sign_publisherThrows_exceptionPropagated() {
        AsyncAws4Signer asyncAws4Signer = AsyncAws4Signer.create();

        RuntimeException error = new RuntimeException("error");
        Flowable<ByteBuffer> errorPublisher = Flowable.error(error);
        AsyncRequestBody asyncBody = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(42L);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                errorPublisher.subscribe(subscriber);
            }
        };

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                .protocol("https")
                .host("my-cool-aws-service.us-west-2.amazonaws.com")
                .method(SdkHttpMethod.GET)
                .putHeader("header1", "headerval1")
                .build();

        AwsCredentials credentials = AwsBasicCredentials.create("akid", "skid");

        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(credentials)
                .signingClockOverride(Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")))
                .signingName("my-cool-aws-service")
                .signingRegion(Region.US_WEST_2)
                .build();

        assertThatThrownBy(asyncAws4Signer.signWithBody(httpRequest, asyncBody, signerParams)::join)
                .hasCause(error);
    }

    @Test
    public void test_sign_futureCancelled_propagatedToPublisher() {
        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                .protocol("https")
                .host("my-cool-aws-service.us-west-2.amazonaws.com")
                .method(SdkHttpMethod.GET)
                .putHeader("header1", "headerval1")
                .build();

        AwsCredentials credentials = AwsBasicCredentials.create("akid", "skid");

        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(credentials)
                .signingClockOverride(Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")))
                .signingName("my-cool-aws-service")
                .signingRegion(Region.US_WEST_2)
                .build();

        AsyncRequestBody mockRequestBody = mock(AsyncRequestBody.class);
        Subscription mockSubscription = mock(Subscription.class);
        doAnswer((Answer<Void>) invocationOnMock -> {
            Subscriber subscriber = invocationOnMock.getArgumentAt(0, Subscriber.class);
            subscriber.onSubscribe(mockSubscription);
            return null;
        }).when(mockRequestBody).subscribe(any(Subscriber.class));

        AsyncAws4Signer asyncAws4Signer = AsyncAws4Signer.create();

        CompletableFuture<SdkHttpFullRequest> signedRequestFuture = asyncAws4Signer.signWithBody(httpRequest,
                mockRequestBody, signerParams);

        signedRequestFuture.cancel(true);

        verify(mockSubscription).cancel();
    }
}
