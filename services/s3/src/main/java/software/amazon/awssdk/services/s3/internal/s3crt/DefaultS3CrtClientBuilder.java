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

import java.net.URI;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

@SdkInternalApi
public final class DefaultS3CrtClientBuilder implements S3CrtAsyncClientBuilder {
    private AwsCredentialsProvider credentialsProvider;
    private Region region;
    private long partSizeBytes;
    private double maxThroughputGbps;

    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    public Region region() {
        return region;
    }

    public long partSizeBytes() {
        return partSizeBytes;
    }

    public double maxThroughputGbps() {
        return maxThroughputGbps;
    }

    @Override
    public S3CrtAsyncClientBuilder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }


    public S3CrtAsyncClientBuilder region(Region region) {
        this.region = region;
        return this;
    }

    // TODO: Add support for this configuration
    @Override
    public S3CrtAsyncClientBuilder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
        throw new UnsupportedOperationException();
    }

    // TODO: Add support for this configuration
    @Override
    public S3CrtAsyncClientBuilder overrideConfiguration(Consumer<ClientOverrideConfiguration.Builder> overrideConfiguration) {
        throw new UnsupportedOperationException();
    }

    // TODO: Add support for this configuration
    @Override
    public S3CrtAsyncClientBuilder endpointOverride(URI endpointOverride) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S3CrtAsyncClientBuilder partSizeBytes(long partSizeBytes) {
        this.partSizeBytes = partSizeBytes;
        return this;
    }

    @Override
    public S3CrtAsyncClientBuilder maxThroughputGbps(double maxThroughputGbps) {
        this.maxThroughputGbps = maxThroughputGbps;
        return this;
    }

    @Override
    public S3CrtAsyncClient build() {
        return new DefaultS3CrtAsyncClient(this);
    }
}
