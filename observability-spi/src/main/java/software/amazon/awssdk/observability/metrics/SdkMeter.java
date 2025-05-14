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

import software.amazon.awssdk.observability.attributes.Attributes;

public interface SdkMeter {

    SdkMonotonicCounter counter(String name, String units, String description);

    SdkMonotonicCounter counter(String name, String units, String description, Attributes attributes);

    SdkHistogram histogram(String name, String units, String description);

    SdkHistogram histogram(String name, String units, String description, Attributes attributes);

    SdkUpDownCounter upDownCounter(String name, String units, String description);

    SdkUpDownCounter upDownCounter(String name, String units, String description, Attributes attributes);

    SdkGauge gauge(String name, String units, String description);

    SdkGauge gauge(String name, String units, String description, Attributes attributes);
}
