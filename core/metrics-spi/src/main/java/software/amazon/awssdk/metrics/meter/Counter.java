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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A metric type to store incrementing and decrementing values
 *
 * @param <T> type of the stored value
 */
@SdkPublicApi
public interface Counter<T> extends Metric, Counting<T> {

    /**
     * Increment the metric value by 1 unit
     */
    void increment();

    /**
     * Increment the metric value by given #value amount
     * @param value amount to increment the metric value
     */
    void increment(T value);

    /**
     * Decrement the metric value by 1 unit
     */
    void decrement();

    /**
     * Decrement the metric value by given #value amount
     * @param value amount to decrement the metric value
     */
    void decrement(T value);
}
