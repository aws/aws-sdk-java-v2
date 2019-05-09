/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.internal.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ItemAttributeValueConverterChain;
import software.amazon.awssdk.enhanced.dynamodb.model.AttributeAware;
import software.amazon.awssdk.enhanced.dynamodb.model.ConverterAware;
import software.amazon.awssdk.utils.Validate;

/**
 * A base class from which {@link DefaultGeneratedRequestItem}, {@link DefaultGeneratedResponseItem},
 * {@link DefaultRequestItem} and {@link DefaultResponseItem} derive their implementations.
 */
@SdkInternalApi
@ThreadSafe
public abstract class DefaultItem<AttributeT> implements ConverterAware,
                                                         AttributeAware<AttributeT> {
    protected final ItemAttributeValueConverterChain converterChain;
    private final Map<String, AttributeT> attributes;
    private final List<ItemAttributeValueConverter> converters;

    protected DefaultItem(Builder<AttributeT, ?> builder) {
        this.converters = Collections.unmodifiableList(new ArrayList<>(builder.converters));
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
        this.converterChain = ItemAttributeValueConverterChain.create(converters());
    }

    @Override
    public Map<String, AttributeT> attributes() {
        return attributes;
    }

    @Override
    public AttributeT attribute(String attributeKey) {
        Validate.paramNotNull(attributeKey, "attributeKey");
        return attributes.get(attributeKey);
    }

    @Override
    public List<ItemAttributeValueConverter> converters() {
        return converters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultItem<?> that = (DefaultItem<?>) o;
        return attributes.equals(that.attributes) &&
               converters.equals(that.converters);
    }

    @Override
    public int hashCode() {
        int result = attributes.hashCode();
        result = 31 * result + converters.hashCode();
        result = 31 * result + converterChain.hashCode();
        return result;
    }

    public abstract static class Builder<AttributeT, BuilderT extends Builder<AttributeT, BuilderT>>
            implements ConverterAware.Builder,
                       AttributeAware.Builder<AttributeT> {
        private Map<String, AttributeT> attributes = new LinkedHashMap<>();
        private List<ItemAttributeValueConverter> converters = new ArrayList<>();

        protected Builder() {}

        protected Builder(DefaultItem<AttributeT> item) {
            this.attributes.putAll(item.attributes);
            this.converters.addAll(item.converters);
        }

        @Override
        public BuilderT putAttributes(Map<String, AttributeT> attributeValues) {
            Validate.paramNotNull(attributeValues, "attributeValues");
            Validate.noNullElements(attributeValues.keySet(), "Attribute keys must not be null.");
            this.attributes.putAll(attributeValues);
            return (BuilderT) this;
        }

        @Override
        public BuilderT putAttribute(String attributeKey, AttributeT attributeValue) {
            Validate.paramNotNull(attributeKey, "attributeKey");
            this.attributes.put(attributeKey, attributeValue);
            return (BuilderT) this;
        }

        @Override
        public BuilderT removeAttribute(String attributeKey) {
            Validate.paramNotNull(attributeKey, "attributeKey");
            this.attributes.remove(attributeKey);
            return (BuilderT) this;
        }

        @Override
        public BuilderT clearAttributes() {
            this.attributes.clear();
            return (BuilderT) this;
        }

        @Override
        public BuilderT addConverters(Collection<? extends ItemAttributeValueConverter> converters) {
            Validate.paramNotNull(converters, "converters");
            Validate.noNullElements(converters, "Converters must not contain null members.");
            this.converters.addAll(converters);
            return (BuilderT) this;
        }

        @Override
        public BuilderT addConverter(ItemAttributeValueConverter converter) {
            Validate.paramNotNull(converter, "converter");
            this.converters.add(converter);
            return (BuilderT) this;
        }

        @Override
        public BuilderT clearConverters() {
            this.converters.clear();
            return (BuilderT) this;
        }
    }
}
