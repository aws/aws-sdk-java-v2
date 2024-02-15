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

package software.amazon.awssdk.core.internal.metrics;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.internal.progress.listener.ProgressUpdater;

public class BytesSentTrackingSubscriber implements Subscriber<ByteBuffer> {

    private Subscriber<? super ByteBuffer> subscriber;
    private ProgressUpdater progressUpdater;

    public BytesSentTrackingSubscriber(Subscriber<? super ByteBuffer> subscriber, Optional<ProgressUpdater> progressUpdater) {
        this.subscriber = subscriber;
        progressUpdater.ifPresent(value -> {
            this.progressUpdater = value;
        });
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        subscriber.onNext(byteBuffer);

        if(progressUpdater != null) {
            progressUpdater.incrementBytesSent(byteBuffer.remaining());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
