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

package software.amazon.awssdk.utils;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A map from {@code AttributeMap.Key<T>} to {@code T} that ensures the values stored with a key matches the type associated with
 * the key. This does not implement {@link Map} because it has more strict typing requirements, but a {@link Map} can be
 * converted
 * to an {code AttributeMap} via the type-unsafe {@link AttributeMap} method.
 *
 * This can be used for storing configuration values ({@code OptionKey.LOG_LEVEL} to {@code Boolean.TRUE}), attaching
 * arbitrary attributes to a request chain ({@code RequestAttribute.CONFIGURATION} to {@code ClientConfiguration}) or similar
 * use-cases.
 */
@SdkProtectedApi
@Immutable
public final class AttributeMap implements ToCopyableBuilder<AttributeMap.Builder, AttributeMap> {

    private final Map<Key<?>, Object> attributes;

    private AttributeMap(Map<? extends Key<?>, ?> attributes) {
        this.attributes = new HashMap<>(attributes);
    }

    /**
     * Return true if the provided key is configured in this map. Useful for differentiating between whether the provided key was
     * not configured in the map or if it is configured, but its value is null.
     */
    public <T> boolean containsKey(Key<T> typedKey) {
        return attributes.containsKey(typedKey);
    }

    /**
     * Get the value associated with the provided key from this map. This will return null if the value is not set or if the
     * value
     * stored is null. These cases can be disambiguated using {@link #containsKey(Key)}.
     */
    public <T> T get(Key<T> key) {
        Validate.notNull(key, "Key to retrieve must not be null.");
        return key.convertValue(attributes.get(key));
    }

    /**
     * Merges two AttributeMaps into one. This object is given higher precedence then the attributes passed in as a parameter.
     *
     * @param lowerPrecedence Options to merge into 'this' AttributeMap object. Any attribute already specified in 'this' object
     *                        will be left as is since it has higher precedence.
     * @return New options with values merged.
     */
    public AttributeMap merge(AttributeMap lowerPrecedence) {
        Map<Key<?>, Object> copiedConfiguration = new HashMap<>(attributes);
        lowerPrecedence.attributes.forEach(copiedConfiguration::putIfAbsent);
        return new AttributeMap(copiedConfiguration);
    }

    public static AttributeMap empty() {
        return builder().build();
    }

    public AttributeMap copy() {
        return toBuilder().build();
    }

    /**
     * An abstract class extended by pseudo-enums defining the key for data that is stored in the {@link AttributeMap}. For
     * example, a {@code ClientOption<T>} may extend this to define options that can be stored in an {@link AttributeMap}.
     */
    public abstract static class Key<T> {

        private final Class<T> valueClass;

        /**
         * Configure the class of {@code T}.
         */
        protected Key(Class<T> valueClass) {
            this.valueClass = valueClass;
        }

        /**
         * Validate the provided value is of the correct type.
         */
        final void validateValue(Object value) {
            if (value != null) {
                Validate.isAssignableFrom(valueClass, value.getClass(),
                                          "Invalid option: %s. Required value of type %s, but was %s.",
                                          this, valueClass, value.getClass());
            }
        }

        /**
         * Validate the provided value is of the correct type and convert it to the proper type for this option.
         */
        final T convertValue(Object value) {
            validateValue(value);
            return valueClass.cast(value);
        }
    }

    @Override
    public String toString() {
        return attributes.toString();
    }

    @Override
    public int hashCode() {
        return attributes.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AttributeMap && attributes.equals(((AttributeMap) obj).attributes);
    }

    @Override
    public Builder toBuilder() {
        return builder().putAll(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements CopyableBuilder<Builder, AttributeMap> {

        private final Map<Key<?>, Object> configuration = new HashMap<>();

        private Builder() {
        }

        public <T> T get(Key<T> key) {
            Validate.notNull(key, "Key to retrieve must not be null.");
            return key.convertValue(configuration.get(key));
        }

        /**
         * Add a mapping between the provided key and value.
         */
        public <T> Builder put(Key<T> key, T value) {
            Validate.notNull(key, "Key to set must not be null.");
            configuration.put(key, value);
            return this;
        }

        /**
         * Adds all the attributes from the map provided. This is not type safe, and will throw an exception during creation if
         * a value in the map is not of the correct type for its key.
         */
        public Builder putAll(Map<? extends Key<?>, ?> attributes) {
            attributes.forEach((key, value) -> {
                key.validateValue(value);
                configuration.put(key, value);
            });
            return this;
        }

        @Override
        public AttributeMap build() {
            return new AttributeMap(configuration);
        }
    }

}
