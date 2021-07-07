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

public class TransferManagerBenchmarkConfig {
    private final String filePath;
    private final String bucket;
    private final String key;
    private final Double targetThroughput;
    private final Long partSizeInMb;
    private final Integer warmupConcurrency;

    private TransferManagerBenchmarkConfig(Builder builder) {
        this.filePath = builder.filePath;
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.targetThroughput = builder.targetThroughput;
        this.partSizeInMb = builder.partSizeInMb;
        this.warmupConcurrency = builder.warmupConcurrency;
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

    public Integer warmupConcurrency() {
        return warmupConcurrency;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "{" +
               "filePath: '" + filePath + '\'' +
               ", bucket: '" + bucket + '\'' +
               ", key: '" + key + '\'' +
               ", targetThroughput: " + targetThroughput +
               ", partSizeInMb: " + partSizeInMb +
               ", warmupConcurrency: " + warmupConcurrency +
               '}';
    }

    static final class Builder {
        private String filePath;
        private String bucket;
        private String key;
        private Double targetThroughput;
        private Long partSizeInMb;
        private Integer warmupConcurrency;

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

        public Builder warmupConcurrency(Integer warmupConcurrency) {
            this.warmupConcurrency = warmupConcurrency;
            return this;
        }

        public TransferManagerBenchmarkConfig build() {
            return new TransferManagerBenchmarkConfig(this);
        }
    }
}
