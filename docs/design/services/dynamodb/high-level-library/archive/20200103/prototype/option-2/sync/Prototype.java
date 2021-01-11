/**
 * A synchronous client for interacting (generically) with document databases.
 *
 * Features:
 * <ol>
 *     <li>Support for Java-specific types, like {@link Instant} and {@link BigDecimal}.</li>
 *     <li>Support for reading and writing custom objects (eg. Java Beans, POJOs).</li>
 * </ol>
 *
 * All {@link DocumentClient}s should be closed via {@link #close()}.
 */
@ThreadSafe
public interface DocumentClient extends SdkAutoCloseable {
    /**
     * Create a {@link DocumentClient} for interating with document databases.
     *
     * The provided runtime will be used for handling object persistence.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         client.listRepositories().repositories().forEach(System.out::println);
     *     }
     * </code>
     */
    static DocumentClient create(Class<? extends DocumentClientRuntime> runtimeClass);

    /**
     * Create a {@link DocumentClient.Builder} for configuring and creating {@link DocumentClient}s.
     *
     * The provided runtime will be used for handling object persistence.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.builder(DynamoDbRuntime.class)
     *                                                .putOption(DynamoDbOption.CLIENT, DynamoDbClient.create())
     *                                                .build()) {
     *         client.listRepositories().repositories().forEach(System.out::println);
     *     }
     * </code>
     */
    static DocumentClient.Builder builder(Class<? extends DocumentClientRuntime> runtimeClass);

    /**
     * Get all {@link DocumentRepository}s that are currently available from this client.
     *
     * This should return every repository that will not result in {@code client.repository(...)} throwing a
     * {@link NoSuchRepositoryException}.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         client.listRepositories().repositories().forEach(System.out::println);
     *     }
     * </code>
     */
    ListRepositoriesResponse listRepositories();

    /**
     * Retrieve a {@link DocumentRepository} based on the provided repository name/id.
     *
     * The {@link DocumentRepository} is used to directly interact with entities in the remote repository.
     * See {@link #mappedRepository(Class)} for a way of interacting with the repository using Java objects.
     *
     * If the repository does not exist, an exception is thrown.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         DocumentRepository repository = client.repository("my-table");
     *         assert repository.name().equals("my-table");
     *     } catch (NoSuchRepositoryException e) {
     *         System.out.println("The requested repository does not exist: " + e.getMessage());
     *         throw e;
     *     }
     * </code>
     */
    DocumentRepository repository(String repositoryId) throws NoSuchRepositoryException;

    /**
     * Retrieve a {@link DocumentRepository} based on the provided repository name/id.
     *
     * The {@link DocumentRepository} is used to create, read, update and delete entities in the remote repository.
     * See {@link #mappedRepository(Class)} for a way of interacting with the repository using Java objects.
     *
     * If the repository does not exist, the provided {@link MissingRepositoryBehavior} determines the behavior.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         DocumentRepository repository = client.repository("my-table", MissingRepositoryBehavior.CREATE);
     *         assert repository.name().equals("my-table");
     *     }
     * </code>
     */
    DocumentRepository repository(String repositoryId, MissingRepositoryBehavior behavior) throws NoSuchRepositoryException;

    /**
     * Retrieve an implementation of a specific {@link MappedRepository} based on the provided Java object class.
     *
     * This {@link MappedRepository} implementation is used to create, read, update and delete entities in the remote repository
     * using Java objects. See {@link #repository(String)} for a way of interacting directly with the entities.
     *
     * If the repository does not exist, an exception is thrown.
     *
     * Usage Example:
     * <code>
     *     @MappedRepository("my-table")
     *     public interface MyItemRepository extends MappedRepository<MyItem, String> {
     *     }
     *
     *     public class MyItem {
     *         @Id
     *         @Column("partition-key")
     *         private UUID partitionKey;
     *
     *         @Column("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(UUID partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         MyItemRepository repository = client.mappedRepository(MyItemRepository.class);
     *         assert repository.name().equals("my-table");
     *     } catch (NoSuchRepositoryException e) {
     *         System.out.println("The requested repository does not exist: " + e.getMessage());
     *         throw e;
     *     }
     * </code>
     */
    <T extends MappedRepository<?, ?>> T mappedRepository(Class<T> repositoryClass);

