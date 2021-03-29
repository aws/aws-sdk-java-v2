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
    private final Double maxThroughput;
    private final Long partSizeInMb;

    private TransferManagerBenchmarkConfig(Builder builder) {
        this.filePath = builder.filePath;
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.maxThroughput = builder.maxThroughput;
        this.partSizeInMb = builder.partSizeInMb;
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

    public Double maxThroughput() {
        return maxThroughput;
    }

    public Long partSizeInMb() {
        return partSizeInMb;
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
               ", maxThroughput: " + maxThroughput +
               ", partSizeInMB: " + partSizeInMb +
               '}';
    }

    static final class Builder {
        private String filePath;
        private String bucket;
        private String key;
        private Double maxThroughput;
        private Long partSizeInMb;

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

        public Builder maxThroughput(Double maxThroughput) {
            this.maxThroughput = maxThroughput;
            return this;
        }

        public Builder partSizeInMb(Long partSizeInMb) {
            this.partSizeInMb = partSizeInMb;
            return this;
        }

        public TransferManagerBenchmarkConfig build() {
            return new TransferManagerBenchmarkConfig(this);
        }
    }
}
