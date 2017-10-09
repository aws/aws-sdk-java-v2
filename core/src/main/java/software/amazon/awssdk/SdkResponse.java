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

/**
 * The base class for all SDK responses.
 *
 * @see SdkRequest
 */
public abstract class SdkResponse<B extends SdkResponse.Builder<B, R, M>,
        R extends SdkResponse<B, R, M>,
        M> implements ToCopyableBuilder<B, R> {

    private final M responseMetadata;

    protected SdkResponse(B builder) {
        responseMetadata = builder.responseMetadata();
    }

    /**
     * @return The metadata for this response.
     */
    public M responseMetadata() {
        return responseMetadata;
    }

    public interface Builder<B extends SdkResponse.Builder<B, R, M>,
            R extends SdkResponse<B, R, M>,
            M> extends CopyableBuilder<B, R> {

        B responseMetadata(M responseMetadata);
        M responseMetadata();
    }

    protected abstract static class BuilderImpl<B extends SdkResponse.Builder<B, R, M>,
            R extends SdkResponse<B, R, M>,
            M> implements Builder<B, R, M> {
        private final Class<B> concrete;

        private M responseMetadata;

        protected BuilderImpl(Class<B> concrete) {
            this.concrete = concrete;
        }

        protected BuilderImpl(Class<B> concrete,
                              SdkResponse<B, R, M> request) {
            this(concrete);
            this.responseMetadata = request.responseMetadata();
        }

        @Override
        public B responseMetadata(M responseMetadata) {
            this.responseMetadata = responseMetadata;
            return concrete.cast(this);
        }

        @Override
        public M responseMetadata() {
            return responseMetadata;
        }
    }
}