    /**
     * Retrieve an implementation of a specific {@link MappedRepository} based on the provided Java object class.
     *
     * This {@link MappedRepository} implementation is used to create, read, update and delete entities in the remote repository
     * using Java objects. See {@link #repository(String)} for a way of interacting directly with the entities.
     *
     * If the repository does not exist, the provided {@link MissingRepositoryBehavior} determines the behavior.
     *
     * Usage Example:
     * <code>
     *     @MappedRepository("my-table")
     *     public interface MyItemRepository extends MappedRepository<MyItem, String> {
     *     }
     *
     *     public class MyItem {
     *         @Id
     *         @Column("partition-key")
     *         private UUID partitionKey;
     *
     *         @Column("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(UUID partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         MyItemRepository repository = client.mappedRepository(MyItemRepository.class, MissingRepositoryBehavior.CREATE);
     *         assert repository.name().equals("my-table");
     *     }
     * </code>
     */
    <T extends MappedRepository<?, ?>> T mappedRepository(Class<T> repositoryClass, MissingRepositoryBehavior behavior);

    /**
     * A builder for configuring and creating {@link DocumentClient} instances.
     *
     * @see #builder(Class)
     */
    @NotThreadSafe
    interface Builder {
        /**
         * Configure the type converters that should be applied globally across all {@link DocumentRepository}s and
         * {@link MappedRepository}s from the client. This can also be overridden at the entity level.
         *
         * The following type conversions are supported by default:
         * <ul>
         *     <li>{@link Long} / {@link long} -> {@link EntityValueType#LONG}</li>
         *     <li>{@link Integer} / {@link int} -> {@link EntityValueType#INT}</li>
         *     <li>{@link Short} / {@link short} -> {@link EntityValueType#SHORT}</li>
         *     <li>{@link Float} / {@link float} -> {@link EntityValueType#FLOAT}</li>
         *     <li>{@link Double} / {@link double} -> {@link EntityValueType#DOUBLE}</li>
         *     <li>{@link Number} -> {@link EntityValueType#NUMBER}</li>
         *     <li>{@link Temporal} -> {@link EntityValueType#NUMBER}</li>
         *     <li>{@link Char} / {@link char} -> {@link EntityValueType#STRING}</li>
         *     <li>{@link char[]} -> {@link EntityValueType#STRING}</li>
         *     <li>{@link CharSequence} -> {@link EntityValueType#STRING}</li>
         *     <li>{@link UUID} -> {@link EntityValueType#STRING}</li>
         *     <li>{@link byte[]} -> {@link EntityValueType#BYTES}</li>
         *     <li>{@link ByteBuffer} -> {@link EntityValueType#BYTES}</li>
         *     <li>{@link BytesWrapper} -> {@link EntityValueType#BYTES}</li>
         *     <li>{@link InputStream} -> {@link EntityValueType#BYTES}</li>
         *     <li>{@link File} -> {@link EntityValueType#BYTES}</li>
         *     <li>{@link Path} -> {@link EntityValueType#BYTES}</li>
         *     <li>{@link Boolean} -> {@link EntityValueType#BOOLEAN}</li>
         *     <li>{@link Collection} -> {@link EntityValueType#LIST}</li>
         *     <li>{@link Stream} -> {@link EntityValueType#LIST}</li>
         *     <li>{@link Iterable} -> {@link EntityValueType#LIST}</li>
         *     <li>{@link Iterator} -> {@link EntityValueType#LIST}</li>
         *     <li>{@link Enumeration} -> {@link EntityValueType#LIST}</li>
         *     <li>{@link Optional} -> {@link EntityValueType#*}</li>
         *     <li>{@link Map} -> {@link EntityValueType#ENTITY}</li>
         *     <li>{@link Object} -> {@link EntityValueType#ENTITY}</li>
         *     <li>{@link null} -> {@link EntityValueType#NULL}</li>
         * </ul>
         *
         * Usage Example:
         * <code>
         *     try (DocumentClient client = DocumentClient.builder(DynamoDbRuntime.class)
         *                                                .addConverter(InstantsAsStringsConverter.create())
         *                                                .build()) {
         *         DocumentRepository repository = client.repository("my-table");
         *
         *         UUID id = UUID.randomUUID();
         *         repository.putEntity(Entity.builder()
         *                                    .putChild("partition-key", id)
         *                                    .putChild("creation-time", Instant.now())
         *                                    .build());
         *
         *         Thread.sleep(5_000); // GetEntity is eventually consistent with the Dynamo DB runtime.
         *
         *         Entity item = repository.getEntity(Entity.builder()
         *                                                  .putChild("partition-key", id)
         *                                                  .build());
         *
         *         // Instants are usually converted to a number-type, but it was stored as an ISO-8601 string now because of the
         *         // InstantsAsStringsConverter.
         *         assert item.getChild("creation-time").isString();
         *         assert item.getChild("creation-time").as(Instant.class).isBetween(Instant.now().minus(1, MINUTE),
         *                                                                           Instant.now());
         *     }
         * </code>
         */
        DocumentClient.Builder converters(Iterable<? extends EntityValueConverter<?>> converters);
        DocumentClient.Builder addConverter(EntityValueConverter<?> converter);
        DocumentClient.Builder clearConverters();

