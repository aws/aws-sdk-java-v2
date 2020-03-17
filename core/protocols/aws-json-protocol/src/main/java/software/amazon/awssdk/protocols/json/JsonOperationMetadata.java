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

package software.amazon.awssdk.protocols.json;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonResponseHandler;

/**
 * Contains various information needed to create a {@link JsonResponseHandler}
 * for the client.
 */
@SdkProtectedApi
public final class JsonOperationMetadata {

    private final boolean hasStreamingSuccessResponse;
    private final boolean isPayloadJson;

    private JsonOperationMetadata(Builder builder) {
        this.hasStreamingSuccessResponse = builder.hasStreamingSuccessResponse;
        this.isPayloadJson = builder.isPayloadJson;
    }

    public boolean hasStreamingSuccessResponse() {
        return hasStreamingSuccessResponse;
    }

    public boolean isPayloadJson() {
        return isPayloadJson;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link JsonOperationMetadata}.
     */
    public static final class Builder {

        private boolean hasStreamingSuccessResponse;
        private boolean isPayloadJson;

        private Builder() {
        }

        /**
         * True is payload contains JSON content, false if it doesn't (i.e. it contains binary content or no content).
         *
         * @return This builder for method chaining.
         */
        public Builder isPayloadJson(boolean payloadJson) {
            isPayloadJson = payloadJson;
            return this;
        }


        /**
         * True if the success response (2xx response) contains a payload that should be treated as streaming. False otherwise.
         *
         * @return This builder for method chaining.
         */
        public Builder hasStreamingSuccessResponse(boolean hasStreamingSuccessResponse) {
            this.hasStreamingSuccessResponse = hasStreamingSuccessResponse;
            return this;
        }

        public JsonOperationMetadata build() {
            return new JsonOperationMetadata(this);
        }
    }
}
