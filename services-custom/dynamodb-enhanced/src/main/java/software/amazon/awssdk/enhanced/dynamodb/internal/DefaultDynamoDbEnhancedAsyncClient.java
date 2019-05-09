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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import java.util.Collection;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.converter.DefaultConverterChain;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ItemAttributeValueConverterChain;
import software.amazon.awssdk.enhanced.dynamodb.internal.model.DefaultAsyncTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.utils.Validate;

/**
 * The default implementation of {@link DynamoDbEnhancedAsyncClient}.
 */
@SdkInternalApi
@ThreadSafe
public class DefaultDynamoDbEnhancedAsyncClient implements DynamoDbEnhancedAsyncClient {
    private boolean shouldCloseUnderlyingClient;
    private final DynamoDbAsyncClient client;
    private final ItemAttributeValueConverterChain converter;

    private DefaultDynamoDbEnhancedAsyncClient(Builder builder) {
        if (builder.client == null) {
            this.client = DynamoDbAsyncClient.create();
            this.shouldCloseUnderlyingClient = true;
        } else {
            this.client = builder.client;
            this.shouldCloseUnderlyingClient = false;
        }

        this.converter = builder.converterChain.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AsyncTable table(String tableName) {
        return DefaultAsyncTable.builder()
                                .converter(converter)
                                .dynamoDbAsyncClient(client)
                                .name(tableName)
                                .build();
    }

    @Override
    public void close() {
        if (shouldCloseUnderlyingClient) {
            client.close();
        }
    }

    @Override
    public Builder toBuilder() {
        throw new UnsupportedOperationException();
    }

    public static class Builder implements DynamoDbEnhancedAsyncClient.Builder {
        private ItemAttributeValueConverterChain.Builder converterChain =
                ItemAttributeValueConverterChain.builder()
                                                .parent(DefaultConverterChain.create());
        private DynamoDbAsyncClient client;

        private Builder() {}

        @Override
        public Builder dynamoDbClient(DynamoDbAsyncClient client) {
            this.client = client;
            return this;
        }

        @Override
        public Builder addConverters(Collection<? extends ItemAttributeValueConverter> converters) {
            Validate.paramNotNull(converters, "converters");
            Validate.noNullElements(converters, "Converters must not contain null members.");
            converterChain.addConverters(converters);
            return this;
        }

        @Override
        public Builder addConverter(ItemAttributeValueConverter converter) {
            Validate.paramNotNull(converter, "converter");
            converterChain.addConverter(converter);
            return this;
        }

        @Override
        public Builder clearConverters() {
            converterChain.clearConverters();
            return this;
        }

        @Override
        public DynamoDbEnhancedAsyncClient build() {
            return new DefaultDynamoDbEnhancedAsyncClient(this);
        }
    }
}
