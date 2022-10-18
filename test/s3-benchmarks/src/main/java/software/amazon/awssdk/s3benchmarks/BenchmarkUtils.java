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

package software.amazon.awssdk.s3benchmarks;

import static software.amazon.awssdk.transfer.s3.SizeConstant.GB;

import java.util.List;
import software.amazon.awssdk.utils.Logger;

public final class BenchmarkUtils {
    static final int PRE_WARMUP_ITERATIONS = 10;
    static final int PRE_WARMUP_RUNS = 20;
    static final int BENCHMARK_ITERATIONS = 10;
    static final String WARMUP_KEY = "warmupobject";

    private static final Logger logger = Logger.loggerFor("TransferManagerBenchmark");

    private BenchmarkUtils() {
    }

    public static void printOutResult(List<Double> metrics, String name, long contentLengthInByte) {
        logger.info(() -> String.format("===============  %s Result ================", name));
        logger.info(() -> String.valueOf(metrics));
        double averageLatency = metrics.stream()
                                       .mapToDouble(a -> a)
                                       .average()
                                       .orElse(0.0);

        double lowestLatency = metrics.stream()
                                      .mapToDouble(a -> a)
                                      .min().orElse(0.0);

        double contentLengthInGigabit = (contentLengthInByte / (double) GB) * 8.0;
        logger.info(() -> "Average latency (s): " + averageLatency);
        logger.info(() -> "Object size (Gigabit): " + contentLengthInGigabit);
        logger.info(() -> "Average throughput (Gbps): " + contentLengthInGigabit / averageLatency);
        logger.info(() -> "Highest average throughput (Gbps): " + contentLengthInGigabit / lowestLatency);
        logger.info(() -> "==========================================================");
    }
}
