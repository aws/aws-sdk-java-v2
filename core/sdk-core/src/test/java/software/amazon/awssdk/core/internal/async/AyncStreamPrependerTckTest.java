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

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import io.reactivex.Flowable;

public class AyncStreamPrependerTckTest extends PublisherVerification<Long> {
    public AyncStreamPrependerTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long l) {
        if (l == 0) {
            return Flowable.empty();
        } else {
            Flowable<Long> delegate = Flowable.rangeLong(1, l - 1);
            return new AsyncStreamPrepender<>(delegate, 0L);
        }
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return new AsyncStreamPrepender<>(Flowable.error(new NullPointerException()), 0L);
    }
}
