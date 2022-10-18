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

/**
 * Factory to create the benchmark
 */
@FunctionalInterface
public interface TransferManagerBenchmark {

    /**
     * The benchmark method to run
     */
    void run();

    static TransferManagerBenchmark download(TransferManagerBenchmarkConfig config) {
        return new TransferManagerDownloadBenchmark(config);
    }

    static TransferManagerBenchmark upload(TransferManagerBenchmarkConfig config) {
        return new TransferManagerUploadBenchmark(config);
    }

    static TransferManagerBenchmark v1Download(TransferManagerBenchmarkConfig config) {
        return new V1TransferManagerDownloadBenchmark(config);
    }

    static TransferManagerBenchmark v1Upload(TransferManagerBenchmarkConfig config) {
        return new V1TransferManagerUploadBenchmark(config);
    }

}
