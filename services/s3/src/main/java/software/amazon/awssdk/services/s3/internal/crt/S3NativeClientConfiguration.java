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

package software.amazon.awssdk.services.s3.internal.crt;


import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

/**
 * Internal client configuration resolver
 */
@SdkInternalApi
public class S3NativeClientConfiguration implements SdkAutoCloseable {
    private static final long DEFAULT_PART_SIZE_IN_BYTES = 8L * 1024 * 1024;
    private static final long DEFAULT_TARGET_THROUGHPUT_IN_GBPS = 5;
    private final String signingRegion;
    private final ClientBootstrap clientBootstrap;
    private final CrtCredentialsProviderAdapter credentialProviderAdapter;
    private final CredentialsProvider credentialsProvider;
    private final long partSizeInBytes;
    private final double targetThroughputInGbps;
    private final int maxConcurrency;
    private final URI endpointOverride;
    private final Executor futureCompletionExecutor;
    private final boolean contentMd5;

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

        this.endpointOverride = builder.endpointOverride;

        this.futureCompletionExecutor = resolveAsyncFutureCompletionExecutor(builder.asynConfiguration);

        this.contentMd5 = builder.contentMd5 == null ? false : builder.contentMd5;
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

    public URI endpointOverride() {
        return endpointOverride;
    }

    public Executor futureCompletionExecutor() {
        return futureCompletionExecutor;
    }

    public boolean isContentMd5() {
        return contentMd5;
    }

    /**
     * Finalize which async executor service will be used for the created client. The default async executor
     * service has at least 8 core threads and can scale up to at least 64 threads when needed depending
     * on the number of processors available.
     *
     * This uses the same default executor from SdkDefaultClientBuilder#resolveAsyncFutureCompletionExecutor.
     * Make sure you update that method if you update the defaults here.
     */
    private Executor resolveAsyncFutureCompletionExecutor(ClientAsyncConfiguration config) {
        Supplier<Executor> defaultExecutor = () -> {
            int processors = Runtime.getRuntime().availableProcessors();
            int corePoolSize = Math.max(8, processors);
            int maxPoolSize = Math.max(64, processors * 2);
            ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                                                                 10, TimeUnit.SECONDS,
                                                                 new LinkedBlockingQueue<>(1_000),
                                                                 new ThreadFactoryBuilder()
                                                                     .threadNamePrefix("sdk-async-response").build());
            // Allow idle core threads to time out
            executor.allowCoreThreadTimeOut(true);
            return executor;
        };

        return Optional.ofNullable(config)
                       .map(c -> c.advancedOption(FUTURE_COMPLETION_EXECUTOR))
                       .orElseGet(defaultExecutor);
    }

    @Override
    public void close() {
        clientBootstrap.close();
        credentialProviderAdapter.close();
        shutdownIfExecutorService(futureCompletionExecutor);
    }

    private void shutdownIfExecutorService(Object object) {
        if (object instanceof ExecutorService) {
            ExecutorService executor = (ExecutorService) object;
            executor.shutdown();
        }
    }

    public static final class Builder {
        private String signingRegion;
        private AwsCredentialsProvider credentialsProvider;
        private Long partSizeInBytes;
        private Double targetThroughputInGbps;
        private Integer maxConcurrency;
        private URI endpointOverride;
        private ClientAsyncConfiguration asynConfiguration;
        private Boolean contentMd5;

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

        public Builder endpointOverride(URI endpointOverride) {
            this.endpointOverride = endpointOverride;
            return this;
        }

        public Builder asyncConfiguration(ClientAsyncConfiguration asyncConfiguration) {
            this.asynConfiguration = asyncConfiguration;
            return this;
        }

        public Builder contentMd5(Boolean contentMd5) {
            this.contentMd5 = contentMd5;
            return this;
        }

        public S3NativeClientConfiguration build() {
            return new S3NativeClientConfiguration(this);
        }
    }
}
