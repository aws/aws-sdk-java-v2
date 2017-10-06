/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper.FailedBatch;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

/**
 * Abstract implementation of {@code IDynamoDBMapper}. Convenient method forms pass through to the
 * corresponding overload that takes a request object, which throws an
 * {@code UnsupportedOperationException}.
 */
public class AbstractDynamoDbMapper implements IDynamoDbMapper {

    private final DynamoDbMapperConfig config;

    protected AbstractDynamoDbMapper(final DynamoDbMapperConfig defaults) {
        this.config = DynamoDbMapperConfig.DEFAULT.merge(defaults);
    }

    protected AbstractDynamoDbMapper() {
        this(DynamoDbMapperConfig.DEFAULT);
    }

    protected final String getTableName(Class<?> clazz, Object object, DynamoDbMapperConfig config) {
        if (object != null && config.getObjectTableNameResolver() != null) {
            return config.getObjectTableNameResolver().getTableName(object, config);
        }
        return getTableName(clazz, config);
    }

    protected final String getTableName(Class<?> clazz, DynamoDbMapperConfig config) {
        if (config.getTableNameResolver() == null) {
            return DynamoDbMapperConfig.DefaultTableNameResolver.INSTANCE.getTableName(clazz, config);
        }
        return config.getTableNameResolver().getTableName(clazz, config);
    }

    protected final DynamoDbMapperConfig mergeConfig(DynamoDbMapperConfig overrides) {
        return this.config.merge(overrides);
    }

    @Override
    public <T extends Object> DynamoDbMapperTableModel<T> getTableModel(Class<T> clazz) {
        return getTableModel(clazz, config);
    }

