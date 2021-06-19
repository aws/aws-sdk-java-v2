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

package software.amazon.awssdk.transfer.s3.internal;


import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.transfer.s3.SizeConstant;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Internal client configuration resolver
 */
@SdkInternalApi
public final class S3NativeClientConfiguration implements SdkAutoCloseable {
    private static final long DEFAULT_PART_SIZE_IN_BYTES = 8L * SizeConstant.MB;
    private static final long DEFAULT_TARGET_THROUGHPUT_IN_GBPS = 5;
    private final String signingRegion;
    private final ClientBootstrap clientBootstrap;
    private final CrtCredentialsProviderAdapter credentialProviderAdapter;
    private final CredentialsProvider credentialsProvider;
    private final long partSizeInBytes;
    private final double targetThroughputInGbps;
    private final int maxConcurrency;

    public S3NativeClientConfiguration(Builder builder) {
        this.signingRegion = builder.signingRegion == null ? DefaultAwsRegionProviderChain.builder().build().getRegion().id() :
                             builder.signingRegion;
        this.clientBootstrap = new ClientBootstrap(null, null);
        this.credentialProviderAdapter =
            builder.credentialsProvider == null ?
            new CrtCredentialsProviderAdapter(DefaultCredentialsProvider.create()) :
            new CrtCredentialsProviderAdapter(builder.credentialsProvider);
        this.credentialsProvider = credentialProviderAdapter.crtCredentials();

        this.partSizeInBytes = builder.partSizeInBytes == null ? DEFAULT_PART_SIZE_IN_BYTES :
                               builder.partSizeInBytes;
        this.targetThroughputInGbps = builder.targetThroughputInGbps == null ?
                                      DEFAULT_TARGET_THROUGHPUT_IN_GBPS : builder.targetThroughputInGbps;

        // Using 0 so that CRT will calculate it based on targetThroughputGbps
        this.maxConcurrency = builder.maxConcurrency == null ? 0 : builder.maxConcurrency;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String signingRegion() {
        return signingRegion;
    }

    public ClientBootstrap clientBootstrap() {
        return clientBootstrap;
    }

    public CredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    public long partSizeBytes() {
        return partSizeInBytes;
    }

    public double targetThroughputInGbps() {
        return targetThroughputInGbps;
    }

    public int maxConcurrency() {
        return maxConcurrency;
    }

    @Override
    public void close() {
        clientBootstrap.close();
        credentialProviderAdapter.close();
        credentialsProvider.close();
    }

    public static final class Builder {
        private String signingRegion;
        private AwsCredentialsProvider credentialsProvider;
        private Long partSizeInBytes;
        private Double targetThroughputInGbps;
        private Integer maxConcurrency;

        private Builder() {
        }

        public Builder signingRegion(String signingRegion) {
            this.signingRegion = signingRegion;
            return this;
        }

        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public Builder partSizeInBytes(Long partSizeInBytes) {
            this.partSizeInBytes = partSizeInBytes;
            return this;
        }

        public Builder targetThroughputInGbps(Double targetThroughputInGbps) {
            this.targetThroughputInGbps = targetThroughputInGbps;
            return this;
        }

        public Builder maxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        public S3NativeClientConfiguration build() {
            return new S3NativeClientConfiguration(this);
        }
    }
}
