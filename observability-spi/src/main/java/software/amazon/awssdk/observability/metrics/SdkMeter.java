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

package software.amazon.awssdk.observability.metrics;

public interface SdkMeter {

    /*
    AsyncMeasurementHandle createGauge(
        // The instrument name
        name: String,
        // Callback invoked when gauge value is read
        callback: (DoubleAsyncMeasurement) -> void,
        // (Optional) The unit of measure
        units: String? = null,
        // (Optional) A description of the metric
        description: String? = null
    );

    UpDownCounter createUpDownCounter(
        // The instrument name
        name: String,
        // (Optional) The unit of measure
        units: String? = null,
        // (Optional) A description of the metric
        description: String? = null
    );


    AsyncMeasurementHandle createAsyncUpDownCounter(
        // The instrument name
        name: String,
        // Callback invoked when gauge value is read
        callback: (LongAsyncMeasurement) -> void,
        // (Optional) The unit of measure
        units: String? = null,
        // (Optional) A description of the metric
        description: String? = null
    );

    MonotonicCounter createCounter(
        // The instrument name
        name: String,
        // (Optional) The unit of measure
        units: String? = null,
        // (Optional) A description of the metric
        description: String? = null
    );

    AsyncMeasurementHandle createAsyncMonotonicCounter(
        // The instrument name
        name: String,
        // Callback invoked when gauge value is read
        callback: (LongAsyncMeasurement) -> void,
        // (Optional) The unit of measure
        units: String? = null,
        // (Optional) A description of the metric
        description: String? = null
    );

    Histogram createHistogram(
        // The instrument name
        name: String,
        // (Optional) The unit of measure
        units: String? = null,
        // (Optional) A description of the metric
        description: String? = null
    );
     */
}
