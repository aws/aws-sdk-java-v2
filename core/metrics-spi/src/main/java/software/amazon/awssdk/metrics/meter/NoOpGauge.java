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
 * A NoOp implementation of the {@link Gauge} metric
 */
@SdkPublicApi
public final class NoOpGauge implements Gauge {
    private static final NoOpGauge INSTANCE = new NoOpGauge();
    private static final Object EMPTY_OBJECT = new Object();

    private NoOpGauge() {
    }

    @Override
    public Object value() {
        return EMPTY_OBJECT;
    }

    public static NoOpGauge instance() {
        return INSTANCE;
    }
}
