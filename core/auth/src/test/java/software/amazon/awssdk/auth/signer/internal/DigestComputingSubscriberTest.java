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

package software.amazon.awssdk.auth.signer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.utils.BinaryUtils;

public class DigestComputingSubscriberTest {

    @Test
    public void test_computesCorrectSha256() {
        String testString = "AWS SDK for Java";
        String expectedDigest = "004c6bbd87e7fe70109b3bc23c8b1ab8f18a8bede0ed38c9233f6cdfd4f7b5d6";

        DigestComputingSubscriber subscriber = DigestComputingSubscriber.forSha256();

        Flowable<ByteBuffer> publisher = Flowable.just(ByteBuffer.wrap(testString.getBytes(StandardCharsets.UTF_8)));

        publisher.subscribe(subscriber);

        String computedDigest = BinaryUtils.toHex(subscriber.digestBytes().join());

        assertThat(computedDigest).isEqualTo(expectedDigest);
    }

    @Test
    public void test_futureCancelledBeforeSubscribe_cancelsSubscription() {
        Subscription mockSubscription = mock(Subscription.class);

        DigestComputingSubscriber subscriber = DigestComputingSubscriber.forSha256();
        subscriber.digestBytes().cancel(true);

        subscriber.onSubscribe(mockSubscription);

        verify(mockSubscription).cancel();
        verify(mockSubscription, times(0)).request(anyLong());
    }

    @Test
    public void test_publisherCallsOnError_errorPropagatedToFuture() {
        Subscription mockSubscription = mock(Subscription.class);

        DigestComputingSubscriber subscriber = DigestComputingSubscriber.forSha256();
        subscriber.onSubscribe(mockSubscription);

        RuntimeException error = new RuntimeException("error");
        subscriber.onError(error);

        assertThatThrownBy(subscriber.digestBytes()::join).hasCause(error);
    }
}
