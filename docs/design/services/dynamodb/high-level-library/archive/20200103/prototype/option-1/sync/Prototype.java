/**
 * The entry-point for all DynamoDB client creation. All Java Dynamo features will be accessible through this single class. This
 * enables easy access to the different abstractions that the AWS SDK for Java provides (in exchange for a bigger JAR size).
 *
 * <p>
 *     <b>Maven Module Location</b>
 *     This would be in a separate maven module (software.amazon.awssdk:dynamodb-all) that depends on all other DynamoDB modules
 *     (software.amazon.awssdk:dynamodb, software.amazon.awssdk:dynamodb-document). Customers that only want one specific client
 *     could instead depend directly on the module that contains it.
 * </p>
 */
@ThreadSafe
public interface DynamoDb {
    /**
     * Create a low-level DynamoDB client with default configuration. Equivalent to DynamoDbClient.create().
     * Already GA in module software.amazon.awssdk:dynamodb.
     */
    DynamoDbClient client();

    /**
     * Create a low-level DynamoDB client builder. Equivalent to DynamoDbClient.builder().
     * Already GA in module software.amazon.awssdk:dynamodb.
     */
    DynamoDbClientBuilder clientBuilder();

    /**
     * Create a high-level "document" DynamoDB client with default configuration.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         client.listTables().tables().forEach(System.out::println);
     *     }
     * </code>
     *
     * @see DynamoDbDocumentClient
     */
    DynamoDbDocumentClient documentClient();

    /**
     * Create a high-level "document" DynamoDB client builder that can configure and create high-level "document" DynamoDB
     * clients.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbClient lowLevelClient = DynamoDb.client();
     *          DynamoDbDocumentClient client = DynamoDb.documentClientBuilder()
     *                                                  .dynamoDbClient(lowLevelClient)
     *                                                  .build()) {
     *         client.listTables().tables().forEach(System.out::println);
     *     }
     * </code>
     *
     * @see DynamoDbDocumentClient.Builder
     */
    DynamoDbDocumentClient.Builder documentClientBuilder();
}

/**
 * A synchronous client for interacting with DynamoDB. While the low-level {@link DynamoDbClient} is generated from a service
 * model, this client is hand-written and provides a richer client experience for DynamoDB.
 *
 * Features:
 * <ol>
 *     <li>Representations of DynamoDB resources, like {@link Table}s and {@link Item}s.</li>
 *     <li>Support for Java-specific types, like {@link Instant} and {@link BigDecimal}.</li>
 *     <li>Support for reading and writing custom objects (eg. Java Beans, POJOs).</li>
 * </ol>
 *
 * All {@link DynamoDbDocumentClient}s should be closed via {@link #close()}.
 */
@ThreadSafe
public interface DynamoDbDocumentClient extends SdkAutoCloseable {
    /**
     * Create a {@link DynamoDbDocumentClient} with default configuration.
     *
     * Equivalent statements:
     * <ol>
     *     <li>{@code DynamoDb.documentClient()}</li>
     *     <li>{@code DynamoDb.documentClientBuilder().build()}</li>
     *     <li>{@code DynamoDbDocumentClient.builder().build()}</li>
     * </ol>
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDbDocumentClient.create()) {
     *         client.listTables().table().forEach(System.out::println);
     *     }
     * </code>
     */
    static DynamoDbDocumentClient create();

    /**
     * Create a {@link DynamoDbDocumentClient.Builder} that can be used to create a {@link DynamoDbDocumentClient} with custom
     * configuration.
     *
     * Equivalent to {@code DynamoDb.documentClientBuilder()}.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbClient lowLevelClient = DynamoDbClient.create();
     *          DynamoDbDocumentClient client = DynamoDbDocumentClient.builder()
     *                                                                .dynamoDbClient(lowLevelClient)
     *                                                                .build()) {
     *         client.listTables().tables().forEach(System.out::println);
     *     }
     * </code>
     */
    static DynamoDbDocumentClient.Builder builder();

