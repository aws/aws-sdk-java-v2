/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.utils.async;

import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class BufferingSubscriber<T> extends DelegatingSubscriber<T, List<T>> {
    private final int bufferSize;
    private List<T> currentBuffer;

    public BufferingSubscriber(Subscriber<? super List<T>> subscriber, int bufferSize) {
        super(subscriber);
        this.bufferSize = bufferSize;
        currentBuffer = new ArrayList<>(bufferSize);
    }

    @Override
    public void onNext(T t) {
        currentBuffer.add(t);
        if (currentBuffer.size() == bufferSize) {
            subscriber.onNext(currentBuffer);
            currentBuffer.clear();
        }
    }

    @Override
    public void onComplete() {
        // Deliver any remaining items before calling on complete
        if (currentBuffer.size() > 0) {
            subscriber.onNext(currentBuffer);
        }
        super.onComplete();
    }
}
