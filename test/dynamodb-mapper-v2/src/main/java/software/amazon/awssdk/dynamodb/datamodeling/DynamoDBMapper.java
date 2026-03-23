/*
 * Copyright 2010-2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.dynamodb.datamodeling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Collections;
import java.util.Map;

/**
 * Object mapper for domain-object interaction with DynamoDB.
 * Port of the v1 DynamoDBMapper to work with the v2 DynamoDbClient.
 * Stripped to load() path for POC.
 */
public class DynamoDBMapper extends AbstractDynamoDBMapper {

    private final DynamoDbClient db;
    private final DynamoDBMapperModelFactory models;
    private final AttributeTransformer transformer;

    private static final Logger log = LoggerFactory.getLogger(DynamoDBMapper.class);

    public DynamoDBMapper(final DynamoDbClient dynamoDB) {
        this(dynamoDB, DynamoDBMapperConfig.DEFAULT, null);
    }

    public DynamoDBMapper(final DynamoDbClient dynamoDB, final DynamoDBMapperConfig config) {
        this(dynamoDB, config, null);
    }

    public DynamoDBMapper(
            final DynamoDbClient dynamoDB,
            final DynamoDBMapperConfig config,
            final AttributeTransformer transformer) {
        super(config);
        this.db = dynamoDB;
        this.transformer = transformer;
        this.models = StandardModelFactories.of();
    }

    @Override
    public <T> DynamoDBMapperTableModel<T> getTableModel(Class<T> clazz, DynamoDBMapperConfig config) {
        return this.models.getTableFactory(config).getTable(clazz);
    }

    @Override
    public <T> T load(T keyObject, DynamoDBMapperConfig config) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) keyObject.getClass();

        config = mergeConfig(config);
        final DynamoDBMapperTableModel<T> model = getTableModel(clazz, config);

        String tableName = getTableName(clazz, keyObject, config);

        Map<String, AttributeValue> key = model.convertKey(keyObject);

        GetItemRequest rq = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .consistentRead(config.getConsistentReads() == DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .build();

        GetItemResponse item = db.getItem(rq);
        Map<String, AttributeValue> itemAttributes = item.item();
        if (itemAttributes == null || itemAttributes.isEmpty()) {
            return null;
        }

        T object = privateMarshallIntoObject(
                toParameters(itemAttributes, clazz, tableName, config));

        return object;
    }

    @Override
    public <T> T load(Class<T> clazz, Object hashKey, Object rangeKey, DynamoDBMapperConfig config) {
        config = mergeConfig(config);
        final DynamoDBMapperTableModel<T> model = getTableModel(clazz, config);
        T keyObject = model.createKey(hashKey, rangeKey);
        return load(keyObject, config);
    }

    @Override
    public <T> T marshallIntoObject(Class<T> clazz, Map<String, AttributeValue> itemAttributes, DynamoDBMapperConfig config) {
        config = mergeConfig(config);
        String tableName = getTableName(clazz, config);
        return privateMarshallIntoObject(toParameters(itemAttributes, clazz, tableName, config));
    }

    /**
     * The one true implementation of marshallIntoObject.
     */
    private <T> T privateMarshallIntoObject(
            AttributeTransformer.Parameters<T> parameters) {

        Class<T> clazz = parameters.getModelClass();
        Map<String, AttributeValue> values = untransformAttributes(parameters);

        final DynamoDBMapperTableModel<T> model = getTableModel(clazz, parameters.getMapperConfig());
        return model.unconvert(values);
    }

    private <T> AttributeTransformer.Parameters<T> toParameters(
            final Map<String, AttributeValue> attributeValues,
            final Class<T> modelClass,
            final String tableName,
            final DynamoDBMapperConfig mapperConfig) {

        return new TransformerParameters<T>(
                getTableModel(modelClass, mapperConfig),
                attributeValues,
                false,
                modelClass,
                mapperConfig,
                tableName);
    }

    private static class TransformerParameters<T>
            implements AttributeTransformer.Parameters<T> {

        private final DynamoDBMapperTableModel<T> model;
        private final Map<String, AttributeValue> attributeValues;
        private final boolean partialUpdate;
        private final Class<T> modelClass;
        private final DynamoDBMapperConfig mapperConfig;
        private final String tableName;

        public TransformerParameters(
                final DynamoDBMapperTableModel<T> model,
                final Map<String, AttributeValue> attributeValues,
                final boolean partialUpdate,
                final Class<T> modelClass,
                final DynamoDBMapperConfig mapperConfig,
                final String tableName) {

            this.model = model;
            this.attributeValues = Collections.unmodifiableMap(attributeValues);
            this.partialUpdate = partialUpdate;
            this.modelClass = modelClass;
            this.mapperConfig = mapperConfig;
            this.tableName = tableName;
        }

        @Override
        public Map<String, AttributeValue> getAttributeValues() {
            return attributeValues;
        }

        @Override
        public boolean isPartialUpdate() {
            return partialUpdate;
        }

        @Override
        public Class<T> getModelClass() {
            return modelClass;
        }

        @Override
        public DynamoDBMapperConfig getMapperConfig() {
            return mapperConfig;
        }

        @Override
        public String getTableName() {
            return tableName;
        }

        @Override
        public String getHashKeyName() {
            return model.hashKey().name();
        }

        @Override
        public String getRangeKeyName() {
            return model.rangeKeyIfExists() == null ? null : model.rangeKey().name();
        }
    }

    private Map<String, AttributeValue> untransformAttributes(
            final AttributeTransformer.Parameters<?> parameters) {
        if (transformer != null) {
            return transformer.untransform(parameters);
        } else {
            return parameters.getAttributeValues();
        }
    }
}