    /**
     * Create a Dynamo DB table that does not already exist. If the table exists already, use {@link #getTable(String)}.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         ProvisionedCapacity tableCapacity = ProvisionedCapacity.builder()
     *                                                                .readCapacity(5)
     *                                                                .writeCapacity(5)
     *                                                                .build();
     *
     *         KeySchema tableKeys = KeySchema.builder()
     *                                        .putKey("partition-key", ItemAttributeIndexType.PARTITION_KEY)
     *                                        .build();
     *
     *         client.createTable(CreateTableRequest.builder()
     *                                              .tableName("my-table")
     *                                              .provisionedCapacity(tableCapacity)
     *                                              .keySchema(tableKeys)
     *                                              .build());
     *
     *         System.out.println("Table created successfully.");
     *     } catch (TableAlreadyExistsException e) {
     *         System.out.println("Table creation failed.");
     *     }
     * </code>
     */
    CreateTableResponse createTable(CreateTableRequest createTableRequest)
            throws TableAlreadyExistsException;

    /**
     * Get a specific DynamoDB table, based on its table name. If the table does not exist, use {@link #createTable}.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         Table table = client.getTable("my-table");
     *         System.out.println(table);
     *     } catch (NoSuchTableException e) {
     *         System.out.println("Table does not exist.");
     *     }
     * </code>
     */
    Table getTable(String tableName)
            throws NoSuchTableException;

    /**
     * Get a lazily-populated iterable over all DynamoDB tables on the current account and region.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         String tables = client.listTables().tables().stream()
     *                               .map(Table::name)
     *                               .collect(Collectors.joining(","));
     *         System.out.println("Current Tables: " + tables);
     *     }
     * </code>
     */
    ListTablesResponse listTables();

    /**
     * The builder for the high-level DynamoDB client. This is used by customers to configure the high-level client with default
     * values to be applied across all client operations.
     *
     * This can be created via {@link DynamoDb#documentClientBuilder()} or {@link DynamoDbDocumentClient#builder()}.
     */
    interface Builder {
        /**
         * Configure the DynamoDB document client with a low-level DynamoDB client.
         *
         * Default: {@code DynamoDbClient.create()}
         */
        DynamoDbDocumentClient.Builder dynamoDbClient(DynamoDbClient client);

        /**
         * Configure the DynamoDB document client with a specific set of configuration values that override the defaults.
         *
         * Default: {@code DocumentClientConfiguration.create()}
         */
        DynamoDbDocumentClient.Builder documentClientConfiguration(DocumentClientConfiguration configuration);

        /**
         * Create a DynamoDB document client with all of the configured values.
         */
        DynamoDbDocumentClient build();
    }
}

/**
 * Configuration for a {@link DynamoDbDocumentClient}. This specific configuration is applied globally across all tables created
 * by a client builder.
 *
 * @see DynamoDbDocumentClient.Builder#documentClientConfiguration(DocumentClientConfiguration)
 */
@ThreadSafe
public interface DocumentClientConfiguration {
    /**
     * Create document client configuration with default values.
     */
    static DocumentClientConfiguration create();

    /**
     * Create a builder instance, with an intent to override default values.
     */
    static DocumentClientConfiguration.Builder builder();

