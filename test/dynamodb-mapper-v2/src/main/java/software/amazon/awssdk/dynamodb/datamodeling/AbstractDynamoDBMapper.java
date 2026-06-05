/*
 * Copyright 2015-2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Map;

/**
 * Abstract implementation of {@code IDynamoDBMapper}. Stripped to load path for POC.
 */
public abstract class AbstractDynamoDBMapper implements IDynamoDBMapper {

    private final DynamoDBMapperConfig config;

    protected AbstractDynamoDBMapper(final DynamoDBMapperConfig defaults) {
        this.config = DynamoDBMapperConfig.DEFAULT.merge(defaults);
    }

    protected AbstractDynamoDBMapper() {
        this(DynamoDBMapperConfig.DEFAULT);
    }

    protected final String getTableName(Class<?> clazz, Object object, DynamoDBMapperConfig config) {
        if (config.getObjectTableNameResolver() != null && object != null) {
            return config.getObjectTableNameResolver().getTableName(object, config);
        }
        return getTableName(clazz, config);
    }

    protected final String getTableName(Class<?> clazz, DynamoDBMapperConfig config) {
        if (config.getTableNameResolver() == null) {
            return DynamoDBMapperConfig.DefaultTableNameResolver.INSTANCE.getTableName(clazz, config);
        }
        return config.getTableNameResolver().getTableName(clazz, config);
    }

    protected final DynamoDBMapperConfig mergeConfig(DynamoDBMapperConfig overrides) {
        return this.config.merge(overrides);
    }

    @Override
    public <T> DynamoDBMapperTableModel<T> getTableModel(Class<T> clazz) {
        return getTableModel(clazz, config);
    }

    @Override
    public <T> DynamoDBMapperTableModel<T> getTableModel(Class<T> clazz, DynamoDBMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> T load(Class<T> clazz, Object hashKey, DynamoDBMapperConfig config) {
        return load(clazz, hashKey, (Object)null, config);
    }

    @Override
    public <T> T load(Class<T> clazz, Object hashKey) {
        return load(clazz, hashKey, (Object)null, config);
    }

    @Override
    public <T> T load(Class<T> clazz, Object hashKey, Object rangeKey) {
        return load(clazz, hashKey, rangeKey, config);
    }

    @Override
    public <T> T load(Class<T> clazz, Object hashKey, Object rangeKey, DynamoDBMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> T load(T keyObject) {
        return load(keyObject, config);
    }

    @Override
    public <T> T load(T keyObject, DynamoDBMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> T marshallIntoObject(Class<T> clazz, Map<String, AttributeValue> itemAttributes) {
        return marshallIntoObject(clazz, itemAttributes, config);
    }

    public <T> T marshallIntoObject(Class<T> clazz, Map<String, AttributeValue> itemAttributes, DynamoDBMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }
}