        /**
         * Usage Example:
         * <code>
         *     try (DocumentClient client = DocumentClient.builder(DynamoDbRuntime.class)
         *                                                .putOption(DynamoDbOption.CLIENT, DynamoDbClient.create())
         *                                                .build()) {
         *         client.listRepositories().repositories().forEach(System.out::println);
         *     }
         * </code>
         */
        DocumentClient.Builder options(Map<? extends OptionKey<?>, ?> options);
        <T> DocumentClient.Builder putOption(OptionKey<T> optionKey, T optionValue);
        DocumentClient.Builder removeOption(OptionKey<?> optionKey);
        DocumentClient.Builder clearOptions();
    }
}

/**
 * When calling {@link DocumentClient#repository} or {@link DocumentClient#mappedRepository} and a repository does not exist on
 * the service side, this is the behavior that the client should take.
 */
@ThreadSafe
public enum MissingRepositoryBehavior {
    /**
     * Create the repository, if it's missing.
     */
    CREATE,

    /**
     * Throw a {@link NoSuchRepositoryException}, if the repository is missing.
     */
    FAIL,

    /**
     * Do not check whether the repository exists, for performance reasons. Methods that require the repository to exist will
     * fail.
     */
    DO_NOT_CHECK
}

/**
 * An interface that repository-specific runtimes can implement to be supported by the document client.
 */
@ThreadSafe
public interface DocumentClientRuntime {
}

/**
 * The DynamoDB implementation of the {@link DocumentClientRuntime}.
 */
@ThreadSafe
public interface DynamoDbRuntime extends DocumentClientRuntime {
}

/**
 * The DynamoDB-specific options available for configuring the {@link DynamoDbRuntime} via
 * {@link DocumentClient.Builder#putOption}.
 */
@ThreadSafe
public class DynamoDbOption {
    /**
     * Configure the DynamoDB client that should be used for communicating with DynamoDB.
     *
     * This only applies to the {@link DynamoDbRuntime}.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.builder(DynamoDbRuntime.class)
     *                                                .putOption(DynamoDbOption.CLIENT, DynamoDbClient.create())
     *                                                .build()) {
     *         client.listRepositories().repositories().forEach(System.out::println);
     *     }
     * </code>
     */
    public static final Option<DynamoDbClient> CLIENT = new Option<>(DynamoDbClient.class);
}

/**
 * A converter between Java types and repository types. These can be attached to {@link DocumentClient}s and
 * {@link Entity}s, so that types are automatically converted when writing to and reading from the repository.
 *
 * @see DocumentClient.Builder#converters(Iterable)
 * @see Entity.Builder#addConverter(EntityValueConverter)
 *
 * @param T The Java type that is generated by this converter.
 */
@ThreadSafe
public interface EntityValueConverter<T> {
    /**
     * The default condition in which this converter is invoked.
     *
     * Even if this condition is not satisfied, it can still be invoked directly via
     * {@link EntityValue#from(Object, EntityValueConverter)}.
     */
    ConversionCondition defaultConversionCondition();

    /**
     * Convert the provided Java type into an {@link EntityValue}.
     */
    EntityValue toEntityValue(T input, ConversionContext context);

    /**
     * Convert the provided {@link EntityValue} into a Java type.
     */
    T fromEntityValue(EntityValue input, ConversionContext context);
}

/**
 * The condition in which a {@link EntityValueConverter} will be invoked.
 *
 * @see EntityValueConverter#defaultConversionCondition()
 */