    interface Builder {
        /**
         * Configure the type converters that should be applied globally across all {@link Table}s from the client. This can
         * also be overridden at the Item level.
         *
         * The following type conversions are supported by default:
         * <ul>
         *     <li>{@link Number} -> {@link ItemAttributeValueType#NUMBER}</li>
         *     <li>{@link Temporal} -> {@link ItemAttributeValueType#NUMBER}</li>
         *     <li>{@link CharSequence} -> {@link ItemAttributeValueType#STRING}</li>
         *     <li>{@link UUID} -> {@link ItemAttributeValueType#STRING}</li>
         *     <li>{@link byte[]} -> {@link ItemAttributeValueType#BYTES}</li>
         *     <li>{@link ByteBuffer} -> {@link ItemAttributeValueType#BYTES}</li>
         *     <li>{@link BytesWrapper} -> {@link ItemAttributeValueType#BYTES}</li>
         *     <li>{@link InputStream} -> {@link ItemAttributeValueType#BYTES}</li>
         *     <li>{@link File} -> {@link ItemAttributeValueType#BYTES}</li>
         *     <li>{@link Boolean} -> {@link ItemAttributeValueType#BOOLEAN}</li>
         *     <li>{@link Collection} -> {@link ItemAttributeValueType#LIST_OF_*}</li>
         *     <li>{@link Stream} -> {@link ItemAttributeValueType#LIST_OF_*}</li>
         *     <li>{@link Iterable} -> {@link ItemAttributeValueType#LIST_OF_*}</li>
         *     <li>{@link Iterator} -> {@link ItemAttributeValueType#LIST_OF_*}</li>
         *     <li>{@link Enumeration} -> {@link ItemAttributeValueType#LIST_OF_*}</li>
         *     <li>{@link Optional} -> {@link ItemAttributeValue#*}</li>
         *     <li>{@link Map} -> {@link ItemAttributeValueType#ITEM}</li>
         *     <li>{@link Object} -> {@link ItemAttributeValueType#ITEM}</li>
         *     <li>{@link null} -> {@link ItemAttributeValueType#NULL}</li>
         * </ul>
         *
         * Usage Example:
         * <code>
         *     DocumentClientConfiguration clientConfiguration =
         *             DocumentClientConfiguration.builder()
         *                                        .addConverter(InstantsAsStringsConverter.create())
         *                                        .build();
         *
         *     try (DynamoDbDocumentClient client = DynamoDb.documentClientBuilder()
         *                                                  .documentClientConfiguration(clientConfiguration)
         *                                                  .build()) {
         *
         *         Table table = client.getTable("my-table");
         *         UUID id = UUID.randomUUID();
         *         table.putItem(Item.builder()
         *                           .putAttribute("partition-key", id)
         *                           .putAttribute("creation-time", Instant.now())
         *                           .build());
         *
         *         Item item = table.getItem(Item.builder()
         *                                       .putAttribute("partition-key", id)
         *                                       .build());
         *
         *         // Items are usually stored as a number, but it was stored as an ISO-8601 string now because of the
         *         // InstantsAsStringsConverter.
         *         assert item.attribute("creation-time").isString();
         *         assert item.attribute("creation-time").as(Instant.class).isBetween(Instant.now().minus(1, MINUTE),
         *                                                                            Instant.now());
         *     }
         * </code>
         */
        DocumentClientConfiguration.Builder converters(List<ItemAttributeValueConverter<?>> converters);
        DocumentClientConfiguration.Builder addConverter(ItemAttributeValueConverter<?> converter);
        DocumentClientConfiguration.Builder clearConverters();

        /**
         * Create the configuration object client with all of the configured values.
         */
        DocumentClientConfiguration build();
    }
}

/**
 * A converter between Java types and DynamoDB types. These can be attached to {@link DynamoDbDocumentClient}s and
 * {@link Item}s, so that types are automatically converted when writing to and reading from DynamoDB.
 *
 * @see DocumentClientConfiguration.Builder#converters(List)
 * @see Item.Builder#converter(ItemAttributeValueConverter)
 *
 * @param T The Java type that is generated by this converter.
 */
@ThreadSafe
public interface ItemAttributeValueConverter<T> {
    /**
     * The default condition in which this converter is invoked.
     *
     * Even if this condition is not satisfied, it can still be invoked directly via
     * {@link ItemAttributeValue#convert(ItemAttributeValueConverter)}.
     */
    ConversionCondition defaultConversionCondition();

    /**
     * Convert the provided Java type into an {@link ItemAttributeValue}.
     */
    ItemAttributeValue toAttributeValue(T input, ConversionContext context);

    /**
     * Convert the provided {@link ItemAttributeValue} into a Java type.
     */
    T fromAttributeValue(ItemAttributeValue input, ConversionContext context);
}

/**
 * The condition in which a {@link ItemAttributeValueConverter} will be invoked.
 *
 * @see ItemAttributeValueConverter#defaultConversionCondition().
 */
@ThreadSafe
public interface ConversionCondition {
    /**
     * Create a conversion condition that causes an {@link ItemAttributeValueConverter} to be invoked if an attribute value's
     * {@link ConversionContext} matches a specific condition.
     *
     * This condition has a larger overhead than the {@link #isInstanceOf(Class)} and {@link #never()}, because it must be
     * invoked for every attribute value being converted and its result cannot be cached. For this reason, lower-overhead
     * conditions like {@link #isInstanceOf(Class)} and {@link #never()} should be favored where performance is important.
     */
    static ConversionCondition contextSatisfies(Predicate<ConversionContext> contextPredicate);

    /**
     * Create a conversion condition that causes an {@link ItemAttributeValueConverter} to be invoked if the attribute value's
     * Java type matches the provided class.
     *
     * The result of this condition can be cached, and will likely not be invoked for previously-converted types.
     */
    static ConversionCondition isInstanceOf(Class<?> clazz);

    /**
     * Create a conversion condition that causes an {@link ItemAttributeValueConverter} to never be invoked by default, except
     * when directly invoked via {@link ItemAttributeValue#convert(ItemAttributeValueConverter)}.
     *
     * The result of this condition can be cached, and will likely not be invoked for previously-converted types.
     */
    static ConversionCondition never();
}

