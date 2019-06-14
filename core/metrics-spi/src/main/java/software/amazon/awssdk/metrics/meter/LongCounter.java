/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics.meter;

import java.util.concurrent.atomic.LongAdder;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A {@link Counter} implementation that stores {@link Long} values.
 */
@SdkPublicApi
public final class LongCounter implements Counter<Long> {

    private final LongAdder count;

    private LongCounter() {
        this.count = new LongAdder();
    }

    public static LongCounter create() {
        return new LongCounter();
    }

    public void increment() {
        increment(1L);
    }

    public void increment(Long value) {
        count.add(value);
    }

    public void decrement() {
        decrement(1L);
    }

    public void decrement(Long value) {
        count.add(-value);
    }

    public Long count() {
        return count.sum();
    }
}
