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

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
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
        this.attributes = new HashMap<>();
    }

    protected ExecutionAttributes(Map<? extends ExecutionAttribute<?>, ?> attributes) {
        this.attributes = new HashMap<>(attributes);
    }

    /**
     * Retrieve the current value of the provided attribute in this collection of attributes. This will return null if the value
     * is not set.
     */
    @SuppressWarnings("unchecked") // Cast is safe due to implementation of {@link #putAttribute}
    public <U> U getAttribute(ExecutionAttribute<U> attribute) {
        return (U) attributes.get(attribute);
    }

    /**
     * Retrieve the collection of attributes.
     */
    public Map<ExecutionAttribute<?>, ?> getAttributes() {
        return attributes;
    }

    /**
     * Update or set the provided attribute in this collection of attributes.
     */
    public <U> ExecutionAttributes putAttribute(ExecutionAttribute<U> attribute, U value) {
        this.attributes.put(attribute, value);
        return this;
    }

    /**
    * Merge attributes of a lower precedence into the current higher precedence current collection.
    */
    public ExecutionAttributes merge(ExecutionAttributes lowerPrecedenceExecutionAttributes) {
        Map<ExecutionAttribute<?>, Object> copiedAttributes = new HashMap<>(this.attributes);
        lowerPrecedenceExecutionAttributes.getAttributes().forEach(copiedAttributes::putIfAbsent);
        return new ExecutionAttributes(copiedAttributes);
    }

    /**
     * Set the provided attribute in this collection of attributes if it does not already exist in the collection.
     */
    public <U> ExecutionAttributes putAttributeIfAbsent(ExecutionAttribute<U> attribute, U value) {
        attributes.putIfAbsent(attribute, value);
        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().putAll(attributes);
    }

    public ExecutionAttributes copy() {
        return toBuilder().build();
    }

    public static final class Builder implements CopyableBuilder<ExecutionAttributes.Builder, ExecutionAttributes> {

        private final Map<ExecutionAttribute<?>, Object> executionAttributes = new HashMap<>();

        private Builder() {
        }


        public <T> T get(ExecutionAttribute<T> key) {
            Validate.notNull(key, "Key to retrieve must not be null.");
            return (T) executionAttributes.get(key);
        }

        /**
         * Add a mapping between the provided key and value.
         */
        public <T> ExecutionAttributes.Builder put(ExecutionAttribute<T> key, T value) {
            Validate.notNull(key, "Key to set must not be null.");
            executionAttributes.put(key, value);
            return this;
        }

        /**
         * Adds all the attributes from the map provided.
         */
        public ExecutionAttributes.Builder putAll(Map<? extends ExecutionAttribute<?>, ?> attributes) {
            executionAttributes.putAll(attributes);
            return this;
        }

        @Override
        public ExecutionAttributes build() {
            return new ExecutionAttributes(executionAttributes);
        }
    }
}