    @Override
    public <T extends Object> DynamoDbMapperTableModel<T> getTableModel(Class<T> clazz, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> T load(Class<T> clazz, Object hashKey, DynamoDbMapperConfig config) {
        return load(clazz, hashKey, (Object) null, config);
    }

    @Override
    public <T> T load(Class<T> clazz, Object hashKey) {
        return load(clazz, hashKey, (Object) null, config);
    }

    @Override
    public <T> T load(Class<T> clazz, Object hashKey, Object rangeKey) {
        return load(clazz, hashKey, rangeKey, config);
    }

    @Override
    public <T> T load(Class<T> clazz, Object hashKey, Object rangeKey, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> T load(T keyObject) {
        return load(keyObject, config);
    }

    @Override
    public <T> T load(T keyObject, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> T marshallIntoObject(Class<T> clazz, Map<String, AttributeValue> itemAttributes) {
        return marshallIntoObject(clazz, itemAttributes, config);
    }

    public <T> T marshallIntoObject(Class<T> clazz, Map<String, AttributeValue> itemAttributes, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> List<T> marshallIntoObjects(Class<T> clazz, List<Map<String, AttributeValue>> itemAttributes) {
        return marshallIntoObjects(clazz, itemAttributes, config);
    }

    public <T> List<T> marshallIntoObjects(Class<T> clazz, List<Map<String, AttributeValue>> itemAttributes,
                                           DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> void save(T object) {
        save(object, (DynamoDbSaveExpression) null, config);
    }

    @Override
    public <T> void save(T object, DynamoDbSaveExpression saveExpression) {
        save(object, saveExpression, config);
    }

    @Override
    public <T> void save(T object, DynamoDbMapperConfig config) {
        save(object, (DynamoDbSaveExpression) null, config);
    }

    @Override
    public <T> void save(T object, DynamoDbSaveExpression saveExpression, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public void delete(Object object) {
        delete(object, (DynamoDbDeleteExpression) null, config);
    }

    @Override
    public void delete(Object object, DynamoDbDeleteExpression deleteExpression) {
        delete(object, deleteExpression, config);
    }

    @Override
    public void delete(Object object, DynamoDbMapperConfig config) {
        delete(object, (DynamoDbDeleteExpression) null, config);
    }

    @Override
    public <T> void delete(T object, DynamoDbDeleteExpression deleteExpression, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public List<FailedBatch> batchDelete(Iterable<? extends Object> objectsToDelete) {
        return batchWrite(Collections.emptyList(), objectsToDelete, config);
    }

    @Override
    public List<FailedBatch> batchDelete(Object... objectsToDelete) {
        return batchWrite(Collections.emptyList(), Arrays.asList(objectsToDelete), config);
    }

    @Override
    public List<FailedBatch> batchSave(Iterable<? extends Object> objectsToSave) {
        return batchWrite(objectsToSave, Collections.emptyList(), config);
    }

    @Override
    public List<FailedBatch> batchSave(Object... objectsToSave) {
        return batchWrite(Arrays.asList(objectsToSave), Collections.emptyList(), config);
    }

    @Override
    public List<FailedBatch> batchWrite(Iterable<? extends Object> objectsToWrite,
                                        Iterable<? extends Object> objectsToDelete) {
        return batchWrite(objectsToWrite, objectsToDelete, config);
    }

    @Override
    public List<FailedBatch> batchWrite(Iterable<? extends Object> objectsToWrite,
                                        Iterable<? extends Object> objectsToDelete,
                                        DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public Map<String, List<Object>> batchLoad(Iterable<? extends Object> itemsToGet) {
        return batchLoad(itemsToGet, config);
    }

    @Override
    public Map<String, List<Object>> batchLoad(Iterable<? extends Object> itemsToGet, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public Map<String, List<Object>> batchLoad(Map<Class<?>, List<KeyPair>> itemsToGet) {
        return batchLoad(itemsToGet, config);
    }

    @Override
    public Map<String, List<Object>> batchLoad(Map<Class<?>, List<KeyPair>> itemsToGet, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> PaginatedScanList<T> scan(Class<T> clazz, DynamoDbScanExpression scanExpression) {
        return scan(clazz, scanExpression, config);
    }

    @Override
    public <T> PaginatedScanList<T> scan(Class<T> clazz,
                                         DynamoDbScanExpression scanExpression,
                                         DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> PaginatedParallelScanList<T> parallelScan(Class<T> clazz,
                                                         DynamoDbScanExpression scanExpression,
                                                         int totalSegments) {
        return parallelScan(clazz, scanExpression, totalSegments, config);
    }

    @Override
    public <T> PaginatedParallelScanList<T> parallelScan(Class<T> clazz,
                                                         DynamoDbScanExpression scanExpression,
                                                         int totalSegments,
                                                         DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> ScanResultPage<T> scanPage(Class<T> clazz, DynamoDbScanExpression scanExpression) {
        return scanPage(clazz, scanExpression, config);
    }

    @Override
    public <T> ScanResultPage<T> scanPage(Class<T> clazz,
                                          DynamoDbScanExpression scanExpression,
                                          DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public int count(Class<?> clazz, DynamoDbScanExpression scanExpression) {
        return count(clazz, scanExpression, config);
    }

    @Override
    public int count(Class<?> clazz, DynamoDbScanExpression scanExpression, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> int count(Class<T> clazz, DynamoDbQueryExpression<T> queryExpression) {
        return count(clazz, queryExpression, config);
    }

    @Override
    public <T> int count(Class<T> clazz, DynamoDbQueryExpression<T> queryExpression, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> PaginatedQueryList<T> query(Class<T> clazz, DynamoDbQueryExpression<T> queryExpression) {
        return query(clazz, queryExpression, config);
    }

    @Override
    public <T> PaginatedQueryList<T> query(Class<T> clazz,
                                           DynamoDbQueryExpression<T> queryExpression,
                                           DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public <T> QueryResultPage<T> queryPage(Class<T> clazz, DynamoDbQueryExpression<T> queryExpression) {
        return queryPage(clazz, queryExpression, config);
    }

    @Override
    public <T> QueryResultPage<T> queryPage(Class<T> clazz,
                                            DynamoDbQueryExpression<T> queryExpression,
                                            DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public S3ClientCache s3ClientCache() {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public S3Link createS3Link(String bucketName, String key) {
        return createS3Link((Region) null, bucketName, key);
    }

    @Override
    public S3Link createS3Link(Region s3region, String bucketName, String key) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public S3Link createS3Link(String s3region, String bucketName, String key) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public CreateTableRequest generateCreateTableRequest(Class<?> clazz) {
        return generateCreateTableRequest(clazz, config);
    }

    public <T> CreateTableRequest generateCreateTableRequest(Class<T> clazz, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

    @Override
    public DeleteTableRequest generateDeleteTableRequest(Class<?> clazz) {
        return generateDeleteTableRequest(clazz, config);
    }

    public <T> DeleteTableRequest generateDeleteTableRequest(Class<T> clazz, DynamoDbMapperConfig config) {
        throw new UnsupportedOperationException("operation not supported in " + getClass());
    }

}
