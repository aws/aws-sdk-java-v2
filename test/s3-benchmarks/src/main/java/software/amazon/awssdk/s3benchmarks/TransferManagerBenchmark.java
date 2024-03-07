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

import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.time.Duration;
import java.util.function.Supplier;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.utils.Logger;

/**
 * Factory to create the benchmark
 */
@FunctionalInterface
public interface TransferManagerBenchmark {
    Logger logger = Logger.loggerFor(TransferManagerBenchmark.class);

    /**
     * The benchmark method to run
     */
    void run();

    static <T extends SdkAsyncHttpClient.Builder<T>> SdkAsyncHttpClient.Builder<T> httpClient(
        TransferManagerBenchmarkConfig config) {
        if (config.forceCrtHttpClient()) {
            logger.info(() -> "Using CRT HTTP client");
            AwsCrtAsyncHttpClient.Builder builder = AwsCrtAsyncHttpClient.builder();
            if (config.readBufferSizeInMb() != null) {
                builder.readBufferSizeInBytes(config.readBufferSizeInMb() * MB);
            }
            if (config.maxConcurrency() != null) {
                builder.maxConcurrency(config.maxConcurrency());
            }
            return (T) builder;
        }
        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder();
        if (config.connectionAcquisitionTimeoutInSec() != null) {
            Duration connAcqTimeout = Duration.ofSeconds(config.connectionAcquisitionTimeoutInSec());
            builder.connectionAcquisitionTimeout(connAcqTimeout);
        }
        if (config.maxConcurrency() != null) {
            builder.maxConcurrency(config.maxConcurrency());
        }
        return (T) builder;
    }



    static TransferManagerBenchmark v2Download(TransferManagerBenchmarkConfig config) {
        return new TransferManagerDownloadBenchmark(config);
    }

    static TransferManagerBenchmark downloadDirectory(TransferManagerBenchmarkConfig config) {
        return new TransferManagerDownloadDirectoryBenchmark(config);
    }

    static TransferManagerBenchmark v2Upload(TransferManagerBenchmarkConfig config) {
        return new TransferManagerUploadBenchmark(config);
    }

    static TransferManagerBenchmark uploadDirectory(TransferManagerBenchmarkConfig config) {
        return new TransferManagerUploadDirectoryBenchmark(config);
    }

    static TransferManagerBenchmark copy(TransferManagerBenchmarkConfig config) {
        return new TransferManagerCopyBenchmark(config);
    }

    static TransferManagerBenchmark v1Download(TransferManagerBenchmarkConfig config) {
        return new V1TransferManagerDownloadBenchmark(config);
    }

    static TransferManagerBenchmark v1DownloadDirectory(TransferManagerBenchmarkConfig config) {
        return new V1TransferManagerDownloadDirectoryBenchmark(config);
    }

    static TransferManagerBenchmark v1Upload(TransferManagerBenchmarkConfig config) {
        return new V1TransferManagerUploadBenchmark(config);
    }

    static TransferManagerBenchmark v1UploadDirectory(TransferManagerBenchmarkConfig config) {
        return new V1TransferManagerUploadDirectoryBenchmark(config);
    }

    static TransferManagerBenchmark v1Copy(TransferManagerBenchmarkConfig config) {
        return new V1TransferManagerCopyBenchmark(config);
    }

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
