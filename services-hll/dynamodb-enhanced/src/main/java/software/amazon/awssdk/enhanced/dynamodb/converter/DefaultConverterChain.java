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

package software.amazon.awssdk.enhanced.dynamodb.converter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.IdentityConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.InstantConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.IntegerConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.ListConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.MapConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.RequestItemConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.ResponseItemConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.StringConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ItemAttributeValueConverterChain;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.RequestItem;
import software.amazon.awssdk.enhanced.dynamodb.model.ResponseItem;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

/**
 * A {@link ItemAttributeValueConverter} that includes all of the converters built into the SDK.
 *
 * This is the root converter for all created {@link DynamoDbEnhancedClient}s and {@link DynamoDbEnhancedAsyncClient}s.
 *
 * Supported Number Types:
 * <ul>
 *     <li>{@link Instant}</li>
 *     <li>{@link Integer}</li>
 * </ul>
 *
 * Supported String Types:
 * <ul>
 *     <li>{@link String}</li>
 * </ul>
 *
 * Supported List Types:
 * <ul>
 *     <li>{@link List} (plus subtypes)</li>
 * </ul>
 *
 * Supported Item Types:
 * <ul>
 *     <li>{@link Map} (plus subtypes)</li>
 *     <li>{@link RequestItem} (plus subtypes)</li>
 *     <li>{@link ResponseItem} (plus subtypes)</li>
 *     <li>{@link ItemAttributeValue}</li>
 * </ul>
 *
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class DefaultConverterChain implements ItemAttributeValueConverter {
    private static final ItemAttributeValueConverter CHAIN;

    static {
        CHAIN = ItemAttributeValueConverterChain.builder()
                                                // Exact InstanceOf Converters

                                                .addConverter(new InstantConverter())
                                                .addConverter(new IntegerConverter())
                                                .addConverter(new StringConverter())
                                                .addConverter(new IdentityConverter())

                                                // InstanceOf Converters
                                                // Potential optimization: allow InstanceOf converters to specify a set of
                                                // types that should be cached in an eager fashion (eg. DefaultRequestItem)
                                                .addConverter(new RequestItemConverter())
                                                .addConverter(new ResponseItemConverter())
                                                .addConverter(new ListConverter())
                                                .addConverter(new MapConverter())
                                                .build();
    }

    private DefaultConverterChain() {}

    /**
     * Create a default convert chain that contains all of the converters built into the SDK.
     */
    public static DefaultConverterChain create() {
        return new DefaultConverterChain();
    }

    @Override
    public ConversionCondition defaultConversionCondition() {
        return CHAIN.defaultConversionCondition();
    }

    @Override
    public ItemAttributeValue toAttributeValue(Object input, ConversionContext context) {
        return CHAIN.toAttributeValue(input, context);
    }

    @Override
    public Object fromAttributeValue(ItemAttributeValue input, TypeToken<?> desiredType, ConversionContext context) {
        return CHAIN.fromAttributeValue(input, desiredType, context);
    }
}
