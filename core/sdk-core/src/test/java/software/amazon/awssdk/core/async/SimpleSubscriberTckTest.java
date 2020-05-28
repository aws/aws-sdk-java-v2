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

package software.amazon.awssdk.core.async;

import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.http.async.SimpleSubscriber;

/**
 * TCK verifiation test for {@link SimpleSubscriber}.
 */
public class SimpleSubscriberTckTest extends org.reactivestreams.tck.SubscriberBlackboxVerification<ByteBuffer> {

    public SimpleSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber() {
        return new SimpleSubscriber(buffer -> {
            // ignore
        });
    }

    @Override
    public ByteBuffer createElement(int i) {
        return ByteBuffer.wrap(String.valueOf(i).getBytes());
    }

}
