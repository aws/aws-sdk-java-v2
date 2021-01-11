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

package software.amazon.awssdk.core.http;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * <b>Warning:</b> This class should only be accessed by a single thread and be used throughout a single request lifecycle.
 */
@NotThreadSafe
@SdkProtectedApi
public final class ExecutionContext implements ToCopyableBuilder<ExecutionContext.Builder, ExecutionContext> {
    private final Signer signer;
    private InterceptorContext interceptorContext;
    private final ExecutionInterceptorChain interceptorChain;
    private final ExecutionAttributes executionAttributes;
    private final MetricCollector metricCollector;

    private ExecutionContext(final Builder builder) {
        this.signer = builder.signer;
        this.interceptorContext = builder.interceptorContext;
        this.interceptorChain = builder.interceptorChain;
        this.executionAttributes = builder.executionAttributes;
        this.metricCollector = builder.metricCollector;
    }

    public static ExecutionContext.Builder builder() {
        return new ExecutionContext.Builder();
    }

    public InterceptorContext interceptorContext() {
        return interceptorContext;
    }

    //TODO: We should switch to fully immutable execution contexts. Currently, we mutate it for the interceptor
    //context, credential providers, etc
    public ExecutionContext interceptorContext(InterceptorContext interceptorContext) {
        this.interceptorContext = interceptorContext;
        return this;
    }

    public ExecutionInterceptorChain interceptorChain() {
        return interceptorChain;
    }

    public ExecutionAttributes executionAttributes() {
        return executionAttributes;
    }

    public Signer signer() {
        return signer;
    }

    public MetricCollector metricCollector() {
        return metricCollector;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder implements CopyableBuilder<Builder, ExecutionContext> {
        private InterceptorContext interceptorContext;
        private ExecutionInterceptorChain interceptorChain;
        private ExecutionAttributes executionAttributes;
        private Signer signer;
        private MetricCollector metricCollector;

        private Builder() {
        }

        public Builder(ExecutionContext executionContext) {
            this.signer = executionContext.signer;
            this.interceptorContext = executionContext.interceptorContext;
            this.interceptorChain = executionContext.interceptorChain;
            this.executionAttributes = executionContext.executionAttributes;
        }

        public Builder interceptorContext(InterceptorContext interceptorContext) {
            this.interceptorContext = interceptorContext;
            return this;
        }

        public Builder interceptorChain(ExecutionInterceptorChain interceptorChain) {
            this.interceptorChain = interceptorChain;
            return this;
        }

        public Builder executionAttributes(ExecutionAttributes executionAttributes) {
            this.executionAttributes = executionAttributes;
            return this;
        }

        public Builder signer(Signer signer) {
            this.signer = signer;
            return this;
        }

        public Builder metricCollector(MetricCollector metricCollector) {
            this.metricCollector = metricCollector;
            return this;
        }

        public ExecutionContext build() {
            return new ExecutionContext(this);
        }

    }

}
