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

package software.amazon.awssdk.core.internal.metrics;

import java.time.Instant;
import java.util.List;

public interface SdkMetricCollection extends Iterable<SdkMetric<?>> {
    /**
     * Returns all metrics within this collection of a given type, does not search into children
     */
    <T> List<SdkMetric<T>> query(SdkMetricType<T> type);

    /**
     * Returns all metrics within this collection of a given type and specific identifier, does not search into children
     */
    <T> List<SdkMetric<T>> query(SdkMetricType<T> type, String identifier);

    Instant startTime();

    Instant endTime();

    SdkMetricContext<SdkMetricCollection> context();
}
