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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.ConvertableItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Validate;

/**
 * The default implementation of {@link ConvertableItemAttributeValue}.
 */
@SdkInternalApi
@ThreadSafe
public final class DefaultConvertableItemAttributeValue implements ConvertableItemAttributeValue {
    private final ItemAttributeValue attributeValue;
    private final ConversionContext conversionContext;

    private DefaultConvertableItemAttributeValue(Builder builder) {
        this.attributeValue = Validate.paramNotNull(builder.attributeValue, "attributeValue");
        this.conversionContext = Validate.paramNotNull(builder.conversionContext, "conversionContext");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> T as(Class<T> type) {
        Validate.paramNotNull(type, "type");
        Object result = conversionContext.converter()
                                         .fromAttributeValue(attributeValue, TypeToken.from(type), conversionContext);
        return validateConverterOutput(type, result);
    }

    @Override
    public <T> T as(TypeToken<T> type) {
        Validate.paramNotNull(type, "type");
        Object result = conversionContext.converter()
                                         .fromAttributeValue(attributeValue, type, conversionContext);
        return validateConverterOutput(type.rawClass(), result);
    }

    private <T> T validateConverterOutput(Class<T> type, Object output) {
        return Validate.isInstanceOf(type, output, "Converter generated a %s after a %s was requested.", output.getClass(), type);
    }

    @Override
    public ItemAttributeValue attributeValue() {
        return attributeValue;
    }

    public static class Builder {
        private ItemAttributeValue attributeValue;
        private ConversionContext conversionContext;

        private Builder() {}

        public Builder attributeValue(ItemAttributeValue attributeValue) {
            this.attributeValue = attributeValue;
            return this;
        }

        public Builder conversionContext(ConversionContext conversionContext) {
            this.conversionContext = conversionContext;
            return this;
        }

        public Builder conversionContext(Consumer<ConversionContext.Builder> conversionContext) {
            ConversionContext.Builder context = ConversionContext.builder();
            conversionContext.accept(context);
            conversionContext(context.build());
            return this;
        }

        public DefaultConvertableItemAttributeValue build() {
            return new DefaultConvertableItemAttributeValue(this);
        }
    }
}
