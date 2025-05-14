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

package software.amazon.awssdk.observability.micrometer.internal;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import software.amazon.awssdk.observability.attributes.Attributes;
import software.amazon.awssdk.observability.metrics.SdkMeter;
import software.amazon.awssdk.observability.metrics.SdkMeterProvider;

public class MicrometerMeterProvider implements SdkMeterProvider {
    private final MeterRegistry meterRegistry;

    public MicrometerMeterProvider(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public SdkMeter meter(String scope) {
        return new MicrometerMeter(meterRegistry, scope);
    }

    @Override
    public SdkMeter meter(String scope, Attributes attributes) {
        Tags tags = MicrometerAttributeConverter.toMicrometerTags(attributes);
        return new MicrometerMeter(meterRegistry, scope, tags);
    }
}