/**
 * Additional context that can be used in the context of converting between Java types and {@link ItemAttributeValue}s.
 *
 * @see ItemAttributeValueConverter#toAttributeValue(Object, ConversionContext)
 * @see ItemAttributeValueConverter#fromAttributeValue(ItemAttributeValue, ConversionContext)
 */
@ThreadSafe
public interface ConversionContext {
    /**
     * The name of the attribute being converted.
     */
    String attributeName();

    /**
     * The schema of the attribute being converted.
     */
    ItemAttributeSchema attributeSchema();

    /**
     * The item that contains the attribute being converted.
     */
    Item parent();

    /**
     * The schema of the {@link #parent()}.
     */
    ItemSchema parentSchema();
}

/**
 * The result of invoking {@link DynamoDbDocumentClient#listTables()}.
 */
@ThreadSafe
public interface ListTablesResponse {
    /**
     * A lazily-populated iterator over all tables in the current region. This may make multiple service calls in the
     * background when iterating over the full result set.
     */
    SdkIterable<Table> tables();
}

/**
 * A DynamoDB table, containing a collection of {@link Item}s.
 *
 * Currently supported operations:
 * <ul>
 *     <li>Writing objects with {@link #putItem(Item)} and {@link #putObject(Object)}</li>
 *     <li>Reading objects with {@link #getItem(Item)}} and {@link #getObject(Object)}</li>
 *     <li>Accessing the current table configuration with {@link #metadata()}.</li>
 *     <li>Creating new indexes with {@link #createGlobalSecondaryIndex(CreateGlobalSecondaryIndexRequest)}.</li>
 * </ul>
 *
 * The full version will all table operations, including Query, Delete, Update, Scan, etc.
 */
@ThreadSafe
public interface Table {
    /**
     * Retrieve the name of this table.
     */
    String name();

    /**
     * Invoke DynamoDB to retrieve the metadata for this table.
     */
    TableMetadata metadata();

    /**
     * Invoke DynamoDB to create a new global secondary index for this table.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         ProvisionedCapacity indexCapacity = ProvisionedCapacity.builder()
     *                                                                .readCapacity(5)
     *                                                                .writeCapacity(5)
     *                                                                .build();
     *
     *         KeySchema indexKeys = KeySchema.builder()
     *                                        .putKey("extra-partition-key", ItemAttributeIndexType.PARTITION_KEY)
     *                                        .build();
     *
     *         Table table = client.getTable("my-table");
     *
     *         table.createGlobalSecondaryIndex(CreateGlobalSecondaryIndexRequest.builder()
     *                                                                           .indexName("my-new-index")
     *                                                                           .provisionedCapacity(tableCapacity)
     *                                                                           .keySchema(tableKeys)
     *                                                                           .build());
     *     }
     * </code>
     */
    CreateGlobalSecondaryIndexResponse createGlobalSecondaryIndex(CreateGlobalSecondaryIndexRequest createRequest);

    /**
     * Invoke DynamoDB to create or override an {@link Item} in this table.
     *
     * This method is optimized for performance, and provides no additional response data. For additional options
     * like conditions or consumed capacity, see {@link #putItem(PutItemRequest)}.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         Table table = client.getTable("my-table");
     *         table.putItem(Item.builder()
     *                           .putAttribute("partition-key", UUID.randomUUID())
     *                           .putAttribute("creation-time", Instant.now())
     *                           .build());
     *     }
     * </code>
     */
    void putItem(Item item);

    /**
     * Invoke DynamoDB to create or override an {@link Item} in this table.
     *
     * This method provides more options than {@link #putItem(Item)}, like conditions or consumed capacity.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         Table table = client.getTable("my-table");
     *         table.putItem(PutItemRequest.builder()
     *                                     .item(Item.builder()
     *                                               .putAttribute("partition-key", "key")
     *                                               .putAttribute("version", 2)
     *                                               .build())
     *                                     .condition("version = :expected_version")
     *                                     .putConditionAttribute(":expected_version", 1)
     *                                     .build());
     *     } catch (ConditionFailedException e) {
     *         System.out.println("Precondition failed.");
     *         throw e;
     *     }
     * </code>
     */
    PutItemResponse putItem(PutItemRequest putRequest)
            throws ConditionFailedException;

