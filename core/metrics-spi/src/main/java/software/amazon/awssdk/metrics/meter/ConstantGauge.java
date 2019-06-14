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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A {@link Gauge} implementation that stores a constant value for a metric.
 * The value stored cannot be changed after object creation
 *
 * @param <TypeT> the type of the value recorded in the Gauge
 */
@SdkPublicApi
public final class ConstantGauge<TypeT> implements Gauge<TypeT> {

    private final TypeT value;

    private ConstantGauge(TypeT value) {
        this.value = value;
    }

    /**
     * @param value the value to store in the guage
     * @param <T> type of the value
     * @return An instance of {@link ConstantGauge} with the given {@link #value} stored in the gauge.
     */
    public static <T> ConstantGauge<T> create(T value) {
        return new ConstantGauge<>(value);
    }

    @Override
    public TypeT value() {
        return value;
    }
}
