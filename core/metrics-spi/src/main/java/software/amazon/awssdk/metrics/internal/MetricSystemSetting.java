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

package software.amazon.awssdk.metrics.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * System properties to configure metrics options in the SDK.
 */
@SdkInternalApi
public enum MetricSystemSetting implements SystemSetting {

    /**
     * Enable the Java SDK Metrics by setting this property. Metrics feature is disabled if this feature is not specified or
     * specified to anything other than "true"
     */
    AWS_JAVA_SDK_METRICS_ENABLED("aws.javasdk2x.metrics.enabled", null),

    /**
     * Specify comma separated {@link MetricCategory} values to enable the categories for metrics collection.
     * Only metrics belonging to these categories are collected by the SDK.
     *
     * <p>
     * This value is defaulted to {@link MetricCategory#DEFAULT}. If this property is not set but metrics are enabled,
     * then metrics belonging to {@link MetricCategory#DEFAULT} category are collected.
     * </p>
     */
    AWS_JAVA_SDK_METRICS_CATEGORY("aws.javasdk2x.metrics.category", "Default")

    ;

    private final String systemProperty;
    private final String defaultValue;

    MetricSystemSetting(String systemProperty, String defaultValue) {
        this.systemProperty = systemProperty;
        this.defaultValue = defaultValue;
    }

    @Override
    public String property() {
        return systemProperty;
    }

    @Override
    public String environmentVariable() {
        return name();
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }
}
