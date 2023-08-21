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

import java.time.Duration;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.utils.ToString;

public final class TransferManagerBenchmarkConfig {
    private final String filePath;
    private final String bucket;
    private final String key;
    private final Double targetThroughput;
    private final Long partSizeInMb;
    private final ChecksumAlgorithm checksumAlgorithm;
    private final Integer iteration;
    private final Long contentLengthInMb;
    private final Duration timeout;
    private final Long memoryUsageInMb;
    private final Long connectionAcquisitionTimeoutInSec;
    private final Boolean forceCrtHttpClient;
    private final Integer maxConcurrency;

    private final Long readBufferSizeInMb;
    private final BenchmarkRunner.TransferManagerOperation operation;
    private String prefix;

    private TransferManagerBenchmarkConfig(Builder builder) {
        this.filePath = builder.filePath;
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.targetThroughput = builder.targetThroughput;
        this.partSizeInMb = builder.partSizeInMb;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.iteration = builder.iteration;
        this.readBufferSizeInMb = builder.readBufferSizeInMb;
        this.operation = builder.operation;
        this.prefix = builder.prefix;
        this.contentLengthInMb = builder.contentLengthInMb;
        this.timeout = builder.timeout;
        this.memoryUsageInMb = builder.memoryUsage;
        this.connectionAcquisitionTimeoutInSec = builder.connectionAcquisitionTimeoutInSec;
        this.forceCrtHttpClient = builder.forceCrtHttpClient;
        this.maxConcurrency = builder.maxConcurrency;
    }

    public String filePath() {
        return filePath;
    }

    public String bucket() {
        return bucket;
    }

    public String key() {
        return key;
    }

    public Double targetThroughput() {
        return targetThroughput;
    }

    public Long partSizeInMb() {
        return partSizeInMb;
    }

    public ChecksumAlgorithm checksumAlgorithm() {
        return checksumAlgorithm;
    }

    public Integer iteration() {
        return iteration;
    }

    public Long readBufferSizeInMb() {
        return readBufferSizeInMb;
    }

    public BenchmarkRunner.TransferManagerOperation operation() {
        return operation;
    }

    public String prefix() {
        return prefix;
    }

    public Long contentLengthInMb() {
        return contentLengthInMb;
    }

    public Duration timeout() {
        return this.timeout;
    }

    public Long memoryUsageInMb() {
        return this.memoryUsageInMb;
    }

    public Long connectionAcquisitionTimeoutInSec() {
        return this.connectionAcquisitionTimeoutInSec;
    }

    public boolean forceCrtHttpClient() {
        return this.forceCrtHttpClient;
    }

    public Integer maxConcurrency() {
        return this.maxConcurrency;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return ToString.builder("TransferManagerBenchmarkConfig")
                       .add("filePath", filePath)
                       .add("bucket", bucket)
                       .add("key", key)
                       .add("targetThroughput", targetThroughput)
                       .add("partSizeInMb", partSizeInMb)
                       .add("checksumAlgorithm", checksumAlgorithm)
                       .add("iteration", iteration)
                       .add("contentLengthInMb", contentLengthInMb)
                       .add("timeout", timeout)
                       .add("memoryUsageInMb", memoryUsageInMb)
                       .add("connectionAcquisitionTimeoutInSec", connectionAcquisitionTimeoutInSec)
                       .add("forceCrtHttpClient", forceCrtHttpClient)
                       .add("maxConcurrency", maxConcurrency)
                       .add("readBufferSizeInMb", readBufferSizeInMb)
                       .add("operation", operation)
                       .add("prefix", prefix)
                       .build();
    }

    static final class Builder {
        private Long readBufferSizeInMb;
        private ChecksumAlgorithm checksumAlgorithm;
        private String filePath;
        private String bucket;
        private String key;
        private Double targetThroughput;
        private Long partSizeInMb;
        private Long contentLengthInMb;
        private Long memoryUsage;
        private Long connectionAcquisitionTimeoutInSec;
        private Boolean forceCrtHttpClient;
        private Integer maxConcurrency;

        private Integer iteration;
        private BenchmarkRunner.TransferManagerOperation operation;
        private String prefix;

        private Duration timeout;

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder targetThroughput(Double targetThroughput) {
            this.targetThroughput = targetThroughput;
            return this;
        }

        public Builder partSizeInMb(Long partSizeInMb) {
            this.partSizeInMb = partSizeInMb;
            return this;
        }

        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        public Builder iteration(Integer iteration) {
            this.iteration = iteration;
            return this;
        }

        public Builder operation(BenchmarkRunner.TransferManagerOperation operation) {
            this.operation = operation;
            return this;
        }

        public Builder readBufferSizeInMb(Long readBufferSizeInMb) {
            this.readBufferSizeInMb = readBufferSizeInMb;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder contentLengthInMb(Long contentLengthInMb) {
            this.contentLengthInMb = contentLengthInMb;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder memoryUsageInMb(Long memoryUsage) {
            this.memoryUsage = memoryUsage;
            return this;
        }

        public Builder connectionAcquisitionTimeoutInSec(Long connectionAcquisitionTimeoutInSec) {
            this.connectionAcquisitionTimeoutInSec = connectionAcquisitionTimeoutInSec;
            return this;
        }

        public Builder forceCrtHttpClient(Boolean forceCrtHttpClient) {
            this.forceCrtHttpClient = forceCrtHttpClient;
            return this;
        }

        public Builder maxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        public TransferManagerBenchmarkConfig build() {
            return new TransferManagerBenchmarkConfig(this);
        }

    }
}
