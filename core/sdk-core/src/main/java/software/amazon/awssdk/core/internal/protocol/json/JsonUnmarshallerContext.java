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

package software.amazon.awssdk.core.internal.protocol.json;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Dependencies needed by implementations of {@link JsonUnmarshaller}.
 */
@SdkInternalApi
public final class JsonUnmarshallerContext {

    private final SdkHttpFullResponse response;
    private final UnmarshallerRegistry unmarshallerRegistry;

    private JsonUnmarshallerContext(Builder builder) {
        this.response = builder.response;
        this.unmarshallerRegistry = builder.unmarshallerRegistry;
    }

    public SdkHttpFullResponse response() {
        return response;
    }

    public UnmarshallerRegistry unmarshallerRegistry() {
        return unmarshallerRegistry;
    }

    /**
     * @return Builder instance to construct a {@link JsonUnmarshallerContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link JsonUnmarshallerContext}.
     */
    public static final class Builder {

        private SdkHttpFullResponse response;
        private UnmarshallerRegistry unmarshallerRegistry;

        private Builder() {
        }

        public Builder response(SdkHttpFullResponse response) {
            this.response = response;
            return this;
        }

        public Builder unmarshallerRegistry(UnmarshallerRegistry unmarshallerRegistry) {
            this.unmarshallerRegistry = unmarshallerRegistry;
            return this;
        }

        /**
         * @return An immutable {@link JsonUnmarshallerContext} object.
         */
        public JsonUnmarshallerContext build() {
            return new JsonUnmarshallerContext(this);
        }
    }
}
