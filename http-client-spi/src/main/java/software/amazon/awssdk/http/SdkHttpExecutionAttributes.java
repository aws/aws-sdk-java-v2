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

package software.amazon.awssdk.http;

import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An immutable collection of {@link SdkHttpExecutionAttribute}s that can be configured on an {@link AsyncExecuteRequest} via
 * {@link AsyncExecuteRequest.Builder#httpExecutionAttributes(SdkHttpExecutionAttributes)}
 */
@SdkPublicApi
public final class SdkHttpExecutionAttributes implements ToCopyableBuilder<SdkHttpExecutionAttributes.Builder,
    SdkHttpExecutionAttributes> {
    private final AttributeMap attributes;

    private SdkHttpExecutionAttributes(Builder builder) {
        this.attributes = builder.sdkHttpExecutionAttributes.build();
    }

    /**
     * Retrieve the current value of the provided attribute in this collection of attributes. This will return null if the value
     * is not set.
     */
    public <T> T getAttribute(SdkHttpExecutionAttribute<T> attribute) {
        return attributes.get(attribute);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(attributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SdkHttpExecutionAttributes that = (SdkHttpExecutionAttributes) o;

        return Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return attributes.hashCode();
    }

    public static final class Builder implements CopyableBuilder<SdkHttpExecutionAttributes.Builder, SdkHttpExecutionAttributes> {
        private AttributeMap.Builder sdkHttpExecutionAttributes = AttributeMap.builder();

        private Builder(AttributeMap attributes) {
            sdkHttpExecutionAttributes = attributes.toBuilder();
        }

        private Builder() {
        }

        /**
         * Add a mapping between the provided key and value.
         */
        public <T> SdkHttpExecutionAttributes.Builder put(SdkHttpExecutionAttribute<T> key, T value) {
            Validate.notNull(key, "Key to set must not be null.");
            sdkHttpExecutionAttributes.put(key, value);
            return this;
        }

        /**
         * Adds all the attributes from the map provided.
         */
        public SdkHttpExecutionAttributes.Builder putAll(Map<? extends SdkHttpExecutionAttribute<?>, ?> attributes) {
            sdkHttpExecutionAttributes.putAll(attributes);
            return this;
        }

        @Override
        public SdkHttpExecutionAttributes build() {
            return new SdkHttpExecutionAttributes(this);
        }
    }
}