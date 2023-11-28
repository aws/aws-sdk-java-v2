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

package software.amazon.awssdk.s3benchmarks.s3express;

import java.util.function.Supplier;

/**
 * Factory to create the benchmark
 */
@FunctionalInterface
public interface Benchmark {

    /**
     * The benchmark method to run
     */
    void run();

    default <T> TimedResult<T> runWithTime(Supplier<T> toRun) {
        long start = System.currentTimeMillis();
        T result = toRun.get();
        long end = System.currentTimeMillis();
        return new TimedResult<>(result, (end - start) / 1000.0);
    }

    final class TimedResult<T> {
        private final Double latency;
        private final T result;

        public TimedResult(T result, Double latency) {
            this.result = result;
            this.latency = latency;
        }

        public Double latency() {
            return latency;
        }

        public T result() {
            return result;
        }

    }
}
