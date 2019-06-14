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

import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A basic implementation of {@link Gauge} that has ability to set, update and return a single value.
 *
 * @param <TypeT> type of the value stored in gauge
 */
@SdkPublicApi
public final class DefaultGauge<TypeT> implements Gauge<TypeT> {

    private final AtomicReference<TypeT> atomicReference;

    private DefaultGauge(TypeT initialValue) {
        this.atomicReference = new AtomicReference<>(initialValue);
    }

    /**
     *
     * @param initialValue the value to stored in the gauge instance when its created
     * @param <T> type of the value
     * @return An instance of {@link DefaultGauge} with the given #initialValue stored in the gauge.
     */
    public static <T> DefaultGauge<T> create(T initialValue) {
        return new DefaultGauge<>(initialValue);
    }

    /**
     * Update the value stored in the gauge
     * @param value the new value to store in the gauge
     */
    public void value(TypeT value) {
        this.atomicReference.set(value);
    }

    @Override
    public TypeT value() {
        return atomicReference.get();
    }
}
