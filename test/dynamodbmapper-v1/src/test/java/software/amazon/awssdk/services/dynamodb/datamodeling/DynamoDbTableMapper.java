/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

/**
 * A wrapper for {@code DynamoDBMapper} which operates only on a specified
 * class/table.  All calls are forwarded to the underlying
 * {@code DynamoDBMapper} which was used to create this table mapper.
 *
 * A minimal example using get annotations,
 * <pre class="brush: java">
 * &#064;DynamoDBTable(tableName=&quot;TestTable&quot;)
 * public class TestClass {
 *     private Long key;
 *     private String rangeKey;
 *     private Double amount;
 *     private Long version;
 *
 *     &#064;DynamoDBHashKey
 *     public Long getKey() { return key; }
 *     public void setKey(Long key) { this.key = key; }
 *
 *     &#064;DynamoDBRangeKey
 *     public String getRangeKey() { return rangeKey; }
 *     public void setRangeKey(String rangeKey) { this.rangeKey = rangeKey; }
 *
 *     &#064;DynamoDBAttribute(attributeName=&quot;amount&quot;)
 *     public Double getAmount() { return amount; }
 *     public void setAmount(Double amount) { this.amount = amount; }
 *
 *     &#064;DynamoDBVersionAttribute
 *     public Long getVersion() { return version; }
 *     public void setVersion(Long version) { this.version = version; }
 * }
 * </pre>
 *
 * Initialize the DynamoDB mapper,
 * <pre class="brush: java">
 * DynamoDbClient dbClient = new AmazonDynamoDbClient();
 * DynamoDBMapper dbMapper = new DynamoDBMapper(dbClient);
 * </pre>
 *
 * Then, create a new table mapper with hash and range key,
 * <pre class="brush: java">
 * DynamoDBTableMapper&lt;TestClass,Long,String&gt; mapper = dbMapper.newTableMapper(TestClass.class);
 * </pre>
 *
 * Or, if the table does not have a range key,
 * <pre class="brush: java">
 * DynamoDBTableMapper&lt;TestClass,Long,?&gt; table = dbMapper.newTableMapper(TestClass.class);
 * </pre>
 *
 * If you don't have your DynamoDB table set up yet, you can use,
 * <pre class="brush: java">
 * table.createTableIfNotExists(new ProvisionedThroughput(25L, 25L));
 * </pre>
 *
 * Save instances of annotated classes and retrieve them,
 * <pre class="brush: java">
 * TestClass object = new TestClass();
 * object.setKey(1234L);
 * object.setRangeKey(&quot;ABCD&quot;);
 * object.setAmount(101D);
 *
 * try {
 *     table.saveIfNotExists(object);
 * } catch (ConditionalCheckFailedException e) {
 *     // handle already existing
 * }
 * </pre>
 *
 * Execute a query operation,
 * <pre class="brush: java">
 * int limit = 10;
 * List&lt;TestClass&gt; objects = new ArrayList&lt;TestClass&gt;(limit);
 *
 * DynamoDBQueryExpression&lt;TestClass&gt; query = new DynamoDBQueryExpression()
 *     .withRangeKeyCondition(table.rangeKey().name(), table.rangeKey().ge(&quot;ABAA&quot;))
 *     .withQueryFilterEntry(&quot;amount&quot;, table.field(&quot;amount&quot;).gt(100D))
 *     .withHashKeyValues(1234L)
 *     .withConsistentReads(true);
 *
 * QueryResponsePage&lt;TestClass&gt; results = new QueryResponsePage&lt;TestClass&gt;();
 *
 * do {
 *     if (results.lastEvaluatedKey() != null) {
 *         query.setExclusiveStartKey(results.lastEvaluatedKey());
 *     }
 *     query.setLimit(limit - objects.size());
 *     results = mapper.query(query);
 *     for (TestClass object : results.getResults()) {
 *         objects.add(object);
 *     }
 * } while (results.lastEvaluatedKey() != null &amp;&amp; objects.size() &lt; limit)
 * </pre>
 *
 * @param <T> The object type which this mapper operates.
 * @param <H> The hash key value type.
 * @param <R> The range key value type; use <code>?</code> if no range key.
 *
 * @see DynamoDbMapper
 * @see DynamoDbClient
 */