@ThreadSafe
public interface ConversionCondition {
    /**
     * Create a conversion condition that causes an {@link EntityValueConverter} to be invoked if an entity value's
     * {@link ConversionContext} matches a specific condition.
     *
     * This condition has a larger overhead than the {@link #isInstanceOf(Class)} and {@link #never()}, because it must be
     * invoked for every entity value being converted and its result cannot be cached. For this reason, lower-overhead
     * conditions like {@link #isInstanceOf(Class)} and {@link #never()} should be favored where performance is important.
     */
    static ConversionCondition contextSatisfies(Predicate<ConversionContext> contextPredicate);

    /**
     * Create a conversion condition that causes an {@link EntityValueConverter} to be invoked if the entity value's
     * Java type matches the provided class.
     *
     * The result of this condition can be cached, and will likely not be invoked for previously-converted types.
     */
    static ConversionCondition isInstanceOf(Class<?> clazz);

    /**
     * Create a conversion condition that causes an {@link EntityValueConverter} to never be invoked by default, except
     * when directly invoked via {@link EntityValue#from(Object, EntityValueConverter)}.
     *
     * The result of this condition can be cached, and will likely not be invoked for previously-converted types.
     */
    static ConversionCondition never();
}

/**
 * Additional context that can be used when converting between Java types and {@link EntityValue}s.
 *
 * @see EntityValueConverter#fromEntityValue(EntityValue, ConversionContext)
 * @see EntityValueConverter#toEntityValue(java.lang.Object, ConversionContext)
 */
@ThreadSafe
public interface ConversionContext {
    /**
     * The name of the entity value being converted.
     */
    String entityValueName();

    /**
     * The schema of the entity value being converted.
     */
    EntityValueSchema entityValueSchema();

    /**
     * The entity that contains the entity value being converted.
     */
    Entity parent();

    /**
     * The schema of the {@link #parent()}.
     */
    EntitySchema parentSchema();
}

/**
 * The result of invoking {@link DocumentClient#listRepositories()}.
 */
@ThreadSafe
public interface ListRepositoriesResponse {
    /**
     * A lazily-populated iterator over all accessible repositories. This may make multiple service calls in the
     * background when iterating over the full result set.
     */
    SdkIterable<DocumentRepository> repositories();
}

/**
 * A client that can be used for creating, reading, updating and deleting entities in a remote repository.
 *
 * Created via {@link DocumentClient#repository(String)}.
 *
 * Usage Example:
 * <code>
 *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
 *         DocumentRepository repository = client.repository("my-table");
 *
 *         UUID id = UUID.randomUUID();
 *         repository.putEntity(Entity.builder()
 *                                    .putChild("partition-key", id)
 *                                    .putChild("creation-time", Instant.now())
 *                                    .build());
 *
 *         Thread.sleep(5_000); // GetEntity is eventually consistent with the Dynamo DB runtime.
 *
 *         Entity item = repository.getEntity(Entity.builder()
 *                                                  .putChild("partition-key", id)
 *                                                  .build());
 *
 *         assert item.getChild("creation-time").as(Instant.class).isBetween(Instant.now().minus(1, MINUTE),
 *                                                                           Instant.now());
 *     } catch (NoSuchEntityException e) {
 *         System.out.println("Item could not be found. Maybe we didn't wait long enough for consistency?");
 *         throw e;
 *     }
 * </code>
 */
public interface DocumentRepository {
    /**
     * Retrieve the name of this repository.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         DocumentRepository repository = client.repository("my-table");
     *         assert repository.name().equals("my-table");
     *     }
     * </code>
     */
    String name();

    /**
     * Convert this repository to a mapped-repository, so that Java objects can be persisted and retrieved.
     *
     * Usage Example:
     * <code>
     *     @MappedRepository("my-table")
     *     public interface MyItemRepository extends MappedRepository<MyItem, String> {
     *     }
     *
     *     public class MyItem {
     *         @Id
     *         @Column("partition-key")
     *         private UUID partitionKey;
     *
     *         @Column("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(UUID partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         DocumentRepository unmappedRepository = client.repository("my-table");
     *         MyItemRepository mappedRepository = unmappedRepository.toMappedRepository(MyItemRepository.class);
     *         assert mappedRepository.name().equals("my-table");
     *     }
     * </code>
     */
    <T extends MappedRepository<?, ?>> T toMappedRepository(Class<T> repositoryClass);

