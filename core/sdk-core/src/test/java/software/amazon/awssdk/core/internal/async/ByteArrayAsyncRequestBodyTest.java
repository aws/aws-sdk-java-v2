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

package software.amazon.awssdk.core.internal.async;

import static org.junit.Assert.assertTrue;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ByteArrayAsyncRequestBodyTest {
    private class testSubscriber implements Subscriber<ByteBuffer> {
        private Subscription subscription;
        protected AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {
            subscription.request(1);
            onCompleteCalled.set(true);
        }
    }

    testSubscriber subscriber = new testSubscriber();

    @Test
    public void concurrentRequests_shouldCompleteNormally() {
        ByteArrayAsyncRequestBody byteArrayReq = new ByteArrayAsyncRequestBody("Hello World!".getBytes());
        byteArrayReq.subscribe(subscriber);
        assertTrue(subscriber.onCompleteCalled.get());
    }

}
