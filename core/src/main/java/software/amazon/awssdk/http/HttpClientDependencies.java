/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.retry.v2.RetryPolicy;
import software.amazon.awssdk.util.CapacityManager;

/**
 * Client scoped dependencies of {@link AmazonHttpClient}. May be injected into constructors of {@link
 * software.amazon.awssdk.http.pipeline.RequestPipeline} implementations by
 * {@link software.amazon.awssdk.http.pipeline.RequestPipelineBuilder}.
 */
public class HttpClientDependencies implements AutoCloseable {

    private final LegacyClientConfiguration config;
    private final RetryPolicy retryPolicy;
    private final CapacityManager retryCapacity;
    private final SdkHttpClient sdkHttpClient;
    // Do we want seperate dependencies for sync/async or just have an Either or something
    private final SdkAsyncHttpClient sdkAsyncHttpClient;
    private final ClientExecutionTimer clientExecutionTimer;
    private final ScheduledExecutorService executorService;
    private final boolean calculateCrc32FromCompressedData;

    /**
     * Time offset may be mutated by {@link software.amazon.awssdk.http.pipeline.RequestPipeline} implementations
     * if a clock skew is detected.
     */
    private volatile int timeOffset = SdkGlobalTime.getGlobalTimeOffset();

    private HttpClientDependencies(Builder builder) {
        this.config = paramNotNull(builder.config, "Configuration");
        this.retryPolicy = paramNotNull(builder.retryPolicy, "RetryPolicy");
        this.retryCapacity = paramNotNull(builder.retryCapacity, "CapacityManager");
        // TODO validate not null
        this.sdkHttpClient = builder.sdkHttpClient;
        this.sdkAsyncHttpClient = builder.sdkAsyncHttpClient;
        this.clientExecutionTimer = paramNotNull(builder.clientExecutionTimer, "ClientExecutionTimer");
        this.executorService = builder.executorService;
        this.calculateCrc32FromCompressedData = builder.calculateCrc32FromCompressedData;
    }

    /**
     * Create a {@link Builder}, used to create a {@link RequestExecutionContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return {@link LegacyClientConfiguration} object provided by generated client.
     */
    public LegacyClientConfiguration config() {
        return config;
    }

    /**
     * @return The {@link RetryPolicy} configured for the client.
     */
    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    /**
     * @return CapacityManager object used for retry throttling.
     */
    public CapacityManager retryCapacity() {
        return retryCapacity;
    }

    /**
     * @return SdkHttpClient implementation to make an HTTP request.
     */
    public SdkHttpClient sdkHttpClient() {
        return sdkHttpClient;
    }

    /**
     * @return SdkAsyncHttpClient implementation to make an HTTP request.
     */
    public SdkAsyncHttpClient sdkAsyncHttpClient() {
        return sdkAsyncHttpClient;
    }

    /**
     * @return Controller for the ClientExecution timeout feature.
     */
    public ClientExecutionTimer clientExecutionTimer() {
        return clientExecutionTimer;
    }

    public ScheduledExecutorService executorService() {
        return executorService;
    }

    /**
     * @return True if the SDK should calculate the CRC32 checksum from the compressed HTTP content, false if it
     * should calculate it from the uncompressed content. Currently, only DynamoDB sets this flag to true.
     */
    public boolean calculateCrc32FromCompressedData() {
        return calculateCrc32FromCompressedData;
    }

    /**
     * @return Current time offset. This is mutable and should not be cached.
     */
    public int timeOffset() {
        return timeOffset;
    }

    /**
     * Updates the time offset of the client as well as the global time offset.
     */
    public void updateTimeOffset(int timeOffset) {
        this.timeOffset = timeOffset;
        // TODO think about why we update global. I assume because it's more likely to have the client's clock skewed.
        SdkGlobalTime.setGlobalTimeOffset(timeOffset);
    }

    @Override
    public void close() throws Exception {
        this.clientExecutionTimer.close();
        // TODO close which one is present
        if (this.sdkAsyncHttpClient != null) {
            this.sdkAsyncHttpClient.close();
        }
        if (this.sdkHttpClient != null) {
            this.sdkHttpClient.close();
        }
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
    }

    /**
     * Builder for {@link HttpClientDependencies}.
     */
    public static final class Builder {

        private LegacyClientConfiguration config;
        private RetryPolicy retryPolicy;
        private CapacityManager retryCapacity;
        private SdkHttpClient sdkHttpClient;
        private SdkAsyncHttpClient sdkAsyncHttpClient;
        private ClientExecutionTimer clientExecutionTimer;
        private ScheduledExecutorService executorService;
        private boolean calculateCrc32FromCompressedData;

        public Builder config(LegacyClientConfiguration config) {
            this.config = config;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder retryCapacity(CapacityManager retryCapacity) {
            this.retryCapacity = retryCapacity;
            return this;
        }

        public Builder sdkHttpClient(SdkHttpClient sdkHttpClient) {
            this.sdkHttpClient = sdkHttpClient;
            return this;
        }

        public Builder sdkAsyncHttpClient(SdkAsyncHttpClient sdkAsyncHttpClient) {
            this.sdkAsyncHttpClient = sdkAsyncHttpClient;
            return this;
        }

        public Builder clientExecutionTimer(ClientExecutionTimer clientExecutionTimer) {
            this.clientExecutionTimer = clientExecutionTimer;
            return this;
        }

        public Builder asyncExecutorService(ScheduledExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder calculateCrc32FromCompressedData(
                boolean calculateCrc32FromCompressedData) {
            this.calculateCrc32FromCompressedData = calculateCrc32FromCompressedData;
            return this;
        }

        public HttpClientDependencies build() {
            return new HttpClientDependencies(this);
        }
    }
}
