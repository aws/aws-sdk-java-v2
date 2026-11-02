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

package software.amazon.awssdk.mapper.dynamodb;

import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBDeleteExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBSaveExpression;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A wrapper for {@code DynamoDBMapper} which operates only on a specified
 * class/table.  All calls are forwarded to the underlying
 * {@code DynamoDBMapper} which was used to create this table mapper.
 *
 * A minimal example using get annotations,
 * {@snippet :
 * @DynamoDBTable(tableName="TestTable")
 * public class TestClass {
 *     private Long key;
 *     private String rangeKey;
 *     private Double amount;
 *     private Long version;
 *
 *     @DynamoDBHashKey
 *     public Long getKey() { return key; }
 *     public void setKey(Long key) { this.key = key; }
 *
 *     @DynamoDBRangeKey
 *     public String getRangeKey() { return rangeKey; }
 *     public void setRangeKey(String rangeKey) { this.rangeKey = rangeKey; }
 *
 *     @DynamoDBAttribute(attributeName="amount")
 *     public Double getAmount() { return amount; }
 *     public void setAmount(Double amount) { this.amount = amount; }
 *
 *     @DynamoDBVersionAttribute
 *     public Long getVersion() { return version; }
 *     public void setVersion(Long version) { this.version = version; }
 * }
 * }
 *
 * Initialize the DynamoDB mapper,
 * {@snippet :
 * DynamoDbClient dbClient = DynamoDbClient.create();
 * DynamoDBMapper dbMapper = DynamoDBMapper.builder().dynamoDbClient(dbClient).build();
 * }
 *
 * Then, create a new table mapper with hash and range key,
 * {@snippet :
 * DynamoDBTableMapper<TestClass,Long,String> mapper = dbMapper.newTableMapper(TestClass.class);
 * }
 *
 * Or, if the table does not have a range key,
 * {@snippet :
 * DynamoDBTableMapper<TestClass,Long,?> table = dbMapper.newTableMapper(TestClass.class);
 * }
 *
 * Save instances of annotated classes and retrieve them,
 * {@snippet :
 * TestClass object = new TestClass();
 * object.setKey(1234L);
 * object.setRangeKey("ABCD");
 * object.setAmount(101D);
 *
 * try {
 *     mapper.saveIfNotExists(object);
 * } catch (ConditionalCheckFailedException e) {
 *     // handle already existing
 * }
 * }
 *
 * Execute a query operation,
 * {@snippet :
 * int limit = 10;
 * List<TestClass> objects = new ArrayList<>(limit);
 *
 * // The hash key value is supplied as an object with only its key set
 * TestClass hashKeyValues = new TestClass();
 * hashKeyValues.setKey(1234L);
 *
 * DynamoDBQueryExpression<TestClass> query = new DynamoDBQueryExpression<TestClass>()
 *     .withHashKeyValues(hashKeyValues)
 *     .withRangeKeyCondition(mapper.rangeKey().name(), mapper.rangeKey().ge("ABAA"))
 *     .withQueryFilterEntry("amount", mapper.field("amount").gt(100D))
 *     .withConsistentRead(true);
 *
 * QueryResultPage<TestClass> results = new QueryResultPage<>();
 *
 * do {
 *     if (results.getLastEvaluatedKey() != null) {
 *         query.setExclusiveStartKey(results.getLastEvaluatedKey());
 *     }
 *     query.setLimit(limit - objects.size());
 *     results = mapper.queryPage(query);
 *     objects.addAll(results.getResults());
 * } while (results.getLastEvaluatedKey() != null && objects.size() < limit);
 * }
 *
 * @param <T> The object type which this mapper operates.
 * @param <H> The hash key value type.
 * @param <R> The range key value type; use <code>?</code> if no range key.
 *
 * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper
 * @see DynamoDbClient
 */
@SdkPublicApi
@SdkPreviewApi
public final class DynamoDBTableMapper<T extends Object, H extends Object, R extends Object> {

    private final DynamoDBMapperTableModel<T> model;
    private final DynamoDBMapperFieldModel<T,H> hk;
    private final DynamoDBMapperFieldModel<T,R> rk;
    private final DynamoDBMapperConfig config;
    private final DynamoDBMapper mapper;
    private final DynamoDbClient db;

    /**
     * Constructs a new table mapper for the given class.
     * @param model The field model factory.
     * @param mapper The DynamoDB mapper.
     * @param db The service object to use for all service calls.
     */
    protected DynamoDBTableMapper(DynamoDbClient db, DynamoDBMapper mapper, final DynamoDBMapperConfig config, final DynamoDBMapperTableModel<T> model) {
        this.rk = model.rangeKeyIfExists();
        this.hk = model.hashKey();
        this.model = model;
        this.config = config;
        this.mapper = mapper;
        this.db = db;
    }

    /**
     * Gets the field model for a given attribute.
     * @param <V> The field model's value type.
     * @param attributeName The attribute name.
     * @return The field model.
     */
    public <V> DynamoDBMapperFieldModel<T,V> field(String attributeName) {
        return this.model.field(attributeName);
    }

