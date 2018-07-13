/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.http;

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder;
import software.amazon.awssdk.core.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.core.internal.util.CapacityManager;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Client scoped dependencies of {@link AmazonSyncHttpClient}. May be injected into constructors of {@link
 * RequestPipeline} implementations by {@link RequestPipelineBuilder}.
 */
@SdkInternalApi
public final class HttpClientDependencies implements SdkAutoCloseable {
    private final SdkClientConfiguration clientConfiguration;
    private final CapacityManager capacityManager;
    private final ClientExecutionTimer clientExecutionTimer;

    /**
     * Time offset may be mutated by {@link RequestPipeline} implementations if a clock skew is detected.
     */
    private volatile int timeOffset = SdkGlobalTime.getGlobalTimeOffset();

    private HttpClientDependencies(Builder builder) {
        this.clientConfiguration = paramNotNull(builder.clientConfiguration, "ClientConfiguration");
        this.capacityManager = paramNotNull(builder.capacityManager, "CapacityManager");
        this.clientExecutionTimer = paramNotNull(builder.clientExecutionTimer, "ClientExecutionTimer");
    }

    public static Builder builder() {
        return new Builder();
    }

    public SdkClientConfiguration clientConfiguration() {
        return clientConfiguration;
    }

    /**
     * @return CapacityManager object used for retry throttling.
     */
    public CapacityManager retryCapacity() {
        return capacityManager;
    }

    /**
     * @return Controller for the ClientExecution timeout feature.
     */
    public ClientExecutionTimer clientExecutionTimer() {
        return clientExecutionTimer;
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
    public void close() {
        this.clientConfiguration.close();
        this.clientExecutionTimer.close();
    }

    /**
     * Builder for {@link HttpClientDependencies}.
     */
    public static class Builder {
        private SdkClientConfiguration clientConfiguration;
        private CapacityManager capacityManager;
        private ClientExecutionTimer clientExecutionTimer;

        private Builder() {}

        public Builder clientConfiguration(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        public Builder capacityManager(CapacityManager capacityManager) {
            this.capacityManager = capacityManager;
            return this;
        }

        public Builder clientExecutionTimer(ClientExecutionTimer clientExecutionTimer) {
            this.clientExecutionTimer = clientExecutionTimer;
            return this;
        }

        public HttpClientDependencies build() {
            return new HttpClientDependencies(this);
        }
    }
}