    /**
     * Invoke DynamoDB to create or override an Item in this table.
     *
     * This will convert the provided object into an {@link Item} automatically using the default Object-to-Item
     * {@link ItemAttributeValueConverter}, unless an alternate converter has been overridden for the provided type.
     *
     * This method is optimized for performance, and provides no additional response data. For additional options
     * like conditions or consumed capacity, see {@link #putObject(PutObjectRequest)}.
     *
     * Usage Example:
     * <code>
     *     public class MyItem {
     *         @Attribute("partition-key")
     *         @Index(AttributeIndexType.PARTITION_KEY)
     *         private String partitionKey;
     *
     *         @Attribute("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(String partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         Table table = client.getTable("my-table");
     *
     *         MyItem myItem = new MyItem();
     *         myItem.setPartitionKey(UUID.randomUUID());
     *         myItem.setCreationTime(Instant.now());
     *
     *         table.putObject(myItem);
     *     }
     * </code>
     */
    void putObject(Object item);

    /**
     * Invoke DynamoDB to create or override an Item in this table.
     *
     * This will convert the provided object into an {@link Item} automatically using the default Object-to-Item
     * {@link ItemAttributeValueConverter}, unless an alternate converter has been overridden for the provided type.
     *
     * This method provides more options than {@link #putObject(Object)} like conditions or consumed capacity.
     *
     * Usage Example:
     * <code>
     *     public class MyItem {
     *         @Attribute("partition-key")
     *         @Index(AttributeIndexType.PARTITION_KEY)
     *         private String partitionKey;
     *
     *         @Attribute
     *         private int version;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public int getVersion() { return this.version; }
     *         public void setPartitionKey(String partitionKey) { this.partitionKey = partitionKey; }
     *         public void setVersion(int version) { this.version = version; }
     *     }
     *
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         Table table = client.getTable("my-table");
     *
     *         MyItem myItem = new MyItem();
     *         myItem.setPartitionKey(UUID.randomUUID());
     *         myItem.setVersion(2);
     *
     *         table.putObject(PutObjectRequest.builder(myItem)
     *                                         .condition("version = :expected_version")
     *                                         .putConditionAttribute(":expected_version", 1)
     *                                         .build());
     *     } catch (ConditionFailedException e) {
     *         System.out.println("Precondition failed.");
     *         throw e;
     *     }
     * </code>
     */
    <T> PutObjectResponse<T> putObject(PutObjectRequest<T> putRequest)
            throws ConditionFailedException;

    /**
     * Invoke DynamoDB to retrieve an Item in this table, based on its partition key (and sort key, if the table has one).
     *
     * This method is optimized for performance, and provides no additional response data. For additional options
     * like consistent reads, see {@link #getItem(GetItemRequest)}.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         Table table = client.getTable("my-table");
     *         UUID id = UUID.randomUUID();
     *         table.putItem(Item.builder()
     *                           .putAttribute("partition-key", id)
     *                           .putAttribute("creation-time", Instant.now())
     *                           .build());
     *
     *         // Wait a little bit, because getItem is eventually consistent by default.
     *         Thread.sleep(5_000);
     *
     *         Item item = table.getItem(Item.builder()
     *                                       .putAttribute("partition-key", id)
     *                                       .build());
     *
     *         // Times are stored as numbers, by default, so they can also be used as sort keys.
     *         assert item.attribute("creation-time").isNumber();
     *         assert item.attribute("creation-time").as(Instant.class).isBetween(Instant.now().minus(1, MINUTE),
     *                                                                            Instant.now());
     *     } catch (NoSuchItemException e) {
     *         System.out.println("Item could not be found. Maybe we didn't wait long enough for consistency?");
     *         throw e;
     *     }
     * </code>
     */
    Item getItem(Item item)
            throws NoSuchItemException;

