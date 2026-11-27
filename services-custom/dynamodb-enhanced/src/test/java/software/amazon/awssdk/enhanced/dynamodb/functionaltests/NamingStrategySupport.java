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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingConverterItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStaticConverterItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyAddress;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyProfile;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingToken;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.PlainNamingItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.TokenConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingCases;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchemaParams;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Builds schemas and reads low-level items for the naming functional tests.
 */
public final class NamingStrategySupport {
    private NamingStrategySupport() {
    }

    /**
     * Returns the five strategies used by every functional class.
     */
    public static Collection<Object[]> parameters() {
        return AttributeNamingCases.functionalParameters();
    }

    /**
     * Returns the stored name for a Java property or a static {@code name} value.
     */
    public static String stored(AttributeNamingStrategy strategy, String sourceName) {
        return AttributeNamingCases.storedName(strategy, sourceName);
    }

    /**
     * Returns a bean schema for {@link NamingStrategyItem}.
     */
    public static TableSchema<NamingStrategyItem> itemSchema(AttributeNamingStrategy strategy) {
        return TableSchemaFrom.bean(strategy);
    }

    /**
     * Returns a bean schema for the given annotated class.
     */
    public static <T> TableSchema<T> beanSchema(Class<T> itemClass, AttributeNamingStrategy strategy) {
        return TableSchemaFrom.bean(itemClass, strategy);
    }

    /**
     * Returns a bean converter schema. {@code PK} stays annotated.
     */
    public static TableSchema<NamingConverterItem> converterBeanSchema(AttributeNamingStrategy strategy) {
        return TableSchemaFrom.bean(NamingConverterItem.class, strategy);
    }

    /**
     * Returns a static converter schema. {@code PK} is transformed.
     */
    public static TableSchema<NamingStaticConverterItem> converterStaticSchema(AttributeNamingStrategy strategy) {
        return StaticTableSchema.builder(NamingStaticConverterItem.class)
                                .newItemSupplier(NamingStaticConverterItem::new)
                                .attributeNamingStrategy(strategy)
                                .addAttribute(String.class, attribute -> attribute.name("PK")
                                                                                 .getter(NamingStaticConverterItem::getId)
                                                                                 .setter(NamingStaticConverterItem::setId)
                                                                                 .tags(primaryPartitionKey()))
                                .addAttribute(String.class, attribute -> attribute.name("orderId")
                                                                                 .getter(NamingStaticConverterItem::getOrderId)
                                                                                 .setter(NamingStaticConverterItem::setOrderId)
                                                                                 .tags(primarySortKey()))
                                .addAttribute(EnhancedType.of(NamingToken.class),
                                              attribute -> attribute.name("customerCode")
                                                                   .getter(NamingStaticConverterItem::getCustomerCode)
                                                                   .setter(NamingStaticConverterItem::setCustomerCode)
                                                                   .attributeConverter(new TokenConverter()))
                                .build();
    }

    /**
     * Returns a static schema whose partition key input is {@code PK}.
     */
    public static TableSchema<PlainNamingItem> staticKeySchema(AttributeNamingStrategy strategy) {
        return StaticTableSchema.builder(PlainNamingItem.class)
                                .newItemSupplier(PlainNamingItem::new)
                                .attributeNamingStrategy(strategy)
                                .addAttribute(String.class, attribute -> attribute.name("PK")
                                                                                 .getter(PlainNamingItem::getId)
                                                                                 .setter(PlainNamingItem::setId)
                                                                                 .tags(primaryPartitionKey()))
                                .addAttribute(String.class, attribute -> attribute.name("customerId")
                                                                                 .getter(PlainNamingItem::getCustomerId)
                                                                                 .setter(PlainNamingItem::setCustomerId))
                                .build();
    }

    /**
     * Returns a plain schema that keeps Java names.
     */
    public static TableSchema<PlainNamingItem> plainSchema() {
        return TableSchema.fromBean(PlainNamingItem.class);
    }

    /**
     * Returns an item with the shared attributes set.
     */
    public static NamingStrategyItem item(String id, String orderId, String customerId) {
        NamingStrategyAddress address = new NamingStrategyAddress();
        address.setCityName("Rome");
        address.setStreetName("Via");
        NamingStrategyProfile profile = new NamingStrategyProfile();
        profile.setDisplayName("Ada");
        NamingStrategyItem item = new NamingStrategyItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setCustomerId(customerId);
        item.setStatusCode("open");
        item.setEmailAddress(customerId + "@example.com");
        item.setCreatedOn("2020-01-01");
        item.setHomeCity(address);
        item.setFlatProfile(profile);
        return item;
    }

    /**
     * Returns an equality condition on one stored attribute.
     */
    public static Expression equalTo(String storedName, String value) {
        Map<String, String> names = new HashMap<>();
        names.put("#a", storedName);
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":v", AttributeValue.fromS(value));
        return Expression.builder()
                         .expression("#a = :v")
                         .expressionNames(names)
                         .expressionValues(values)
                         .build();
    }

    /**
     * Reads one item with the low-level client.
     * The key uses {@code PK} and the resolved sort name.
     */
    public static Map<String, AttributeValue> lowLevelItem(DynamoDbClient client,
                                                           String tableName,
                                                           String id,
                                                           String orderId,
                                                           AttributeNamingStrategy strategy) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("PK", AttributeValue.fromS(id));
        key.put(stored(strategy, "orderId"), AttributeValue.fromS(orderId));
        return client.getItem(request -> request.tableName(tableName).key(key)).item();
    }

    /**
     * Reads one item whose only key is the given attribute.
     */
    public static Map<String, AttributeValue> lowLevelByKey(DynamoDbClient client,
                                                            String tableName,
                                                            String keyName,
                                                            String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(keyName, AttributeValue.fromS(id));
        return client.getItem(request -> request.tableName(tableName).key(key)).item();
    }

    /**
     * Counts strategy calls during schema construction.
     */
    public static final class CountingStrategy implements AttributeNamingStrategy {
        private final AttributeNamingStrategy delegate;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<String> sources = new ArrayList<>();

        /**
         * Counts every call and then applies the delegate.
         */
        public CountingStrategy(AttributeNamingStrategy delegate) {
            this.delegate = delegate;
        }

        /**
         * Returns how many times {@link #transform(String)} was called.
         */
        public int calls() {
            return calls.get();
        }

        /**
         * Returns the source names received, in call order.
         */
        public List<String> sources() {
            return new ArrayList<>(sources);
        }

        @Override
        public String transform(String sourceName) {
            calls.incrementAndGet();
            sources.add(sourceName);
            return delegate.transform(sourceName);
        }
    }

    /**
     * Hides the generic schema call so callers stay short.
     */
    private static final class TableSchemaFrom {
        private TableSchemaFrom() {
        }

        private static TableSchema<NamingStrategyItem> bean(AttributeNamingStrategy strategy) {
            return bean(NamingStrategyItem.class, strategy);
        }

        private static <T> TableSchema<T> bean(Class<T> itemClass, AttributeNamingStrategy strategy) {
            return TableSchema.fromBean(
                BeanTableSchemaParams.builder(itemClass).attributeNamingStrategy(strategy).build());
        }
    }
}