public final class DynamoDbTableMapper<T extends Object, H extends Object, R extends Object> {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTableMapper.class);

    private final DynamoDbMapperTableModel<T> model;
    private final DynamoDbMapperFieldModel<T, H> hk;
    private final DynamoDbMapperFieldModel<T, R> rk;
    private final DynamoDbMapperConfig config;
    private final DynamoDbMapper mapper;
    private final DynamoDbClient db;

    /**
     * Constructs a new table mapper for the given class.
     * @param model The field model factory.
     * @param mapper The DynamoDB mapper.
     * @param db The service object to use for all service calls.
     */
    protected DynamoDbTableMapper(DynamoDbClient db, DynamoDbMapper mapper, final DynamoDbMapperConfig config,
                                  final DynamoDbMapperTableModel<T> model) {
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
    public <V> DynamoDbMapperFieldModel<T, V> field(String attributeName) {
        return this.model.field(attributeName);
    }

    /**
     * Gets the hash key field model for the specified type.
     * @param <H> The hash key type.
     * @return The hash key field model.
     * @throws DynamoDbMappingException If the hash key is not present.
     */
    public DynamoDbMapperFieldModel<T, H> hashKey() {
        return this.model.hashKey();
    }

    /**
     * Gets the range key field model for the specified type.
     * @param <R> The range key type.
     * @return The range key field model.
     * @throws DynamoDbMappingException If the range key is not present.
     */
    public DynamoDbMapperFieldModel<T, R> rangeKey() {
        return this.model.rangeKey();
    }

    /**
     * Retrieves multiple items from the table using their primary keys.
     * @param itemsToGet The items to get.
     * @return The list of objects.
     * @see DynamoDbMapper#batchLoad
     */
    public List<T> batchLoad(Iterable<T> itemsToGet) {
        final Map<String, List<Object>> results = mapper.batchLoad(itemsToGet);
        if (results.isEmpty()) {
            return Collections.<T>emptyList();
        }
        return (List<T>) results.get(mapper.getTableName(model.targetType(), config));
    }

    /**
     * Saves the objects given using one or more calls to the batchWriteItem API.
     * @param objectsToSave The objects to save.
     * @return The list of failed batches.
     * @see DynamoDbMapper#batchSave
     */
    public List<DynamoDbMapper.FailedBatch> batchSave(Iterable<T> objectsToSave) {
        return mapper.batchWrite(objectsToSave, (Iterable<T>) Collections.<T>emptyList());
    }

    /**
     * Deletes the objects given using one or more calls to the batchWtiteItem API.
     * @param objectsToDelete The objects to delete.
     * @return The list of failed batches.
     * @see DynamoDbMapper#batchDelete
     */
    public List<DynamoDbMapper.FailedBatch> batchDelete(Iterable<T> objectsToDelete) {
        return mapper.batchWrite((Iterable<T>) Collections.<T>emptyList(), objectsToDelete);
    }

    /**
     * Saves and deletes the objects given using one or more calls to the
     * batchWriteItem API.
     * @param objectsToWrite The objects to write.
     * @param objectsToDelete The objects to delete.
     * @return The list of failed batches.
     * @see DynamoDbMapper#batchWrite
     */
    public List<DynamoDbMapper.FailedBatch> batchWrite(Iterable<T> objectsToWrite, Iterable<T> objectsToDelete) {
        return mapper.batchWrite(objectsToWrite, objectsToDelete);
    }

    /**
     * Loads an object with the hash key given.
     * @param hashKey The hash key value.
     * @return The object.
     * @see DynamoDbMapper#load
     */
    public T load(H hashKey) {
        return mapper.<T>load(model.targetType(), hashKey);
    }

    /**
     * Loads an object with the hash and range key.
     * @param hashKey The hash key value.
     * @param rangeKey The range key value.
     * @return The object.
     * @see DynamoDbMapper#load
     */
    public T load(H hashKey, R rangeKey) {
        return mapper.<T>load(model.targetType(), hashKey, rangeKey);
    }

    /**
     * Saves the object given into DynamoDB.
     * @param object The object to save.
     * @see DynamoDbMapper#save
     */
    public void save(T object) {
        mapper.<T>save(object);
    }

    /**
     * Saves the object given into DynamoDB using the specified saveExpression.
     * @param object The object to save.
     * @param saveExpression The save expression.
     * @see DynamoDbMapper#save
     */
    public void save(T object, DynamoDbSaveExpression saveExpression) {
        mapper.<T>save(object, saveExpression);
    }

    /**
     * Saves the object given into DynamoDB with the condition that the hash
     * and if applicable, the range key, does not already exist.
     * @param object The object to create.
     * @throws ConditionalCheckFailedException If the object exists.
     * @see DynamoDbMapper#save
     * @see DynamoDbSaveExpression
     * @see software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue
     */
    public void saveIfNotExists(T object) throws ConditionalCheckFailedException {
        final DynamoDbSaveExpression saveExpression = new DynamoDbSaveExpression();
        for (final DynamoDbMapperFieldModel<T, Object> key : model.keys()) {
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
     * @see DynamoDbMapper#save
     * @see DynamoDbSaveExpression
     * @see software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue
     */
    public void saveIfExists(T object) throws ConditionalCheckFailedException {
        final DynamoDbSaveExpression saveExpression = new DynamoDbSaveExpression();
        for (final DynamoDbMapperFieldModel<T, Object> key : model.keys()) {
            saveExpression.withExpectedEntry(key.name(), ExpectedAttributeValue.builder()
                    .exists(true).value(key.convert(key.get(object))).build());
        }
        mapper.<T>save(object, saveExpression);
    }

    /**
     * Deletes the given object from its DynamoDB table.
     * @param object The object to delete.
     * @see DynamoDbMapper#delete
     */
    public void delete(final T object) {
        mapper.delete(object);
    }

    /**
     * Deletes the given object from its DynamoDB table using the specified
     * deleteExpression.
     * @param object The object to delete.
     * @param deleteExpression The delete expression.
     * @see DynamoDbMapper#delete
     */
    public void delete(final T object, final DynamoDbDeleteExpression deleteExpression) {
        mapper.delete(object, deleteExpression);
    }

    /**
     * Deletes the given object from its DynamoDB table with the condition that
     * the hash and, if applicable, the range key, already exist.
     * @param object The object to delete.
     * @throws ConditionalCheckFailedException If the object does not exist.
     * @see DynamoDbMapper#delete
     * @see DynamoDbDeleteExpression
     * @see software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue
     */
    public void deleteIfExists(T object) throws ConditionalCheckFailedException {
        final DynamoDbDeleteExpression deleteExpression = new DynamoDbDeleteExpression();
        for (final DynamoDbMapperFieldModel<T, Object> key : model.keys()) {
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
     * @see DynamoDbMapper#count
     */
    public int count(DynamoDbQueryExpression<T> queryExpression) {
        return mapper.<T>count(model.targetType(), queryExpression);
    }

    /**
     * Queries an Amazon DynamoDB table and returns the matching results as an
     * unmodifiable list of instantiated objects.
     * @param queryExpression The query expression.
     * @return The query results.
     * @see DynamoDbMapper#query
     */
    public PaginatedQueryList<T> query(DynamoDbQueryExpression<T> queryExpression) {
        return mapper.<T>query(model.targetType(), queryExpression);
    }

    /**
     * Queries an Amazon DynamoDB table and returns a single page of matching
     * results.
     * @param queryExpression The query expression.
     * @return The query results.
     * @see DynamoDbMapper#query
     */
    public QueryResultPage<T> queryPage(DynamoDbQueryExpression<T> queryExpression) {
        return mapper.<T>queryPage(model.targetType(), queryExpression);
    }

    /**
     * Evaluates the specified scan expression and returns the count of matching
     * items, without returning any of the actual item data.
     * @param scanExpression The scan expression.
     * @return The count.
     * @see DynamoDbMapper#count
     */
    public int count(DynamoDbScanExpression scanExpression) {
        return mapper.count(model.targetType(), scanExpression);
    }

    /**
     * Scans through an Amazon DynamoDB table and returns the matching results
     * as an unmodifiable list of instantiated objects.
     * @param scanExpression The scan expression.
     * @return The scan results.
     * @see DynamoDbMapper#scan
     */
    public PaginatedScanList<T> scan(DynamoDbScanExpression scanExpression) {
        return mapper.<T>scan(model.targetType(), scanExpression);
    }

    /**
     * Scans through an Amazon DynamoDB table and returns a single page of
     * matching results.
     * @param scanExpression The scan expression.
     * @return The scan results.
     * @see DynamoDbMapper#scanPage
     */
    public ScanResultPage<T> scanPage(DynamoDbScanExpression scanExpression) {
        return mapper.<T>scanPage(model.targetType(), scanExpression);
    }

    /**
     * Scans through an Amazon DynamoDB table on logically partitioned segments
     * in parallel and returns the matching results in one unmodifiable list of
     * instantiated objects.
     * @param scanExpression The scan expression.
     * @param totalSegments The total segments.
     * @return The scan results.
     * @see DynamoDbMapper#parallelScan
     */
    public PaginatedParallelScanList<T> parallelScan(DynamoDbScanExpression scanExpression, int totalSegments) {
        return mapper.<T>parallelScan(model.targetType(), scanExpression, totalSegments);
    }

    /**
     * Returns information about the table, including the current status of the
     * table, when it was created, the primary key schema, and any indexes on
     * the table.
     * @return The describe table results.
     * @see DynamoDbClient#describeTable
     */
    public TableDescription describeTable() {
        return db.describeTable(DescribeTableRequest.builder()
                .tableName(mapper.getTableName(model.targetType(), config))
                .build())
                .table();
    }

    /**
     * Creates the table with the specified throughput; also populates the same
     * throughput for all global secondary indexes.
     * @param throughput The provisioned throughput.
     * @return The table decription.
     * @see DynamoDbClient#createTable
     * @see software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
     */
    public TableDescription createTable(ProvisionedThroughput throughput) {
        CreateTableRequest request = mapper.generateCreateTableRequest(model.targetType());
        CreateTableRequest.Builder modified = request.toBuilder()
                .provisionedThroughput(throughput);
        if (request.globalSecondaryIndexes() != null) {
            modified.globalSecondaryIndexes((Collection<GlobalSecondaryIndex>) null);
            for (GlobalSecondaryIndex gsi : request.globalSecondaryIndexes()) {
                gsi = gsi.toBuilder().provisionedThroughput(throughput).build();
                modified.globalSecondaryIndexes(gsi);
            }
        }
        request = modified.build();
        return db.createTable(request).tableDescription();
    }

    /**
     * Creates the table and ignores the {@code ResourceInUseException} if it
     * ialready exists.
     * @param throughput The provisioned throughput.
     * @return True if created, or false if the table already existed.
     * @see DynamoDbClient#createTable
     * @see software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
     */
    public boolean createTableIfNotExists(ProvisionedThroughput throughput) {
        try {
            createTable(throughput);
        } catch (final ResourceInUseException e) {
            if (log.isTraceEnabled()) {
                log.trace("Table already exists, no need to create", e);
            }
            return false;
        }
        return true;
    }

    /**
     * Deletes the table.
     * @return The table decription.
     * @see DynamoDbClient#deleteTable
     * @see software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest
     */
    public TableDescription deleteTable() {
        return db.deleteTable(
                mapper.generateDeleteTableRequest(model.targetType())
                             ).tableDescription();
    }

    /**
     * Deletes the table and ignores the {@code ResourceNotFoundException} if
     * it does not already exist.
     * @return True if the table was deleted, or false if the table did not exist.
     * @see DynamoDbClient#deleteTable
     * @see software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest
     */
    public boolean deleteTableIfExists() {
        try {
            deleteTable();
        } catch (final ResourceNotFoundException e) {
            if (log.isTraceEnabled()) {
                log.trace("Table does not exist, no need to delete", e);
            }
            return false;
        }
        return true;
    }

}