    /**
     * Invoke DynamoDB to retrieve an Item in this table, based on its partition key (and sort key, if table has one).
     *
     * This method provides more options than {@link #getItem(Item)}, like whether reads should be consistent.
     *
     * Usage Example:
     * <code>
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         Table table = client.getTable("my-table");
     *         UUID id = UUID.randomUUID();
     *         table.putItem(Item.builder()
     *                           .putAttribute("partition-key", id)
     *                           .putAttribute("creation-time", Instant.now())
     *                           .build());
     *
     *         GetItemResponse response =
     *                 table.getItem(GetItemRequest.builder()
     *                                             .item(Item.builder()
     *                                                       .putAttribute("partition-key", id)
     *                                                       .build())
     *                                             .consistentRead(true)
     *                                             .build());
     *
     *         // Times are stored as numbers, by default, so they can also be used as sort keys.
     *         assert response.item().attribute("creation-time").isNumber();
     *         assert response.item().attribute("creation-time").as(Instant.class).isBetween(Instant.now().minus(1, MINUTE),
     *                                                                                       Instant.now());
     *     } catch (NoSuchItemException e) {
     *         System.out.println("Item was deleted between creation and retrieval.");
     *         throw e;
     *     }
     * </code>
     */
    GetItemResponse getItem(GetItemRequest getRequest)
            throws NoSuchItemException;

    /**
     * Invoke DynamoDB to retrieve an Item in this table.
     *
     * This will use the partition and sort keys from the provided object and convert the DynamoDB response to a Java object
     * automatically using the default Object-to-Item {@link ItemAttributeValueConverter}, unless an alternate converter
     * has been overridden for the provided type.
     *
     * This method is optimized for performance, and provides no additional response data. For additional options
     * like consistent reads, see {@link #getObject(GetObjectRequest)}.
     *
     * Usage Example:
     * <code>
     *     public class MyItem {
     *         @Attribute("partition-key")
     *         @Index(AttributeIndexType.PARTITION_KEY)
     *         private String partitionKey;
     *
     *         @Attribute("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(String partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         Table table = client.getTable("my-table");
     *
     *         UUID id = UUID.randomUUID();
     *
     *         MyItem itemToCreate = new MyItem();
     *         itemToCreate.setPartitionKey(id);
     *         itemToCreate.setCreationTime(Instant.now());
     *
     *         table.putObject(itemToCreate);
     *
     *         // Wait a little bit, because getObject is eventually consistent by default.
     *         Thread.sleep(5_000);
     *
     *         MyItem itemToRetrieve = new MyItem();
     *         itemToRetrieve.setPartitionKey(id);
     *
     *         MyItem retrievedItem = table.getObject(itemToRetrieve);
     *         assert retrievedItem.getCreationTime().isBetween(Instant.now().minus(1, MINUTE),
     *                                                          Instant.now());
     *     } catch (NoSuchItemException e) {
     *         System.out.println("Item could not be found. Maybe we didn't wait long enough for consistency?");
     *         throw e;
     *     }
     * </code>
     */
    <T> T getObject(T item)
            throws NoSuchItemException;

    /**
     * Invoke DynamoDB to retrieve an Item in this table.
     *
     * This will use the partition and sort keys from the provided object and convert the DynamoDB response to a Java object
     * automatically using the default Object-to-Item {@link ItemAttributeValueConverter}, unless an alternate converter
     * has been overridden for the provided type.
     *
     * This method provides more options than {@link #getObject(GetObjectRequest)}, like whether reads should be consistent.
     *
     * Usage Example:
     * <code>
     *     public class MyItem {
     *         @Attribute("partition-key")
     *         @Index(AttributeIndexType.PARTITION_KEY)
     *         private String partitionKey;
     *
     *         @Attribute("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(String partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DynamoDbDocumentClient client = DynamoDb.documentClient()) {
     *         Table table = client.getTable("my-table");
     *
     *         UUID id = UUID.randomUUID();
     *
     *         MyItem itemToCreate = new MyItem();
     *         itemToCreate.setPartitionKey(id);
     *         itemToCreate.setCreationTime(Instant.now());
     *
     *         table.putObject(itemToCreate);
     *
     *         MyItem itemToRetrieve = new MyItem();
     *         itemToRetrieve.setPartitionKey(id);
     *
     *         GetObjectResponse<MyItem> response = table.getObject(GetObjectRequest.builder(itemToRetrieve)
     *                                                                              .consistentReads(true)
     *                                                                              .build());
     *         MyItem retrievedItem = response.item();
     *         assert retrievedItem.getCreationTime().isBetween(Instant.now().minus(1, MINUTE),
     *                                                          Instant.now());
     *     } catch (NoSuchItemException e) {
     *         System.out.println("Item was deleted between creation and retrieval.");
     *         throw e;
     *     }
     * </code>
     */
    <T> GetObjectResponse<T> getObject(GetObjectRequest<T> getRequest)
            throws NoSuchItemException;
}

/**
 * Additional information about a {@link Table}, retrieved via {@link Table#metadata()}.
 */
