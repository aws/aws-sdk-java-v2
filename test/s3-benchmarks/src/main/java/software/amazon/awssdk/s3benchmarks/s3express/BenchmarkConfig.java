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

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public final class BenchmarkConfig {
    private final String az;
    private final Integer iteration;
    private final Integer contentLengthInKb;
    private final Integer numBuckets;
    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;
    private final Boolean useS3Express;

    private BenchmarkConfig(Builder builder) {
        this.az = builder.az;
        this.iteration = builder.iteration;
        this.contentLengthInKb = builder.contentLengthInKb;
        this.numBuckets = builder.numBuckets;
        this.region = builder.region;
        this.credentialsProvider = builder.credentialsProvider;
        this.useS3Express = builder.useS3Express;
    }

    public String az() {
        return az;
    }

    public Integer iteration() {
        return iteration;
    }

    public Integer contentLengthInKb() {
        return contentLengthInKb;
    }

    public Integer numBuckets() {
        return numBuckets;
    }

    public Region region() {
        return region;
    }

    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    public Boolean useS3Express() {
        return useS3Express;
    }

    public static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private String az;
        private Integer iteration;
        private Integer contentLengthInKb;
        private Integer numBuckets;
        private Region region;
        private AwsCredentialsProvider credentialsProvider;
        private Boolean useS3Express;

        public Builder az(String az) {
            this.az = az;
            return this;
        }

        public Builder iteration(Integer iteration) {
            this.iteration = iteration;
            return this;
        }

        public Builder contentLengthInKb(Integer contentLengthInKb) {
            this.contentLengthInKb = contentLengthInKb;
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

        public Builder useS3Express(Boolean useS3Express) {
            this.useS3Express = useS3Express;
            return this;
        }

        public BenchmarkConfig build() {
            return new BenchmarkConfig(this);
        }
    }
}
