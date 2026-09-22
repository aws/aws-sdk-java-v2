/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.awscore.eventstream;

import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.utils.FunctionalUtils;

public class SequentialConsumerSubscriberTckTest extends SubscriberBlackboxVerification<Integer> {

    public SequentialConsumerSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<Integer> createSubscriber() {
        return EventStreamResponseHandlerFromBuilder.sequentialConsumerSubscriber(i -> {},
                                                                                  FunctionalUtils.noOpConsumer());
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
