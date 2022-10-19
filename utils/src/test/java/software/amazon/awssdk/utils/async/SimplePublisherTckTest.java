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

package software.amazon.awssdk.utils.async;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

public class SimplePublisherTckTest extends PublisherVerification<Integer> {
    public SimplePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        for (int i = 0; i < elements; i++) {
            publisher.send(i);
        }
        publisher.complete();
        return publisher;
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        SimplePublisher<Integer> publisher = new SimplePublisher<>();
        publisher.error(new RuntimeException());
        return publisher;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 256L;
    }
}