@ThreadSafe
public interface TableMetadata {
    /**
     * All global secondary indexes that can be used for querying or retrieving from the table.
     */
    List<GlobalSecondaryIndexMetadata> globalSecondaryIndexMetadata();

    /**
     * All local secondary indexes that can be used for querying or retrieving from the table.
     */
    List<LocalSecondaryIndexMetadata> localSecondaryIndexMetadata();
}

/**
 * An item in a {@link Table}. This is similar to a "row" in a traditional relational database.
 *
 * In the following table, { "User ID": 1, "Username": "joe" } is an item:
 *
 * <pre>
 * Table: Users
 * | ------------------ |
 * | User ID | Username |
 * | ------------------ |
 * |       1 |      joe |
 * |       2 |     jane |
 * | ------------------ |
 * </pre>
 */
@ThreadSafe
public interface Item {
    /**
     * Create a builder for configuring and creating a {@link Item}.
     */
    static Item.Builder builder();

    /**
     * Retrieve all {@link ItemAttributeValue}s in this item.
     */
    Map<String, ItemAttributeValue> attributes();

    /**
     * Retrieve a specific attribute from this item.
     */
    ItemAttributeValue attribute(String attributeKey);

    interface Builder {
        /**
         * Add an attribute to this item. The methods accepting "Object", will be converted using the default
         * {@link ItemAttributeValueConverter}s.
         */
        Item.Builder putAttribute(String attributeKey, ItemAttributeValue attributeValue);
        Item.Builder putAttribute(String attributeKey, ItemAttributeValue attributeValue, ItemAttributeSchema attributeSchema);
        Item.Builder putAttribute(String attributeKey, Object attributeValue);
        Item.Builder putAttribute(String attributeKey, Object attributeValue, ItemAttributeSchema attributeSchema);
        Item.Builder removeAttribute(String attributeKey);
        Item.Builder clearAttributes();

        /**
         * Add converters that should be used for this item and its attributes. These converters are used with a higher
         * precidence than those configured in the {@link DocumentClientConfiguration}.
         *
         * See {@link DocumentClientConfiguration.Builder#addConverter(ItemAttributeValueConverter)} for example usage.
         */
        Item.Builder converters(List<ItemAttributeValueConverter<?>> converters);
        Item.Builder addConverter(ItemAttributeValueConverter<?> converter);
        Item.Builder clearConverters();

        /**
         * Create an {@link Item} using the current configuration on the builder.
         */
        Item build();
    }
}

/**
 * The value of an attribute within an {@link Item}. In a traditional relational database, this would be analogous to a cell
 * in the table.
 *
 * In the following table, "joe" and "jane" are both attribute values:
 * <pre>
 * Table: Users
 * | ------------------ |
 * | User ID | Username |
 * | ------------------ |
 * |       1 |      joe |
 * |       2 |     jane |
 * | ------------------ |
 * </pre>
 */
@ThreadSafe
public interface ItemAttributeValue {
    /**
     * Create an {@link ItemAttributeValue} from the provided object.
     */
    static ItemAttributeValue from(Object object);

    /**
     * Create an {@link ItemAttributeValue} from the provided object, and associate this value with the provided
     * {@link ItemAttributeValueConverter}. This allows it to be immediately converted with {@link #as(Class)}.
     *
     * This is equivalent to {@code ItemAttributeValue.from(object).convertFromJavaType(converter)}.
     */
    static ItemAttributeValue from(Object object, ItemAttributeValueConverter<?> converter);

    /**
     * Create an {@link ItemAttributeValue} that represents the DynamoDB-specific null type.
     */
    static ItemAttributeValue nullValue();

    /**
     * Convert this item attribute value into the requested Java type.
     *
     * This uses the {@link ItemAttributeValueConverter} configured on this type via
     * {@link #from(Object, ItemAttributeValueConverter)} or {@link #convertFromJavaType(ItemAttributeValueConverter)}.
     */
    <T> T as(Class<T> type);

    /**
     * Retrieve the {@link ItemAttributeValueType} of this value.
     */
    ItemAttributeValueType type();

    /**
     * The {@code is*} methods can be used to check the underlying DynamoDB-specific type of the attribute value.
     *
     * If the type isn't known (eg. because it was created via {@link ItemAttributeValue#from(Object)}), {@link #isJavaType()}
     * will return true. Such types will be converted into DynamoDB-specific types by the document client before they are
     * persisted.
     */