    /**
     * Gets the hash key field model for the specified type.
     * @return The hash key field model.
     * @throws DynamoDBMappingException If the hash key is not present.
     */
    public DynamoDBMapperFieldModel<T,H> hashKey() {
        return this.model.hashKey();
    }

    /**
     * Gets the range key field model for the specified type.
     * @return The range key field model.
     * @throws DynamoDBMappingException If the range key is not present.
     */
    public DynamoDBMapperFieldModel<T,R> rangeKey() {
        return this.model.rangeKey();
    }

    /**
     * Retrieves multiple items from the table using their primary keys.
     * @param itemsToGet The items to get.
     * @return The list of objects.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#batchLoad
     */
    public List<T> batchLoad(Iterable<T> itemsToGet) {
        Map<String,List<Object>> results = mapper.batchLoad(itemsToGet);
        if (results.isEmpty()) {
            return Collections.<T>emptyList();
        }
        return (List<T>)results.get(mapper.getTableName(model.targetType(), config));
    }

    /**
     * Saves the objects given using one or more calls to the batchWriteItem API.
     * @param objectsToSave The objects to save.
     * @return The list of failed batches.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#batchSave
     */
    public List<DynamoDBMapper.FailedBatch> batchSave(Iterable<T> objectsToSave) {
        return mapper.batchWrite(objectsToSave, (Iterable<T>)Collections.<T>emptyList());
    }

    /**
     * Deletes the objects given using one or more calls to the batchWtiteItem API.
     * @param objectsToDelete The objects to delete.
     * @return The list of failed batches.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#batchDelete
     */
    public List<DynamoDBMapper.FailedBatch> batchDelete(Iterable<T> objectsToDelete) {
        return mapper.batchWrite((Iterable<T>)Collections.<T>emptyList(), objectsToDelete);
    }

    /**
     * Saves and deletes the objects given using one or more calls to the
     * batchWriteItem API.
     * @param objectsToWrite The objects to write.
     * @param objectsToDelete The objects to delete.
     * @return The list of failed batches.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#batchWrite
     */
    public List<DynamoDBMapper.FailedBatch> batchWrite(Iterable<T> objectsToWrite, Iterable<T> objectsToDelete) {
        return mapper.batchWrite(objectsToWrite, objectsToDelete);
    }

    /**
     * Transactionally writes objects specified by transactionWriteRequest by calling
     * {@link software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#transactionWrite(TransactionWriteRequest)} API.
     * @param transactionWriteRequest List of objects to write
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#transactionWrite(TransactionWriteRequest)
     */
    public void transactionWrite(TransactionWriteRequest transactionWriteRequest) {
        for (TransactionWriteRequest.TransactionWriteOperation transactionWriteOperation : transactionWriteRequest.getTransactionWriteOperations()) {
            if (!model.targetType().equals(transactionWriteOperation.getObject().getClass())) {
                throw new DynamoDBMappingException("Input object is of the classType: " + transactionWriteOperation.getObject().getClass()
                                                           + " but tableMapper is declared with classType: " + model.targetType());
            }
        }
        mapper.transactionWrite(transactionWriteRequest);
    }

    /**
     * Transactionally loads objects specified by transactionLoadRequest by calling
     * {@link software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#transactionLoad(TransactionLoadRequest)} API.
     * @param transactionLoadRequest List of objects to load
     * @return List of objects
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#transactionLoad(TransactionLoadRequest)
     */
    public List<Object> transactionLoad(TransactionLoadRequest transactionLoadRequest) {
        for (Object object : transactionLoadRequest.getObjectsToLoad() ) {
            if (!model.targetType().equals(object.getClass())) {
                throw new DynamoDBMappingException("Input object is of the classType: " + object.getClass() + " but tableMapper is declared with classType: " + model.targetType());
            }
        }
        return mapper.transactionLoad(transactionLoadRequest);
    }

    /**
     * Loads an object with the hash key given.
     * @param hashKey The hash key value.
     * @return The object.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#load
     */
    public T load(H hashKey) {
        return mapper.<T>load(model.targetType(), hashKey);
    }

    /**
     * Loads an object with the hash and range key.
     * @param hashKey The hash key value.
     * @param rangeKey The range key value.
     * @return The object.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#load
     */
    public T load(H hashKey, R rangeKey) {
        return mapper.<T>load(model.targetType(), hashKey, rangeKey);
    }

    /**
     * Saves the object given into DynamoDB.
     * @param object The object to save.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#save
     */
    public void save(T object) {
        mapper.<T>save(object);
    }

    /**
     * Saves the object given into DynamoDB using the specified saveExpression.
     * @param object The object to save.
     * @param saveExpression The save expression.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#save
     */
    public void save(T object, DynamoDBSaveExpression saveExpression) {
        mapper.<T>save(object, saveExpression);
    }