    /**
     * Create or update an existing entity in the repository.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         DocumentRepository repository = client.repository("my-table");
     *
     *         repository.putEntity(Entity.builder()
     *                                    .putChild("partition-key", UUID.randomUUID())
     *                                    .putChild("creation-time", Instant.now())
     *                                    .build());
     *     }
     * </code>
     */
    void putEntity(Entity entity);

    /**
     * Create or update an existing entity in the repository.
     *
     * This API allows specifying additional runtime-specific options via {@link PutRequest#putOption}, and retrieving runtime-
     * specific options via {@link PutResponse#option}.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         DocumentRepository repository = client.repository("my-table");
     *
     *         PutResponse response =
     *             repository.putEntity(Entity.builder()
     *                                        .putChild("partition-key", UUID.randomUUID())
     *                                        .putChild("creation-time", Instant.now())
     *                                        .build(),
     *                                  PutRequest.builder()
     *                                            .putOption(DynamoDbPutRequestOption.REQUEST,
     *                                                       putItemRequest -> putItemRequest.returnConsumedCapacity(TOTAL))
     *                                            .build());
     *         System.out.println(response.option(DynamoDbPutResponseOption.RESPONSE).consumedCapacity());
     *     }
     * </code>
     */
    PutResponse putEntity(Entity entity, PutRequest options)

    /**
     * Retrieve an entity from the repository. The index will be chosen automatically based on the fields provided in the
     * input entity.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         DocumentRepository repository = client.repository("my-table");
     *
     *         UUID id = UUID.randomUUID();
     *         repository.putEntity(Entity.builder()
     *                                    .putChild("partition-key", id)
     *                                    .putChild("creation-time", Instant.now())
     *                                    .build());
     *
     *         Thread.sleep(5_000); // GetEntity is eventually consistent with the Dynamo DB runtime.
     *
     *         Entity item = repository.getEntity(Entity.builder()
     *                                                  .putChild("partition-key", id)
     *                                                  .build());
     *
     *         assert item.getChild("creation-time").as(Instant.class).isBetween(Instant.now().minus(1, MINUTE),
     *                                                                           Instant.now());
     *     } catch (NoSuchEntityException e) {
     *         System.out.println("Item could not be found. Maybe we didn't wait long enough for consistency?");
     *         throw e;
     *     }
     * </code>
     */
    Entity getEntity(Entity keyEntity);

    /**
     * Retrieve an entity from the repository. The index will be chosen automatically based on the fields provided in the
     * input entity.
     *
     * This API allows specifying additional runtime-specific options via {@link GetRequest#putOption}, and retrieving runtime-
     * specific options via {@link GetResponse#option}.
     *
     * Usage Example:
     * <code>
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         DocumentRepository repository = client.repository("my-table");
     *
     *         UUID id = UUID.randomUUID();
     *         repository.putEntity(Entity.builder()
     *                                    .putChild("partition-key", id)
     *                                    .putChild("creation-time", Instant.now())
     *                                    .build());
     *
     *         GetResponse response =
     *             repository.getEntity(Entity.builder()
     *                                        .putChild("partition-key", id)
     *                                        .build(),
     *                                  GetRequest.builder()
     *                                            .putOption(DynamoDbGetRequestOption.CONSISTENT, true)
     *                                            .build());
     *
     *         assert response.entity().getChild("creation-time").as(Instant.class).isBetween(Instant.now().minus(1, MINUTE),
     *                                                                                        Instant.now());
     *     }
     * </code>
     */
    GetResponse getEntity(Entity keyEntity, GetRequest options);
}

/**
 * A base class for type-specific repositories for creating, reading, updating and deleting entities in the remote repository
 * using Java objects.
 *
 * Created via {@link DocumentClient#mappedRepository(Class)}.
 */