    boolean isItem();
    boolean isString();
    boolean isNumber();
    boolean isBytes();
    boolean isBoolean();
    boolean isListOfStrings();
    boolean isListOfNumbers();
    boolean isListOfBytes();
    boolean isListOfAttributeValues();
    boolean isNull();
    boolean isJavaType();

    /**
     * The {@code as*} methods can be used to retrieve this value without the overhead of type conversion of {@link #as(Class)}.
     *
     * An exception will be thrown from these methods if the requested type does not match the actual underlying type. When
     * the type isn't know, the {@code is*} or {@link #type()} methods can be used to query the underlying type before
     * invoking these {@code as*} methods.
     */
    Item asItem();
    String asString();
    BigDecimal asNumber();
    SdkBytes asBytes();
    Boolean asBoolean();
    List<String> asListOfStrings();
    List<BigDecimal> asListOfNumbers();
    List<SdkBytes> asListOfBytes();
    List<ItemAttributeValue> asListOfAttributeValues();
    Object asJavaType();

    /**
     * Convert this attribute value from a {@link ItemAttributeValueType#JAVA_TYPE} to a type that can be persisted in DynamoDB.
     *
     * This will throw an exception if {@link #isJavaType()} is false.
     */
    ItemAttributeValue convertFromJavaType(ItemAttributeValueConverter<?> converter);
}

/**
 * The schema for a specific item. This describes the item's structure and which attributes it contains.
 *
 * This is mostly an implementation detail, and can be ignored except by developers interested in creating
 * {@link ItemAttributeValueConverter}.
 */
@ThreadSafe
public interface ItemSchema {
    /**
     * Create a builder for configuring and creating an {@link ItemSchema}.
     */
    static ItemSchema.Builder builder();

    interface Builder {
        /**
         * Specify the attribute schemas that describe each attribute of this item.
         */
        ItemSchema.Builder attributeSchemas(Map<String, ItemAttributeSchema> attributeSchemas);
        ItemSchema.Builder putAttributeSchema(String attributeName, ItemAttributeSchema attributeSchema);
        ItemSchema.Builder removeAttributeSchema(String attributeName);
        ItemSchema.Builder clearAttributeSchemas();

        /**
         * The converter that should be used for converting all items that conform to this schema.
         */
        ItemSchema.Builder converter(ItemAttributeValueConverter<?> converter);

        /**
         * Create an {@link ItemSchema} using the current configuration on the builder.
         */
        ItemSchema build();
    }
}

/**
 * The schema for a specific item attribute. This describes the attribute's structure, including whether it is known to be an
 * index, what the Java-specific type representation is for this attribute, etc.
 *
 * This is mostly an implementation detail, and can be ignored except by developers interested in creating
 * {@link ItemAttributeValueConverter}.
 */
@ThreadSafe
public interface ItemAttributeSchema {
    /**
     * Create a builder for configuring and creating an {@link ItemAttributeSchema}.
     */
    static ItemAttributeSchema.Builder builder();

    interface Builder {
        /**
         * Specify whether this field is known to be an index.
         */
        ItemAttributeSchema.Builder indexType(AttributeIndexType attributeIndexType);

        /**
         * Specify the Java-specific type representation for this type.
         */
        ItemAttributeSchema.Builder javaType(Class<?> attributeJavaType);

        /**
         * The DynamoDB-specific type representation for this type.
         */
        ItemAttributeSchema.Builder dynamoType(ItemAttributeValueType attributeDynamoType);

        /**
         * The converter that should be used for converting all items that conform to this schema.
         */
        ItemAttributeSchema.Builder converter(ItemAttributeValueConverter<?> converter);

        /**
         * Create an {@link ItemAttributeSchema} using the current configuration on the builder.
         */
        ItemAttributeSchema build();
    }
}

/**
 * The index type of an {@link ItemAttributeValue}.
 */
@ThreadSafe
public enum ItemAttributeIndexType {
    PARTITION_KEY,
    SORT_KEY,
    NOT_AN_INDEX
}

/**
 * The underlying type of an {@link ItemAttributeValue}.
 */
@ThreadSafe
public enum ItemAttributeValueType {
    ITEM,
    STRING,
    NUMBER,
    BYTES,
    BOOLEAN,
    LIST_OF_STRINGS,
    LIST_OF_NUMBERS,
    LIST_OF_BYTES,
    LIST_OF_ATTRIBUTE_VALUES,
    NULL,
    JAVA_TYPE
}