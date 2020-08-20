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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.internal.MarshallerUtil;

/**
 * Dependencies needed by implementations of {@link JsonUnmarshaller}.
 */
@SdkInternalApi
public final class JsonUnmarshallerContext {

    private final SdkHttpFullResponse response;
    private final JsonUnmarshallerRegistry unmarshallerRegistry;

    private JsonUnmarshallerContext(Builder builder) {
        this.response = builder.response;
        this.unmarshallerRegistry = builder.unmarshallerRegistry;
    }

    /**
     * @return The {@link SdkHttpFullResponse} of the API call.
     */
    public SdkHttpFullResponse response() {
        return response;
    }

    /**
     * Lookup the marshaller for the given location andtype.
     *
     * @param location {@link MarshallLocation} of member.
     * @param marshallingType {@link MarshallingType} of member.
     * @return Unmarshaller implementation.
     * @throws SdkClientException if no unmarshaller is found.
     */
    public JsonUnmarshaller<Object> getUnmarshaller(MarshallLocation location, MarshallingType<?> marshallingType) {
        // A member being in the URI on a response is nonsensical; when a member is declared to be somewhere in the URI,
        // it should be found in the payload on response
        if (MarshallerUtil.locationInUri(location)) {
            location = MarshallLocation.PAYLOAD;
        }
        return unmarshallerRegistry.getUnmarshaller(location, marshallingType);
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
        private JsonUnmarshallerRegistry unmarshallerRegistry;

        private Builder() {
        }

        public Builder response(SdkHttpFullResponse response) {
            this.response = response;
            return this;
        }

        public Builder unmarshallerRegistry(JsonUnmarshallerRegistry unmarshallerRegistry) {
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
