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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class BufferingSubscriber<T> implements Subscriber<T> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final List<T> bufferedItems = new ArrayList<>();
    private Throwable bufferedError = null;
    private boolean isCompleted = false;

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        bufferedItems.add(t);
    }

    @Override
    public void onError(Throwable throwable) {
        this.bufferedError = throwable;
        this.latch.countDown();
    }

    @Override
    public void onComplete() {
        this.isCompleted = true;
        this.latch.countDown();
    }

    public void waitForCompletion(long timeoutInMillis) {
        try {
            this.latch.await(timeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<T> bufferedItems() {
        return bufferedItems;
    }

    public Throwable bufferedError() {
        return bufferedError;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}
