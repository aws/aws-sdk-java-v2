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

import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.ThreadSafe;

@ThreadSafe
public interface SdkMetricCollector extends AutoCloseable {
    <T> void reportMetric(SdkMetricType<T> type, T value);

    default void reportMetric(SdkMetricType<Void> type) {
        reportMetric(type, null);
    }

    SdkMetricCollector createChild(SdkMetricContext<SdkMetricCollection> childContext);

    static SdkMetricCollector create(SdkMetricContext<SdkMetricCollection> parentContext) {
        throw new UnsupportedOperationException();
    }

    SdkMetricContext<SdkMetricCollector> context();

    Publisher<SdkMetricCollection> publisher();

    @Override
    void close();
}
