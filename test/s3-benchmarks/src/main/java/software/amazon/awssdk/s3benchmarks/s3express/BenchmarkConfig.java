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

import java.time.Duration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.utils.ToString;

public final class BenchmarkConfig {
    private final String filePath;
    private final String az;
    private final String key;
    private final Double targetThroughput;
    private final Long partSizeInMb;
    private final ChecksumAlgorithm checksumAlgorithm;
    private final Integer iteration;
    private final Long contentLengthInMb;

    private final Integer contentLengthInKb;
    private final Duration timeout;
    private final Long memoryUsageInMb;
    private final Long connectionAcquisitionTimeoutInSec;
    private final Boolean forceCrtHttpClient;
    private final Integer maxConcurrency;

    private final Long readBufferSizeInMb;
    private final Integer numBuckets;
    private final String prefix;

    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;

    private final Boolean useS3Express;

    private BenchmarkConfig(Builder builder) {
        this.filePath = builder.filePath;
        this.az = builder.az;
        this.key = builder.key;
        this.targetThroughput = builder.targetThroughput;
        this.partSizeInMb = builder.partSizeInMb;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.iteration = builder.iteration;
        this.readBufferSizeInMb = builder.readBufferSizeInMb;
        this.prefix = builder.prefix;
        this.contentLengthInMb = builder.contentLengthInMb;
        this.timeout = builder.timeout;
        this.memoryUsageInMb = builder.memoryUsage;
        this.connectionAcquisitionTimeoutInSec = builder.connectionAcquisitionTimeoutInSec;
        this.forceCrtHttpClient = builder.forceCrtHttpClient;
        this.maxConcurrency = builder.maxConcurrency;
        this.numBuckets = builder.numBuckets;
        this.contentLengthInKb = builder.contentLengthInKb;
        this.region = builder.region;
        this.credentialsProvider = builder.credentialsProvider;
        this.useS3Express = builder.useS3Express;
    }

    public String filePath() {
        return filePath;
    }

    public String az() {
        return az;
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

    public String prefix() {
        return prefix;
    }

    public Long contentLengthInMb() {
        return contentLengthInMb;
    }

    public Integer contentLengthInKb() {
        return contentLengthInKb;
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

    public Boolean useS3Express() {
        return this.useS3Express;
    }

    public Integer maxConcurrency() {
        return this.maxConcurrency;
    }

    public Integer numBuckets() {
        return this.numBuckets;
    }

    public Region region() {
        return region;
    }

    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return ToString.builder("BenchmarkConfig")
                       .add("filePath", filePath)
                       .add("bucket", az)
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
                       .add("prefix", prefix)
                       .build();
    }

    static final class Builder {
        private Long readBufferSizeInMb;
        private ChecksumAlgorithm checksumAlgorithm;
        private String filePath;
        private String az;
        private String key;
        private Double targetThroughput;
        private Long partSizeInMb;
        private Long contentLengthInMb;
        private Integer contentLengthInKb;
        private Long memoryUsage;
        private Long connectionAcquisitionTimeoutInSec;
        private Boolean forceCrtHttpClient;
        private Integer maxConcurrency;
        private Integer iteration;
        private Integer numBuckets;

        private Region region;
        private AwsCredentialsProvider credentialsProvider;

        private String prefix;

        private Duration timeout;

        private Boolean useS3Express;

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder az(String az) {
            this.az = az;
            return this;
        }

        public Builder useS3Express(Boolean useS3Express) {
            this.useS3Express = useS3Express;
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

        public Builder contentLengthInKb(Integer contentLengthInKb) {
            this.contentLengthInKb = contentLengthInKb;
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

        public Builder numBuckets(Integer numBuckets) {
            this.numBuckets = numBuckets;
            return this;
        }

        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public BenchmarkConfig build() {
            return new BenchmarkConfig(this);
        }

    }
}