    /**
     * Saves the object given into DynamoDB with the condition that the hash
     * and if applicable, the range key, does not already exist.
     * @param object The object to create.
     * @throws ConditionalCheckFailedException If the object exists.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#save
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBSaveExpression
     * @see ExpectedAttributeValue
     */
    public void saveIfNotExists(T object) throws ConditionalCheckFailedException {
        DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
        for (DynamoDBMapperFieldModel<T,Object> key : model.keys()) {
            saveExpression.withExpectedEntry(key.name(), ExpectedAttributeValue.builder()
                .exists(false).build());
        }
        mapper.<T>save(object, saveExpression);
    }

    /**
     * Saves the object given into DynamoDB with the condition that the hash
     * and, if applicable, the range key, already exist.
     * @param object The object to update.
     * @throws ConditionalCheckFailedException If the object does not exist.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#save
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBSaveExpression
     * @see ExpectedAttributeValue
     */
    public void saveIfExists(T object) throws ConditionalCheckFailedException {
        DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
        for (DynamoDBMapperFieldModel<T,Object> key : model.keys()) {
            saveExpression.withExpectedEntry(key.name(), ExpectedAttributeValue.builder()
                .exists(true).value(key.convert(key.get(object))).build());
        }
        mapper.<T>save(object, saveExpression);
    }

    /**
     * Deletes the given object from its DynamoDB table.
     * @param object The object to delete.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#delete
     */
    public final void delete(final T object) {
        mapper.delete(object);
    }

    /**
     * Deletes the given object from its DynamoDB table using the specified
     * deleteExpression.
     * @param object The object to delete.
     * @param deleteExpression The delete expression.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#delete
     */
    public final void delete(final T object, final DynamoDBDeleteExpression deleteExpression) {
        mapper.delete(object, deleteExpression);
    }

    /**
     * Deletes the given object from its DynamoDB table with the condition that
     * the hash and, if applicable, the range key, already exist.
     * @param object The object to delete.
     * @throws ConditionalCheckFailedException If the object does not exist.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#delete
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBDeleteExpression
     * @see ExpectedAttributeValue
     */
    public void deleteIfExists(T object) throws ConditionalCheckFailedException {
        DynamoDBDeleteExpression deleteExpression = new DynamoDBDeleteExpression();
        for (DynamoDBMapperFieldModel<T,Object> key : model.keys()) {
            deleteExpression.withExpectedEntry(key.name(), ExpectedAttributeValue.builder()
                .exists(true).value(key.convert(key.get(object))).build());
        }
        mapper.delete(object, deleteExpression);
    }

    /**
     * Evaluates the specified query expression and returns the count of matching
     * items, without returning any of the actual item data
     * @param queryExpression The query expression.
     * @return The count.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#count
     */
    public int count(DynamoDBQueryExpression<T> queryExpression) {
        return mapper.<T>count(model.targetType(), queryExpression);
    }

    /**
     * Queries an Amazon DynamoDB table and returns the matching results as an
     * unmodifiable list of instantiated objects.
     * @param queryExpression The query expression.
     * @return The query results.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#query
     */
    public PaginatedQueryList<T> query(DynamoDBQueryExpression<T> queryExpression) {
        return mapper.<T>query(model.targetType(), queryExpression);
    }

    /**
     * Queries an Amazon DynamoDB table and returns a single page of matching
     * results.
     * @param queryExpression The query expression.
     * @return The query results.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#query
     */
    public QueryResultPage<T> queryPage(DynamoDBQueryExpression<T> queryExpression) {
        return mapper.<T>queryPage(model.targetType(), queryExpression);
    }

    /**
     * Evaluates the specified scan expression and returns the count of matching
     * items, without returning any of the actual item data.
     * @param scanExpression The scan expression.
     * @return The count.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#count
     */
    public int count(DynamoDBScanExpression scanExpression) {
        return mapper.count(model.targetType(), scanExpression);
    }

    /**
     * Scans through an Amazon DynamoDB table and returns the matching results
     * as an unmodifiable list of instantiated objects.
     * @param scanExpression The scan expression.
     * @return The scan results.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#scan
     */
    public PaginatedScanList<T> scan(DynamoDBScanExpression scanExpression) {
        return mapper.<T>scan(model.targetType(), scanExpression);
    }

    /**
     * Scans through an Amazon DynamoDB table and returns a single page of
     * matching results.
     * @param scanExpression The scan expression.
     * @return The scan results.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#scanPage
     */
    public ScanResultPage<T> scanPage(DynamoDBScanExpression scanExpression) {
        return mapper.<T>scanPage(model.targetType(), scanExpression);
    }

    /**
     * Scans through an Amazon DynamoDB table on logically partitioned segments
     * in parallel and returns the matching results in one unmodifiable list of
     * instantiated objects.
     * @param scanExpression The scan expression.
     * @param totalSegments The total segments.
     * @return The scan results.
     * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper#parallelScan
     */
    public PaginatedParallelScanList<T> parallelScan(DynamoDBScanExpression scanExpression, int totalSegments) {
        return mapper.<T>parallelScan(model.targetType(), scanExpression, totalSegments);
    }

}
