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

package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Container for extra dependencies needed during execution of a request.
 */
@SdkProtectedApi
public class SdkRequestContext {
    private final boolean isFullDuplex;

    private SdkRequestContext(Builder builder) {
        this.isFullDuplex = builder.isFullDuplex;
    }

    /**
     * @return Builder instance to construct a {@link SdkRequestContext}.
     */
    @SdkInternalApi
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Option to indicate if the request is for a full duplex operation ie., request and response are sent/received at the same
     * time.
     * This can be used to set http configuration like ReadTimeouts as soon as request has begin sending data instead of
     * waiting for the entire request to be sent.
     *
     * @return True if the operation this request belongs to is full duplex. Otherwise false.
     */
    public boolean fullDuplex() {
        return isFullDuplex;
    }

    /**
     * Builder for a {@link SdkRequestContext}.
     */
    @SdkInternalApi
    public static final class Builder {
        private boolean isFullDuplex;

        private Builder() {
        }

        public boolean fullDuplex() {
            return isFullDuplex;
        }

        public Builder fullDuplex(boolean fullDuplex) {
            isFullDuplex = fullDuplex;
            return this;
        }

        /**
         * @return An immutable {@link SdkRequestContext} object.
         */
        public SdkRequestContext build() {
            return new SdkRequestContext(this);
        }
    }
}
