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

package software.amazon.awssdk.core.internal.http;

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder;
import software.amazon.awssdk.core.internal.retry.ClockSkewAdjuster;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Client scoped dependencies of {@link AmazonSyncHttpClient} and {@link AmazonAsyncHttpClient}.
 * May be injected into constructors of {@link RequestPipeline} implementations by {@link RequestPipelineBuilder}.
 */
@SdkInternalApi
public final class HttpClientDependencies implements SdkAutoCloseable {
    private final ClockSkewAdjuster clockSkewAdjuster;
    private final SdkClientConfiguration clientConfiguration;

    /**
     * Time offset may be mutated by {@link RequestPipeline} implementations if a clock skew is detected.
     */
    private volatile int timeOffset = SdkGlobalTime.getGlobalTimeOffset();

    private HttpClientDependencies(Builder builder) {
        this.clockSkewAdjuster = builder.clockSkewAdjuster != null ? builder.clockSkewAdjuster : new ClockSkewAdjuster();
        this.clientConfiguration = paramNotNull(builder.clientConfiguration, "ClientConfiguration");
    }

    public static Builder builder() {
        return new Builder();
    }

    public SdkClientConfiguration clientConfiguration() {
        return clientConfiguration;
    }

    /**
     * @return The adjuster used for adjusting the {@link #timeOffset} for this client.
     */
    public ClockSkewAdjuster clockSkewAdjuster() {
        return clockSkewAdjuster;
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
        SdkGlobalTime.setGlobalTimeOffset(timeOffset);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public void close() {
        this.clientConfiguration.close();
    }

    /**
     * Builder for {@link HttpClientDependencies}.
     */
    public static class Builder {
        private ClockSkewAdjuster clockSkewAdjuster;
        private SdkClientConfiguration clientConfiguration;

        private Builder() {
        }

        private Builder(HttpClientDependencies from) {
            this.clientConfiguration = from.clientConfiguration;
            this.clockSkewAdjuster = from.clockSkewAdjuster;
        }

        public Builder clockSkewAdjuster(ClockSkewAdjuster clockSkewAdjuster) {
            this.clockSkewAdjuster = clockSkewAdjuster;
            return this;
        }

        public Builder clientConfiguration(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        public Builder clientConfiguration(Consumer<SdkClientConfiguration.Builder> clientConfiguration) {
            SdkClientConfiguration.Builder c = SdkClientConfiguration.builder();
            clientConfiguration.accept(c);
            clientConfiguration(c.build());
            return this;
        }

        public HttpClientDependencies build() {
            return new HttpClientDependencies(this);
        }
    }
}
