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

package software.amazon.awssdk.benchmark.utils;

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.DEFAULT_JDK_SSL_PROVIDER;
import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.OPEN_SSL_PROVIDER;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.util.Statistics;
import software.amazon.awssdk.benchmark.stats.SdkBenchmarkStatistics;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;

/**
 * Contains utilities methods used by the benchmarks
 */
public final class BenchmarkUtils {

    private static final Logger logger = Logger.loggerFor(BenchmarkConstant.class);

    private BenchmarkUtils() {
    }

    public static void countDownUponCompletion(Blackhole blackhole,
                                               CompletableFuture<?> completableFuture,
                                               CountDownLatch countDownLatch) {
        completableFuture.whenComplete((r, t) -> {
            if (t != null) {
                logger.error(() -> "Exception returned from the response ", t);
                blackhole.consume(t);
            } else {
                blackhole.consume(r);
            }
            countDownLatch.countDown();
        });
    }

    public static SslProvider getSslProvider(String sslProviderValue) {
        switch (sslProviderValue) {
            case DEFAULT_JDK_SSL_PROVIDER:
                return SslProvider.JDK;
            case OPEN_SSL_PROVIDER:
                return SslProvider.OPENSSL;
            default:
                return SslContext.defaultClientProvider();
        }
    }

    public static void awaitCountdownLatchUninterruptibly(CountDownLatch countDownLatch, int timeout, TimeUnit unit) {
        try {
            countDownLatch.await(timeout, unit);
        } catch (InterruptedException e) {
            // No need to re-interrupt.
            logger.error(() -> "InterruptedException thrown ", e);
        }
    }

    public static AttributeMap.Builder trustAllTlsAttributeMapBuilder() {
        return AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true);
    }

    /**
     * Returns an unused port in the localhost.
     */
    public static int getUnusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    /**
     * Compare the results statistically.
     *
     * See {@link Statistics#compareTo(Statistics, double)}
     *
     * @param current the current result
     * @param other the other result to compare with
     * @param confidence the confidence level
     * @return a negative integer, zero, or a positive integer as this statistics
     * is less than, equal to, or greater than the specified statistics.
     */
    public static int compare(SdkBenchmarkStatistics current, SdkBenchmarkStatistics other, double confidence) {
        if (isDifferent(current, other, confidence)) {
            logger.info(() -> "isDifferent ? " + true);
            double t = current.getMean();
            double o = other.getMean();
            return (t > o) ? -1 : 1;
        }

        return 0;
    }

    /**
     * See {@link Statistics#compareTo(Statistics)}
     */
    public static int compare(SdkBenchmarkStatistics current, SdkBenchmarkStatistics other) {
        return compare(current, other, 0.99);
    }

    private static boolean isDifferent(SdkBenchmarkStatistics current, SdkBenchmarkStatistics other, double confidence) {
        return TestUtils.tTest(current, other, 1 - confidence);
    }
}
