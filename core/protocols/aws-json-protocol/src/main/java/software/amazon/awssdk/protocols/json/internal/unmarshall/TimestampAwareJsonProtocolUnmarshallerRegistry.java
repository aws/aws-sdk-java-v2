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
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.utils.Validate;

/**
 * Registry of {@link JsonUnmarshaller} implementations by location and type. This implementation has special logic to handle
 * Instant values to support {@link TimestampFormatTrait} default values. It does so by splitting the responsability in two
 * separated registries, one that is used exclusively for marshalling type {@link MarshallingType#INSTANT} and another one for
 * the rest. This allow us to combine shared static registries with more dynamic ones that change depending on the set of
 * default values for timestamp formats.
 */
@SdkInternalApi
public final class TimestampAwareJsonProtocolUnmarshallerRegistry implements JsonUnmarshallerRegistry {
    private final JsonUnmarshallerRegistry registry;
    private final JsonUnmarshallerRegistry instantRegistry;

    private TimestampAwareJsonProtocolUnmarshallerRegistry(Builder builder) {
        this.registry = Validate.notNull(builder.registry, "registry");
        this.instantRegistry = Validate.notNull(builder.instantRegistry, "instantRegistry");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> JsonUnmarshaller<Object> getUnmarshaller(MarshallLocation marshallLocation, MarshallingType<T> marshallingType) {
        if (marshallingType == MarshallingType.INSTANT) {
            return instantRegistry.getUnmarshaller(marshallLocation, marshallingType);
        }
        return registry.getUnmarshaller(marshallLocation, marshallingType);
    }

    /**
     * @return Builder instance to construct a {@link DefaultJsonUnmarshallerRegistry}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link DefaultJsonUnmarshallerRegistry}.
     */
    public static final class Builder {
        private JsonUnmarshallerRegistry registry;
        private JsonUnmarshallerRegistry instantRegistry;

        private Builder() {
        }

        /**
         * Add the default registry.
         */
        public <T> Builder registry(JsonUnmarshallerRegistry registry) {
            this.registry = registry;
            return this;
        }

        /**
         * Add the protocol specific instant registry.
         */
        public <T> Builder instantRegistry(JsonUnmarshallerRegistry instantRegistry) {
            this.instantRegistry = instantRegistry;
            return this;
        }

        /**
         * @return An immutable {@link DefaultJsonUnmarshallerRegistry} object.
         */
        public JsonUnmarshallerRegistry build() {
            return new TimestampAwareJsonProtocolUnmarshallerRegistry(this);
        }
    }
}
