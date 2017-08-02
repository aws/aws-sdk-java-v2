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

import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.util.CapacityManager;

/**
 * Client scoped dependencies of {@link AmazonHttpClient}. May be injected into constructors of {@link
 * software.amazon.awssdk.http.pipeline.RequestPipeline} implementations by
 * {@link software.amazon.awssdk.http.pipeline.RequestPipelineBuilder}.
 */
public abstract class HttpClientDependencies implements AutoCloseable {
    private final ClientConfiguration clientConfiguration;
    private final CapacityManager capacityManager;
    private final ClientExecutionTimer clientExecutionTimer;

    /**
     * Time offset may be mutated by {@link software.amazon.awssdk.http.pipeline.RequestPipeline} implementations
     * if a clock skew is detected.
     */
    private volatile int timeOffset = SdkGlobalTime.getGlobalTimeOffset();

    protected HttpClientDependencies(ClientConfiguration clientConfiguration, Builder<?> builder) {
        this.clientConfiguration = paramNotNull(clientConfiguration, "ClientConfiguration");
        this.capacityManager = paramNotNull(builder.capacityManager, "CapacityManager");
        this.clientExecutionTimer = paramNotNull(builder.clientExecutionTimer, "ClientExecutionTimer");
    }

    public ClientConfiguration clientConfiguration() {
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
    public void close() throws Exception {
        this.clientExecutionTimer.close();
    }

    /**
     * Builder for {@link HttpClientDependencies}.
     */
    public static class Builder<T extends Builder<T>> {
        private CapacityManager capacityManager;
        private ClientExecutionTimer clientExecutionTimer;

        public T capacityManager(CapacityManager capacityManager) {
            this.capacityManager = capacityManager;
            return thisBuilder();
        }

        public T clientExecutionTimer(ClientExecutionTimer clientExecutionTimer) {
            this.clientExecutionTimer = clientExecutionTimer;
            return thisBuilder();
        }

        @SuppressWarnings("unchecked")
        private T thisBuilder() {
            return (T) this;
        }
    }
}
