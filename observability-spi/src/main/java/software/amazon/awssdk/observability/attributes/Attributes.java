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

package software.amazon.awssdk.observability.attributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class Attributes {

    private final Map<String, Object> attributes;

    private Attributes(Map<String, Object> attributes) {
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    public Set<String> keySet() {
        return attributes.keySet();
    }

    public Map<String, Object> asMap() {
        return attributes;
    }

    public void forEach(BiConsumer<String, Object> consumer) {
        attributes.forEach(consumer);
    }

    public Optional<String> getString(String key) {
        return get(key).filter(v -> v instanceof String).map(v -> (String) v);
    }

    public Optional<Boolean> getBoolean(String key) {
        return get(key).filter(v -> v instanceof Boolean).map(v -> (Boolean) v);
    }

    public Optional<Double> getDouble(String key) {
        return get(key).filter(v -> v instanceof Double).map(v -> (Double) v);
    }

    public Optional<Long> getLong(String key) {
        return get(key).filter(v -> v instanceof Long).map(v -> (Long) v);
    }

    public <T> Optional<List<T>> getArray(String key, Class<T> type) {
        return get(key)
            .filter(v -> v instanceof List)
            .map(v -> (List<?>) v)
            .filter(list -> !list.isEmpty() && type.isInstance(list.get(0)))
            .map(list -> list.stream()
                             .map(type::cast)
                             .collect(Collectors.toList()));
    }

    public int size() {
        return attributes.size();
    }

    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    @Override
    public String toString() {
        return attributes.toString();
    }

    public static final class Builder {
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        public Builder put(String key, String value) {
            validateKey(key);
            validateValue(value);
            attributes.put(key, value);
            return this;
        }

        public Builder put(String key, boolean value) {
            validateKey(key);
            attributes.put(key, value);
            return this;
        }

        public Builder put(String key, double value) {
            validateKey(key);
            attributes.put(key, value);
            return this;
        }

        public Builder put(String key, long value) {
            validateKey(key);
            attributes.put(key, value);
            return this;
        }

        public <T> Builder putArray(String key, List<T> values, Class<T> type) {
            validateKey(key);
            Objects.requireNonNull(values, "Attribute values must not be null");
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Attribute array must not be empty");
            }
            for (T v : values) {
                validateValue(v);
                if (!type.isInstance(v)) {
                    throw new IllegalArgumentException("Array values must be of type " + type.getSimpleName());
                }
            }
            attributes.put(key, values);
            return this;
        }

        public Attributes build() {
            return new Attributes(attributes);
        }

        private void validateKey(String key) {
            Objects.requireNonNull(key, "Attribute key must not be null");
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Attribute key must not be empty");
            }
        }

        private void validateValue(Object value) {
            Objects.requireNonNull(value, "Attribute value must not be null");
            if (!(value instanceof String || value instanceof Boolean || value instanceof Double || value instanceof Long)) {
                throw new IllegalArgumentException("Unsupported attribute value type: " + value.getClass().getSimpleName());
            }
        }
    }
}
