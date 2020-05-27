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

package software.amazon.awssdk.metrics.internal.util;

import java.time.Duration;
import java.util.concurrent.Callable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public final class MetricUtil {

    private MetricUtil() {
    }

    public static <T> Pair<T, Duration> measureDuration(Callable<T> c) {
        long start = System.nanoTime();

        T result;

        try {
            result = c.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Duration d = Duration.ofNanos(System.nanoTime() - start);

        return Pair.of(result, d);
    }
}
