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

import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.jsoncore.JsonValueNodeFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultProtocolUnmarshallDependencies implements ProtocolUnmarshallDependencies {
    private final JsonUnmarshallerRegistry jsonUnmarshallerRegistry;
    private final JsonValueNodeFactory nodeValueFactory;
    private final Map<MarshallLocation, TimestampFormatTrait.Format> timestampFormats;
    private final JsonFactory jsonFactory;

    private DefaultProtocolUnmarshallDependencies(Builder builder) {
        this.jsonUnmarshallerRegistry = Validate.notNull(builder.jsonUnmarshallerRegistry, "jsonUnmarshallerRegistry");
        this.nodeValueFactory = Validate.notNull(builder.nodeValueFactory, "nodeValueFactory");
        this.timestampFormats = Validate.notNull(builder.timestampFormats, "timestampFormats");
        this.jsonFactory = Validate.notNull(builder.jsonFactory, "jsonFactory");
    }

    @Override
    public JsonUnmarshallerRegistry jsonUnmarshallerRegistry() {
        return jsonUnmarshallerRegistry;
    }

    @Override
    public JsonValueNodeFactory nodeValueFactory() {
        return nodeValueFactory;
    }

    @Override
    public Map<MarshallLocation, TimestampFormatTrait.Format> timestampFormats() {
        return timestampFormats;
    }

    @Override
    public JsonFactory jsonFactory() {
        return jsonFactory;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private JsonUnmarshallerRegistry jsonUnmarshallerRegistry;
        private JsonValueNodeFactory nodeValueFactory;
        private Map<MarshallLocation, TimestampFormatTrait.Format> timestampFormats;
        private JsonFactory jsonFactory;

        public Builder jsonUnmarshallerRegistry(JsonUnmarshallerRegistry jsonUnmarshallerRegistry) {
            this.jsonUnmarshallerRegistry = jsonUnmarshallerRegistry;
            return this;
        }

        public Builder nodeValueFactory(JsonValueNodeFactory nodeValueFactory) {
            this.nodeValueFactory = nodeValueFactory;
            return this;
        }

        public Builder timestampFormats(Map<MarshallLocation, TimestampFormatTrait.Format> timestampFormats) {
            this.timestampFormats = timestampFormats;
            return this;
        }

        public Builder jsonFactory(JsonFactory jsonFactory) {
            this.jsonFactory = jsonFactory;
            return this;
        }

        public DefaultProtocolUnmarshallDependencies build() {
            return new DefaultProtocolUnmarshallDependencies(this);
        }
    }
}