public interface MappedRepository<T, ID> extends DocumentRepository {
    /**
     * Create or update an existing entity in the repository.
     *
     * Usage Example:
     * <code>
     *     @MappedRepository
     *     public interface MyItemRepository extends MappedRepository<MyItem, String> {
     *     }
     *
     *     @Repository("my-table")
     *     public class MyItem {
     *         @Id
     *         @Column("partition-key")
     *         private UUID partitionKey;
     *
     *         @Column("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(UUID partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         MyItemRepository repository = client.repository(MyItemRepository.class);
     *
     *         UUID id = UUID.randomUUID();
     *
     *         MyItem itemToCreate = new MyItem();
     *         itemToCreate.setPartitionKey(id);
     *         itemToCreate.setCreationTime(Instant.now());
     *
     *         repository.putObject(itemToCreate);
     *     }
     * </code>
     */
    void putObject(T entity);

    /**
     * Create or update an existing entity in the repository.
     *
     * This API allows specifying additional runtime-specific options via {@link PutRequest#putOption}, and retrieving runtime-
     * specific options via {@link PutObjectResponse#option}.
     *
     * Usage Example:
     * <code>
     *     @MappedRepository
     *     public interface MyItemRepository extends MappedRepository<MyItem, String> {
     *     }
     *
     *     @Repository("my-table")
     *     public class MyItem {
     *         @Id
     *         @Column("partition-key")
     *         private UUID partitionKey;
     *
     *         @Column("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(UUID partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         MyItemRepository repository = client.repository(MyItemRepository.class);
     *
     *         UUID id = UUID.randomUUID();
     *
     *         MyItem itemToCreate = new MyItem();
     *         itemToCreate.setPartitionKey(id);
     *         itemToCreate.setCreationTime(Instant.now());
     *
     *         PutObjectResponse<MyItem> response =
     *             repository.putObject(itemToCreate,
     *                                  PutRequest.builder()
     *                                            .putOption(DynamoDbPutRequestOption.REQUEST,
     *                                                       putItemRequest -> putItemRequest.returnConsumedCapacity(TOTAL))
     *                                            .build());
     *         System.out.println(response.option(DynamoDbPutResponseOption.RESPONSE).consumedCapacity());
     *     }
     * </code>
     */
    PutObjectResponse<T> putObject(T entity, PutRequest options)

    /**
     * Retrieve an object from the repository. The index will be chosen automatically based on the fields provided in the
     * input object.
     *
     * Usage Example:
     * <code>
     *     @MappedRepository
     *     public interface MyItemRepository extends MappedRepository<MyItem, String> {
     *     }
     *
     *     @Repository("my-table")
     *     public class MyItem {
     *         @Id
     *         @Column("partition-key")
     *         private UUID partitionKey;
     *
     *         @Column("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(UUID partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         MyItemRepository repository = client.repository(MyItemRepository.class);
     *
     *         UUID id = UUID.randomUUID();
     *
     *         MyItem itemToCreate = new MyItem();
     *         itemToCreate.setPartitionKey(id);
     *         itemToCreate.setCreationTime(Instant.now());
     *
     *         repository.putObject(itemToCreate);
     *
     *         // Wait a little bit, because getObject is eventually consistent by default.
     *         Thread.sleep(5_000);
     *
     *         MyItem itemToRetrieve = new MyItem();
     *         itemToRetrieve.setPartitionKey(id);
     *
     *         MyItem retrievedItem = repository.getObject(itemToRetrieve);
     *         assert retrievedItem.getCreationTime().isBetween(Instant.now().minus(1, MINUTE),
     *                                                          Instant.now());
     *     } catch (NoSuchEntityException e) {
     *         System.out.println("Item could not be found. Maybe we didn't wait long enough for consistency?");
     *         throw e;
     *     }
     * </code>
     */
    T getObject(ID id);

