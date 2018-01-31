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

package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Container for extra dependencies needed during execution of a request.
 */
@ReviewBeforeRelease("Should we keep this? It was previously used for metrics, which was removed.")
public class SdkRequestContext {

    private SdkRequestContext(Builder builder) {
    }

    /**
     * @return Builder instance to construct a {@link SdkRequestContext}.
     */
    @SdkInternalApi
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link SdkRequestContext}.
     */
    @SdkInternalApi
    public static final class Builder {

        private Builder() {
        }

        /**
         * @return An immutable {@link SdkRequestContext} object.
         */
        public SdkRequestContext build() {
            return new SdkRequestContext(this);
        }
    }
}
