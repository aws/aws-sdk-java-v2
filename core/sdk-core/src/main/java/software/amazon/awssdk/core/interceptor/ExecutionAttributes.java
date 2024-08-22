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

package software.amazon.awssdk.core.interceptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A mutable collection of {@link ExecutionAttribute}s that can be modified by {@link ExecutionInterceptor}s in order to save and
 * retrieve information specific to the current execution.
 *
 * This is useful for sharing data between {@link ExecutionInterceptor} method calls specific to a particular execution.
 */
@SdkPublicApi
@NotThreadSafe
public class ExecutionAttributes implements ToCopyableBuilder<ExecutionAttributes.Builder, ExecutionAttributes> {
    private final Map<ExecutionAttribute<?>, Object> attributes;

    public ExecutionAttributes() {
        this.attributes = new HashMap<>(32);
    }

    protected ExecutionAttributes(Map<? extends ExecutionAttribute<?>, ?> attributes) {
        this.attributes = new HashMap<>(attributes);
    }
    
    /**
     * Retrieve the current value of the provided attribute in this collection of attributes. This will return null if the value
     * is not set.
     */
    public <U> U getAttribute(ExecutionAttribute<U> attribute) {
        return attribute.storage().get(attributes);
    }

    /**
     * Retrieve the collection of attributes.
     */
    public Map<ExecutionAttribute<?>, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Retrieve the Optional current value of the provided attribute in this collection of attributes.
     * This will return Optional Value.
     */
    public <U> Optional<U> getOptionalAttribute(ExecutionAttribute<U> attribute) {
        return Optional.ofNullable(getAttribute(attribute));
    }

    /**
     * Update or set the provided attribute in this collection of attributes.
     */
    public <U> ExecutionAttributes putAttribute(ExecutionAttribute<U> attribute, U value) {
        attribute.storage().set(attributes, value);
        return this;
    }

    /**
     * Set the provided attribute in this collection of attributes if it does not already exist in the collection.
     */
    public <U> ExecutionAttributes putAttributeIfAbsent(ExecutionAttribute<U> attribute, U value) {
        attribute.storage().setIfAbsent(attributes, value);
        return this;
    }

    /**
     * Merge attributes of a higher precedence into the current lower precedence collection.
     */
    public ExecutionAttributes merge(ExecutionAttributes lowerPrecedenceExecutionAttributes) {
        Map<ExecutionAttribute<?>, Object> copiedAttributes = new HashMap<>(this.attributes);
        lowerPrecedenceExecutionAttributes.getAttributes().forEach(copiedAttributes::putIfAbsent);
        return new ExecutionAttributes(copiedAttributes);
    }

    /**
     * Add the provided attributes to this attribute, if the provided attribute does not exist.
     */
    public void putAbsentAttributes(ExecutionAttributes lowerPrecedenceExecutionAttributes) {
        if (lowerPrecedenceExecutionAttributes != null) {
            lowerPrecedenceExecutionAttributes.getAttributes().forEach(attributes::putIfAbsent);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new ExecutionAttributes.Builder(this);
    }

    public ExecutionAttributes copy() {
        return toBuilder().build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || !(o instanceof ExecutionAttributes)) {
            return false;
        }

        ExecutionAttributes that = (ExecutionAttributes) o;

        return attributes != null ? attributes.equals(that.attributes) : that.attributes == null;
    }

    @Override
    public int hashCode() {
        return attributes != null ? attributes.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("ExecutionAttributes")
                       .add("attributes", attributes.keySet())
                       .build();
    }

    public static ExecutionAttributes unmodifiableExecutionAttributes(ExecutionAttributes attributes) {
        return new UnmodifiableExecutionAttributes(attributes);
    }

    private static class UnmodifiableExecutionAttributes extends ExecutionAttributes {
        UnmodifiableExecutionAttributes(ExecutionAttributes executionAttributes) {
            super(executionAttributes.attributes);
        }

        @Override
        public <U> ExecutionAttributes putAttribute(ExecutionAttribute<U> attribute, U value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U> ExecutionAttributes putAttributeIfAbsent(ExecutionAttribute<U> attribute, U value) {
            throw new UnsupportedOperationException();
        }
    }

    public static final class Builder implements CopyableBuilder<ExecutionAttributes.Builder, ExecutionAttributes> {
        private final Map<ExecutionAttribute<?>, Object> executionAttributes = new HashMap<>(32);

        private Builder() {
        }

        private Builder(ExecutionAttributes source) {
            this.executionAttributes.putAll(source.attributes);
        }

        /**
         * Add a mapping between the provided key and value.
         */
        public <T> ExecutionAttributes.Builder put(ExecutionAttribute<T> key, T value) {
            Validate.notNull(key, "Key to set must not be null.");
            key.storage().set(executionAttributes, value);
            return this;
        }

        /**
         * Adds all the attributes from the map provided.
         */
        public ExecutionAttributes.Builder putAll(Map<? extends ExecutionAttribute<?>, ?> attributes) {
            attributes.forEach(this::unsafePut);
            return this;
        }

        /**
         * There is no way to make this safe without runtime checks, which we can't do because we don't have the class of T.
         * This will just throw an exception at runtime if the types don't match up.
         */
        @SuppressWarnings("unchecked")
        private <T> void unsafePut(ExecutionAttribute<T> key, Object value) {
            key.storage().set(executionAttributes, (T) value);
        }

        @Override
        public ExecutionAttributes build() {
            return new ExecutionAttributes(executionAttributes);
        }
    }
}
