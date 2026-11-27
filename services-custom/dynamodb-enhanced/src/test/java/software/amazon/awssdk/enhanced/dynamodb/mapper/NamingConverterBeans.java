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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Models and converters used by the naming converter tests.
 */
final class NamingConverterBeans {
    static final String TOKEN_PREFIX = "token:";

    private NamingConverterBeans() {
    }

    /**
     * A value stored by {@link TokenConverter}.
     */
    public static final class Token {
        private final String value;

        public Token(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            return Objects.equals(value, ((Token) other).value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

    /**
     * A value whose converter writes a map.
     */
    public static final class TokenMap {
        private final String value;

        public TokenMap(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Writes a token as a string with a fixed prefix.
     */
    public static final class TokenConverter implements AttributeConverter<Token> {
        @Override
        public AttributeValue transformFrom(Token input) {
            return AttributeValue.fromS(TOKEN_PREFIX + input.value());
        }

        @Override
        public Token transformTo(AttributeValue input) {
            return new Token(input.s().substring(TOKEN_PREFIX.length()));
        }

        @Override
        public EnhancedType<Token> type() {
            return EnhancedType.of(Token.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    /**
     * Writes a map whose inner key is not a Java property name.
     */
    public static final class TokenMapConverter implements AttributeConverter<TokenMap> {
        @Override
        public AttributeValue transformFrom(TokenMap input) {
            return AttributeValue.fromM(CollectionsHolder.singleton("innerKey", AttributeValue.fromS(input.value())));
        }

        @Override
        public TokenMap transformTo(AttributeValue input) {
            return new TokenMap(input.m().get("innerKey").s());
        }

        @Override
        public EnhancedType<TokenMap> type() {
            return EnhancedType.of(TokenMap.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.M;
        }
    }

    /**
     * Supplies {@link TokenConverter} for {@link Token}.
     */
    public static final class TokenConverterProvider implements AttributeConverterProvider {
        private final TokenConverter converter = new TokenConverter();

        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            if (enhancedType.rawClass().equals(Token.class)) {
                return cast(converter);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private static <T> AttributeConverter<T> cast(TokenConverter converter) {
            return (AttributeConverter<T>) converter;
        }
    }

    /**
     * A bean with converted attributes, one of them annotated.
     */
    @DynamoDbBean
    public static class ConvertedBean {
        private Token id;
        private Token customerCode;
        private TokenMap payload;

        @DynamoDbPartitionKey
        @DynamoDbAttribute("PK")
        @DynamoDbConvertedBy(TokenConverter.class)
        public Token getId() {
            return id;
        }

        public void setId(Token id) {
            this.id = id;
        }

        @DynamoDbConvertedBy(TokenConverter.class)
        public Token getCustomerCode() {
            return customerCode;
        }

        public void setCustomerCode(Token customerCode) {
            this.customerCode = customerCode;
        }

        @DynamoDbConvertedBy(TokenMapConverter.class)
        public TokenMap getPayload() {
            return payload;
        }

        public void setPayload(TokenMap payload) {
            this.payload = payload;
        }
    }

    /**
     * A bean whose token conversion comes from {@link #converterProviders()}.
     */
    @DynamoDbBean(converterProviders = {TokenConverterProvider.class, DefaultAttributeConverterProvider.class})
    public static class ProviderBean {
        private String id;
        private Token customerCode;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Token getCustomerCode() {
            return customerCode;
        }

        public void setCustomerCode(Token customerCode) {
            this.customerCode = customerCode;
        }
    }

    /**
     * A discovered child with its own converter.
     */
    @DynamoDbBean
    public static class ConvertedChild {
        private Token customerCode;

        @DynamoDbConvertedBy(TokenConverter.class)
        public Token getCustomerCode() {
            return customerCode;
        }

        public void setCustomerCode(Token customerCode) {
            this.customerCode = customerCode;
        }
    }

    /**
     * A parent that discovers {@link ConvertedChild}.
     */
    @DynamoDbBean
    public static class ConvertedParent {
        private String id;
        private ConvertedChild child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public ConvertedChild getChild() {
            return child;
        }

        public void setChild(ConvertedChild child) {
            this.child = child;
        }
    }

    /**
     * An immutable bean with a converted attribute.
     */
    @DynamoDbImmutable(builder = ConvertedImmutable.Builder.class)
    public static final class ConvertedImmutable {
        private final String id;
        private final Token customerCode;

        private ConvertedImmutable(Builder builder) {
            this.id = builder.id;
            this.customerCode = builder.customerCode;
        }

        /**
         * Returns a new builder.
         */
        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        @DynamoDbConvertedBy(TokenConverter.class)
        public Token customerCode() {
            return customerCode;
        }

        /**
         * Builds a {@link ConvertedImmutable}.
         */
        public static final class Builder {
            private String id;
            private Token customerCode;

            /**
             * Sets the key.
             */
            public Builder id(String id) {
                this.id = id;
                return this;
            }

            /**
             * Sets the converted code.
             */
            public Builder customerCode(Token customerCode) {
                this.customerCode = customerCode;
                return this;
            }

            /**
             * Returns the record.
             */
            public ConvertedImmutable build() {
                return new ConvertedImmutable(this);
            }
        }
    }

    /**
     * A mutable record for static schemas.
     */
    public static class StaticConverted {
        private String id;
        private Token customerCode;
        private List<Token> tokens;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Token getCustomerCode() {
            return customerCode;
        }

        public void setCustomerCode(Token customerCode) {
            this.customerCode = customerCode;
        }

        public List<Token> getTokens() {
            return tokens;
        }

        public void setTokens(List<Token> tokens) {
            this.tokens = tokens;
        }
    }

    /**
     * Holds one map entry without a static import that would clash with test imports.
     */
    static final class CollectionsHolder {
        private CollectionsHolder() {
        }

        /**
         * Returns a map with one entry.
         */
        static Map<String, AttributeValue> singleton(String key, AttributeValue value) {
            return java.util.Collections.singletonMap(key, value);
        }
    }
}
