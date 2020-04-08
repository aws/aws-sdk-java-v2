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

package software.amazon.awssdk.metrics.meter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class LongCounterTest {

    private Counter<Long> counter;

    @Before
    public void setup() {
        counter = LongCounter.create();
    }

    @Test
    public void noarg_increment() {
        counter.increment();
        assertThat(counter.count()).isEqualTo(1L);
    }

    @Test
    public void noarg_decrement() {
        counter.decrement();
        assertThat(counter.count()).isEqualTo(-1L);
    }

    @Test
    public void increment_positive_values() {
        counter.increment(5L);
        counter.increment();

        assertThat(counter.count()).isEqualTo(6L);
    }

    @Test
    public void decrement_positive_values() {
        counter.decrement(5L);

        assertThat(counter.count()).isEqualTo(-5L);
    }

    @Test (expected = IllegalArgumentException.class)
    public void increment_negative_values() {
        counter.increment(-5L);
    }

    @Test (expected = IllegalArgumentException.class)
    public void decrement_negative_values() {
        counter.decrement(-5L);
    }

    @Test
    public void operate_on_zero_values() {
        counter.increment(0L);
        counter.decrement(0L);
        counter.decrement(0L);

        assertThat(counter.count()).isEqualTo(0L);
    }

    @Test
    public void increment_boundary_values() {
        counter.increment(Long.MAX_VALUE);

        assertThat(counter.count()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void decrement_boundary_values() {
        counter.decrement(Long.MAX_VALUE);

        assertThat(counter.count()).isEqualTo(-Long.MAX_VALUE);
    }
}