    /**
     * Retrieve an entity from the repository. The index will be chosen automatically based on the fields provided in the
     * input entity.
     *
     * This API allows specifying additional runtime-specific options via {@link GetRequest#putOption}, and retrieving runtime-
     * specific options via {@link GetObjectResponse#option}.
     *
     * Usage Example:
     * <code>
     *     @MappedRepository
     *     public interface MyItemRepository extends MappedRepository<MyItem, String> {
     *     }
     *
     *     @Repository("my-table")
     *     public class MyItem {
     *         @Id
     *         @Column("partition-key")
     *         private UUID partitionKey;
     *
     *         @Column("creation-time")
     *         private Instant creationTime;
     *
     *         public String getPartitionKey() { return this.partitionKey; }
     *         public Instant getCreationTime() { return this.creationTime; }
     *         public void setPartitionKey(UUID partitionKey) { this.partitionKey = partitionKey; }
     *         public void setCreationTime(Instant creationTime) { this.creationTime = creationTime; }
     *     }
     *
     *     try (DocumentClient client = DocumentClient.create(DynamoDbRuntime.class)) {
     *         MyItemRepository repository = client.repository(MyItemRepository.class);
     *
     *         UUID id = UUID.randomUUID();
     *
     *         MyItem itemToCreate = new MyItem();
     *         itemToCreate.setPartitionKey(id);
     *         itemToCreate.setCreationTime(Instant.now());
     *
     *         repository.putObject(itemToCreate);
     *
     *         MyItem itemToRetrieve = new MyItem();
     *         itemToRetrieve.setPartitionKey(id);
     *
     *         GetObjectResponse<MyItem> response =
     *             repository.getObject(itemToRetrieve,
     *                                  GetRequest.builder()
     *                                            .putOption(DynamoDbGetRequestOption.CONSISTENT, true)
     *                                            .build());
     *
     *         assert response.entity().getCreationTime().isBetween(Instant.now().minus(1, MINUTE),
     *                                                              Instant.now());
     *     }
     * </code>
     */
    GetObjectResponse<T> getObject(ID id, GetRequest options);
}

/**
 * An entity in a {@link DocumentRepository}. This is similar to a "row" in a traditional relational database.
 *
 * In the following repository, { "User ID": 1, "Username": "joe" } is an entity:
 *
 * <pre>
 * Repository: Users
 * | ------------------ |
 * | User ID | Username |
 * | ------------------ |
 * |       1 |      joe |
 * |       2 |     jane |
 * | ------------------ |
 * </pre>
 */
@ThreadSafe
public interface Entity {
    /**
     * Create a builder for configuring and creating an {@link Entity}.
     */
    static Entity.Builder builder();

    /**
     * Retrieve all {@link EntityValue}s in this entity.
     */
    Map<String, EntityValue> children();

    /**
     * Retrieve a specific {@link EntityValue} from this entity.
     */
    EntityValue child(String entityName);

    interface Builder {
        /**
         * Add a child to this entity. The methods accepting "Object", will be converted using the default
         * {@link EntityValueConverter}s.
         */
        Entity.Builder putChild(String entityName, Object value);
        Entity.Builder putChild(String entityName, Object value, EntityValueSchema schema);
        Entity.Builder putChild(String entityName, EntityValue value);
        Entity.Builder putChild(String entityName, EntityValue value, EntityValueSchema schema);
        Entity.Builder removeChild(String entityName);
        Entity.Builder clearChildren();

        /**
         * Add converters that should be used for this entity and its children. These converters are used with a higher
         * precedence than those configured in the {@link DocumentClient.Builder}.
         *
         * See {@link DocumentClient.Builder#addConverter} for example usage.
         */
        Entity.Builder converters(Iterable<? extends EntityValueConverter<?>> converters);
        Entity.Builder addConverter(EntityValueConverter<?> converter);
        Entity.Builder clearConverters();

        /**
         * Create an {@link Entity} using the current configuration on the builder.
         */
        Entity build();
    }
}

