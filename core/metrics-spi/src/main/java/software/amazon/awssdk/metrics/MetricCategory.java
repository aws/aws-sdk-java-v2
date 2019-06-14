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

package software.amazon.awssdk.metrics;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A enum class representing the different types of metric categories in the SDK.
 *
 * A metric can be tagged with multiple categories. Clients can enable/disable metric collection
 * at {@link MetricCategory} level.
 */
@SdkPublicApi
public enum MetricCategory {
    None("none"),

    Default("default"),

    HttpClient("httpclient"),

    Streaming("streaming"),

    All("all")

    ;

    private final String value;

    MetricCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Create a {@link MetricCategory} from the given String value. This method is case insensitive.
     *
     * @param value the value to create the {@link MetricCategory} from
     * @return A {@link MetricCategory} if the given {@link #value} matches one of the enum values.
     *         Otherwise throws {@link IllegalArgumentException}
     */
    public static MetricCategory fromString(String value) {
        for (MetricCategory mc : MetricCategory.values()) {
            if (mc.value.equalsIgnoreCase(value)) {
                return mc;
            }
        }

        throw new IllegalArgumentException("MetricCategory cannot be created from value: " + value);
    }
}
