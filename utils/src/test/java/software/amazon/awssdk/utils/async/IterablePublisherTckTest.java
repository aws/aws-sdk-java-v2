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

import java.util.Iterator;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

public class IterablePublisherTckTest extends PublisherVerification<Long>  {


    public IterablePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        Iterable<Long> iterable = () -> new Iterator<Long>() {
            private long count;
            @Override
            public boolean hasNext() {
                if (count == elements) {
                    return false;
                }

                return true;
            }

            @Override
            public Long next() {
                count++;
                return count;
            }
        };
        return new IterablePublisher<>(iterable);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }
}