/**
 * The value within an {@link Entity}. In a traditional relational database, this would be analogous to a cell
 * in the table.
 *
 * In the following table, "joe" and "jane" are both entity values:
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
public interface EntityValue {
    /**
     * Create an {@link EntityValue} from the provided object.
     */
    static EntityValue from(Object object);

    /**
     * Create an {@link EntityValue} from the provided object, and associate this value with the provided
     * {@link EntityValueConverter}. This allows it to be immediately converted with {@link #as(Class)}.
     *
     * This is equivalent to {@code EntityValue.from(object).convertFromJavaType(converter)}.
     */
    static EntityValue from(Object object, EntityValueConverter<?> converter);

    /**
     * Create an {@link EntityValue} that represents the null type.
     */
    static EntityValue nullValue();

    /**
     * Retrieve the {@link EntityValueType} of this value.
     */
    EntityValueType type();

    /**
     * The {@code is*} methods can be used to check the underlying repository type of the entity value.
     *
     * If the type isn't known (eg. because it was created via {@link EntityValue#from(Object)}), {@link #isJavaType()}
     * will return true. Such types will be converted into repository-specific types by the document client before they are
     * persisted.
     */

    boolean isString();
    default boolean isNumber() { return isFloat() || isDouble() || isShort() || isInt() || isLong(); }
    boolean isFloat();
    boolean isDouble();
    boolean isShort();
    boolean isInt();
    boolean isLong();
    boolean isBytes();
    boolean isBoolean();
    boolean isNull();
    boolean isJavaType();

    boolean isList();
    boolean isEntity();

    /**
     * Convert this entity value into the requested Java type.
     *
     * This uses the {@link EntityValueConverter} configured on this type via
     * {@link #from(Object, EntityValueConverter)} or {@link #convertFromJavaType(EntityValueConverter)}.
     */
    <T> T as(Class<T> type);

    /**
     * The {@code as*} methods can be used to retrieve this value without the type-conversion overhead of {@link #as(Class)}.
     *
     * An exception will be thrown from these methods if the requested type does not match the actual underlying type. When
     * the type isn't know, the {@code is*} or {@link #type()} methods can be used to query the underlying type before
     * invoking these {@code as*} methods.
     */

    String asString();
    BigDecimal asNumber();
    float asFloat();
    double asDouble();
    short asShort();
    int asInt();
    long asLong();
    SdkBytes asBytes();
    Boolean asBoolean();
    Object asJavaType();

    List<EntityValue> asList();
    Entity asEntity();

    /**
     * Convert this entity value from a {@link EntityValueType#JAVA_TYPE} to a type that can be persisted in DynamoDB.
     *
     * This will throw an exception if {@link #isJavaType()} is false.
     */
    EntityValue convertFromJavaType(EntityValueConverter<?> converter);
}

/**
 * The underlying repository type of an {@link EntityValue}.
 */
@ThreadSafe
public enum EntityValueType {
    ENTITY,
    LIST,
    STRING,
    FLOAT,
    DOUBLE,
    SHORT,
    INT,
    LONG,
    BYTES,
    BOOLEAN,
    NULL,
    JAVA_TYPE
}

/**
 * The schema for a specific entity. This describes the entity's structure and which values it contains.
 *
 * This is mostly an implementation detail, and can be ignored except by developers interested in creating
 * {@link EntityValueConverter}s.
 */
@ThreadSafe
public interface EntitySchema {
    /**
     * Create a builder for configuring and creating an {@link EntitySchema}.
     */
    static EntitySchema.Builder builder();

    interface Builder {
        /**
         * The Java type of the entity that this schema represents.
         */
        EntitySchema.Builder javaType(Class<?> javaType);

        /**
         * The repository type of the entity that this schema represents.
         */
        EntitySchema.Builder entityValueType(EntityValueType entityValueType);

        /**
         * The converter that should be used for converting an entity conforming to this schema to/from the
         * repository-specific type.
         */
        EntitySchema.Builder converter(EntityValueConverter<?> converter);

        /**
         * Specify the child schemas that describe each child of this entity.
         */
        EntitySchema.Builder childSchemas(Map<String, EntityValueSchema> childSchemas);
        EntitySchema.Builder putChildSchema(String childName, EntityValueSchema childSchema);
        EntitySchema.Builder removeChildSchema(String childName);
        EntitySchema.Builder clearChildSchemas();

        /**
         * Create an {@link EntitySchema} using the current configuration on the builder.
         */
        EntitySchema build();
    }
}

/**
 * The schema for a specific entity value. This describes the entity child's structure, including what the Java-specific type
 * representation is for this value, etc.
 *
 * This is mostly an implementation detail, and can be ignored except by developers interested in creating
 * {@link EntityValueConverter}.
 */
@ThreadSafe
public interface EntityValueSchema {
    /**
     * Create a builder for configuring and creating an {@link EntityValueSchema}s.
     */
    static EntityValueSchema.Builder builder();

    interface Builder {
        /**
         * Specify the Java-specific type representation for this type.
         */
        EntityValueSchema.Builder javaType(Class<?> javaType);

        /**
         * The repository type of the value that this schema represents.
         */
        EntityValueSchema.Builder entityValueType(EntityValueType entityValueType);

        /**
         * The converter that should be used for converting a value conforming to this schema to/from the
         * repository-specific type.
         */
        EntityValueSchema.Builder converter(EntityValueConverter<?> converter);

        /**
         * Create an {@link EntityValueSchema} using the current configuration on the builder.
         */
        EntityValueSchema build();
    }
}