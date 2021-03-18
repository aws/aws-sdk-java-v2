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

package software.amazon.awssdk.services.s3.internal.s3crt;


import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.crt.auth.credentials.DefaultChainCredentialsProvider;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@SdkInternalApi
public final class S3NativeClientConfiguration implements SdkAutoCloseable {
    // TODO: update those defaults.
    private static final long DEFAULT_PART_SIZE_BYTES = 5 * 1024 * 1024L;
    private static final long DEFAULT_MAX_THROUGPUT_GBPS = 100;
    private final String signingRegion;
    private final ClientBootstrap clientBootstrap;
    private final CredentialsProvider credentialsProvider;
    private final long partSizeBytes;
    private final double maxThroughputGbps;

    public S3NativeClientConfiguration(Builder builder) {
        this.signingRegion = builder.signingRegion == null ? DefaultAwsRegionProviderChain.builder().build().getRegion().id() :
                             builder.signingRegion;
        this.clientBootstrap = builder.clientBootstrap == null ? new ClientBootstrap(null, null) : builder.clientBootstrap;
        this.credentialsProvider = builder.credentialsProvider == null ?
                                   new DefaultChainCredentialsProvider.DefaultChainCredentialsProviderBuilder()
                                       .withClientBootstrap(clientBootstrap)
                                       .build() :
                                   builder.credentialsProvider;
        this.partSizeBytes = builder.partSizeBytes == null ? DEFAULT_PART_SIZE_BYTES : builder.partSizeBytes;
        this.maxThroughputGbps = builder.maxThroughputGbps == null ? DEFAULT_MAX_THROUGPUT_GBPS : builder.maxThroughputGbps;
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
        return partSizeBytes;
    }

    public double maxThroughputGbps() {
        return maxThroughputGbps;
    }

    @Override
    public void close() {
        clientBootstrap.close();
        credentialsProvider.close();
    }

    public static final class Builder {
        private String signingRegion;
        private ClientBootstrap clientBootstrap;
        private CredentialsProvider credentialsProvider;
        private Long partSizeBytes;
        private Double maxThroughputGbps;

        private Builder() {
        }

        public Builder signingRegion(String signingRegion) {
            this.signingRegion = signingRegion;
            return this;
        }

        public Builder clientBootstrap(ClientBootstrap clientBootstrap) {
            this.clientBootstrap = clientBootstrap;
            return this;
        }

        public Builder credentialsProvider(CredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public Builder partSizeBytes(long partSizeBytes) {
            this.partSizeBytes = partSizeBytes;
            return this;
        }

        public Builder maxThroughputGbps(double maxThroughputGbps) {
            this.maxThroughputGbps = maxThroughputGbps;
            return this;
        }

        public S3NativeClientConfiguration build() {
            return new S3NativeClientConfiguration(this);
        }
    }
}
