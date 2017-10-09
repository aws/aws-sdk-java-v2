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

package software.amazon.awssdk;

import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

import java.util.Optional;

/**
 * The base class for all SDK requests.
 *
 * @see SdkResponse
 */
public abstract class SdkRequest<B extends SdkRequest.Builder<B, R, RequestOverrideConfigT>,
        R extends SdkRequest<B, R, RequestOverrideConfigT>,
        RequestOverrideConfigT>
        implements ToCopyableBuilder<B, R> {

    private final RequestOverrideConfigT requestOverrideConfig;

    protected SdkRequest(B builder) {
        requestOverrideConfig = builder.requestOverrideConfig();
    }

    public Optional<RequestOverrideConfigT> requestOverrideConfig() {
        return Optional.ofNullable(requestOverrideConfig);
    }

    public interface Builder<B extends SdkRequest.Builder<B, R, RequestOverrideConfigT>,
            R extends SdkRequest<B, R, RequestOverrideConfigT>,
            RequestOverrideConfigT> extends CopyableBuilder<B, R> {

        B requestOverrideConfig(RequestOverrideConfigT sdkRequestOverrideConfig);
        RequestOverrideConfigT requestOverrideConfig();
    }

    protected abstract static class BuilderImpl<B extends Builder<B, R, RequestOverrideConfigT>,
            R extends SdkRequest<B, R, RequestOverrideConfigT>,
            RequestOverrideConfigT> implements Builder<B, R, RequestOverrideConfigT> {
        private final Class<B> concrete;

        private RequestOverrideConfigT requestOverrideConfig;

        protected BuilderImpl(Class<B> concrete) {
            this.concrete = concrete;
        }

        protected BuilderImpl(Class<B> concrete, R request) {
            this(concrete);
            request.requestOverrideConfig().map(c -> requestOverrideConfig = c);
        }

        public RequestOverrideConfigT requestOverrideConfig() {
            return requestOverrideConfig;
        }

        @Override
        public B requestOverrideConfig(RequestOverrideConfigT requestOverrideConfig) {
            this.requestOverrideConfig = requestOverrideConfig;
            return concrete.cast(this);
        }
    }
}
