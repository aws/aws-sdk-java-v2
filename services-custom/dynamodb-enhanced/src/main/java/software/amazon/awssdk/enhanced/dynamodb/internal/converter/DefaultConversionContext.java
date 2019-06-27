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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.utils.Validate;

/**
 * The default implementation of {@link ConversionContext}.
 */
@SdkInternalApi
@ThreadSafe
public class DefaultConversionContext implements ConversionContext {
    private final String attributeName;
    private final SubtypeAttributeConverter<Object> converter;

    private DefaultConversionContext(DefaultConversionContext.Builder builder) {
        this.attributeName = builder.attributeName;
        this.converter = Validate.paramNotNull(builder.converter, "converter");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The name of the attribute being converted.
     */
    @Override
    public Optional<String> attributeName() {
        return Optional.ofNullable(this.attributeName);
    }

    @Override
    public SubtypeAttributeConverter<Object> attributeConverter() {
        return converter;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder implements ConversionContext.Builder {
        private String attributeName;
        private SubtypeAttributeConverter<Object> converter;

        private Builder() {}

        public Builder(DefaultConversionContext context) {
            this.attributeName = context.attributeName;
            this.converter = context.converter;
        }

        @Override
        public Builder attributeName(String attributeName) {
            this.attributeName = attributeName;
            return this;
        }

        @Override
        public Builder attributeConverter(SubtypeAttributeConverter<Object> converter) {
            this.converter = converter;
            return this;
        }

        public ConversionContext build() {
            return new DefaultConversionContext(this);
        }
    }
}
