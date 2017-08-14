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

import static java.util.stream.Collectors.toMap;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.HASH;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.RANGE;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retry.RetryUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.BatchLoadRetryStrategy;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.BatchWriteRetryStrategy;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.ConsistentReads;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.SaveBehavior;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalOperator;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.awssdk.util.VersionInfo;

/**
 * Object mapper for domain-object interaction with DynamoDB.
 * <p>
 * To use, define a domain class that represents an item in a DynamoDB table and
 * annotate it with the annotations found in the
 * software.amazon.awssdk.services.dynamodbv2.datamodeling package. In order to allow the
 * mapper to correctly persist the data, each modeled property in the domain
 * class should be accessible via getter and setter methods, and each property
 * annotation should be either applied to the getter method or the class field.
 * A minimal example using getter annotations:
 *
 * <pre class="brush: java">
 * &#064;DynamoDBTable(tableName = &quot;TestTable&quot;)
 * public class TestClass {
 *
 *     private Long key;
 *     private double rangeKey;
 *     private Long version;
 *
 *     private Set&lt;Integer&gt; integerSetAttribute;
 *
 *     &#064;DynamoDBHashKey
 *     public Long getKey() {
 *         return key;
 *     }
 *
 *     public void setKey(Long key) {
 *         this.key = key;
 *     }
 *
 *     &#064;DynamoDBRangeKey
 *     public double getRangeKey() {
 *         return rangeKey;
 *     }
 *
 *     public void setRangeKey(double rangeKey) {
 *         this.rangeKey = rangeKey;
 *     }
 *
 *     &#064;DynamoDBAttribute(attributeName = &quot;integerSetAttribute&quot;)
 *     public Set&lt;Integer&gt; getIntegerAttribute() {
 *         return integerSetAttribute;
 *     }
 *
 *     public void setIntegerAttribute(Set&lt;Integer&gt; integerAttribute) {
 *         this.integerSetAttribute = integerAttribute;
 *     }
 *
 *     &#064;DynamoDBVersionAttribute
 *     public Long getVersion() {
 *         return version;
 *     }
 *
 *     public void setVersion(Long version) {
 *         this.version = version;
 *     }
 * }
 * </pre>
 * <p>
 * Save instances of annotated classes to DynamoDB, retrieve them, and delete
 * them using the {@link DynamoDbMapper} class, as in the following example.
 *
 * <pre class="brush: java">
 * DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
 * Long hashKey = 105L;
 * double rangeKey = 1.0d;
 * TestClass obj = mapper.load(TestClass.class, hashKey, rangeKey);
 * obj.getIntegerAttribute().add(42);
 * mapper.save(obj);
 * mapper.delete(obj);
 * </pre>
 * <p>
 * If you don't have your DynamoDB table set up yet, you can use
 * {@link DynamoDbMapper#generateCreateTableRequest(Class)} to construct the
 * {@link CreateTableRequest} for the table represented by your annotated class.
 *
 * <pre class="brush: java">
 * DynamoDBClient dynamoDBClient = new AmazonDynamoDBClient();
 * DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
 * CreateTableRequest req = mapper.generateCreateTableRequest(TestClass.class);
 * // Table provision throughput is still required since it cannot be specified in your POJO
 * req.setProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
 * // Fire off the CreateTableRequest using the low-level client
 * dynamoDBClient.createTable(req);
 * </pre>
 * <p>
 * When using the save, load, and delete methods, {@link DynamoDbMapper} will
 * throw {@link DynamoDbMappingException}s to indicate that domain classes are
 * incorrectly annotated or otherwise incompatible with this class. Service
 * exceptions will always be propagated as {@link SdkClientException}, and
 * DynamoDB-specific subclasses such as {@link ConditionalCheckFailedException}
 * will be used when possible.
 * <p>
 * This class is thread-safe and can be shared between threads. It's also very
 * lightweight, so it doesn't need to be.
 *
 * @see DynamoDbTable
 * @see DynamoDbHashKey
 * @see DynamoDbRangeKey
 * @see DynamoDbAutoGeneratedKey
 * @see DynamoDbAttribute
 * @see DynamoDbVersionAttribute
 * @see DynamoDbIgnore
 * @see DynamoDbMarshalling
 * @see DynamoDbMapperConfig
 */
public class DynamoDbMapper extends AbstractDynamoDbMapper {

    /**
     * The max back off time for batch get. The configuration for batch write
     * has been moved to DynamoDBMapperConfig
     */
    protected static final long MAX_BACKOFF_IN_MILLISECONDS = 1000 * 3L;
    /** The max number of items allowed in a BatchWrite request. */
    protected static final int MAX_ITEMS_PER_BATCH = 25;
    /**
     * This retry count is applicable only when every batch get item request
     * results in no data retrieved from server and the un processed keys is
     * same as request items
     */
    protected static final int BATCH_GET_MAX_RETRY_COUNT_ALL_KEYS = 5;
    /**
     * User agent for requests made using the {@link DynamoDbMapper}.
     */
    private static final String USER_AGENT =
        DynamoDbMapper.class.getName() + "/" + VersionInfo.SDK_VERSION;
    private static final String USER_AGENT_BATCH_OPERATION =
            DynamoDbMapper.class.getName() + "_batch_operation/" + VersionInfo.SDK_VERSION;
    private static final Logger log = LoggerFactory.getLogger(DynamoDbMapper.class);
    private final DynamoDBClient db;
    private final DynamoDbMapperModelFactory models;
    private final S3Link.Factory s3Links;
    private final AttributeTransformer transformer;

    /**
     * Constructs a new mapper with the service object given, using the default
     * configuration.
     *
     * @param dynamoDb
     *            The service object to use for all service calls.
     * @see DynamoDbMapperConfig#DEFAULT
     */
    public DynamoDbMapper(final DynamoDBClient dynamoDb) {
        this(dynamoDb, DynamoDbMapperConfig.DEFAULT, null, null);
    }


    /**
     * Constructs a new mapper with the service object and configuration given.
     *
     * @param dynamoDb
     *            The service object to use for all service calls.
     * @param config
     *            The default configuration to use for all service calls. It can
     *            be overridden on a per-operation basis.
     */
    public DynamoDbMapper(
            final DynamoDBClient dynamoDb,
            final DynamoDbMapperConfig config) {

        this(dynamoDb, config, null, null);
    }

    /**
     * Constructs a new mapper with the service object and S3 client cache
     * given, using the default configuration.
     *
     * @param ddb
     *            The service object to use for all service calls.
     * @param s3CredentialProvider
     *            The credentials provider for accessing S3.
     *            Relevant only if {@link S3Link} is involved.
     * @see DynamoDbMapperConfig#DEFAULT
     */
    public DynamoDbMapper(
            final DynamoDBClient ddb,
            final AwsCredentialsProvider s3CredentialProvider) {

        this(ddb, DynamoDbMapperConfig.DEFAULT, s3CredentialProvider);
    }

    /**
     * Constructs a new mapper with the given service object, configuration,
     * and transform hook.
     *
     * @param dynamoDb
     *            the service object to use for all service calls
     * @param config
     *            the default configuration to use for all service calls. It
     *            can be overridden on a per-operation basis
     * @param transformer
     *            The custom attribute transformer to invoke when serializing or
     *            deserializing an object.
     */
    public DynamoDbMapper(
            final DynamoDBClient dynamoDb,
            final DynamoDbMapperConfig config,
            final AttributeTransformer transformer) {

        this(dynamoDb, config, transformer, null);
    }

    /**
     * Constructs a new mapper with the service object, configuration, and S3
     * client cache given.
     *
     * @param dynamoDb
     *            The service object to use for all service calls.
     * @param config
     *            The default configuration to use for all service calls. It can
     *            be overridden on a per-operation basis.
     * @param s3CredentialProvider
     *            The credentials provider for accessing S3.
     *            Relevant only if {@link S3Link} is involved.
     */
    public DynamoDbMapper(
            final DynamoDBClient dynamoDb,
            final DynamoDbMapperConfig config,
            final AwsCredentialsProvider s3CredentialProvider) {

        this(dynamoDb, config, null, validate(s3CredentialProvider));
    }

    /**
     * Constructor with all parameters.
     *
     * @param dynamoDb
     *            The service object to use for all service calls.
     * @param config
     *            The default configuration to use for all service calls. It can
     *            be overridden on a per-operation basis.
     * @param transformer
     *            The custom attribute transformer to invoke when serializing or
     *            deserializing an object.
     * @param s3CredentialsProvider
     *            The credentials provider for accessing S3.
     *            Relevant only if {@link S3Link} is involved.
     */
    public DynamoDbMapper(
            final DynamoDBClient dynamoDb,
            final DynamoDbMapperConfig config,
            final AttributeTransformer transformer,
            final AwsCredentialsProvider s3CredentialsProvider) {
        super(config);

        failFastOnIncompatibleSubclass(getClass());

        this.db = dynamoDb;
        this.transformer = transformer;

        this.s3Links = S3Link.Factory.of(s3CredentialsProvider);

        this.models = StandardModelFactories.of(this.s3Links);
    }

    /**
     * Fail fast when trying to create a subclass of the DynamoDBMapper that
     * attempts to override one of the old {@code transformAttributes} methods.
     */
    private static void failFastOnIncompatibleSubclass(Class<?> clazz) {
        while (clazz != DynamoDbMapper.class) {
            Class<?>[] classOverride = new Class<?>[] {
                Class.class,
                Map.class
            };
            Class<?>[] nameOverride = new Class<?>[] {
                String.class,
                String.class,
                Map.class
            };

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals("transformAttributes")) {
                    Class<?>[] params = method.getParameterTypes();
                    if (Arrays.equals(params, classOverride)
                        || Arrays.equals(params, nameOverride)) {

                        throw new IllegalStateException(
                                "The deprecated transformAttributes method is "
                                + "no longer supported as of 1.9.0. Use an "
                                + "AttributeTransformer to inject custom "
                                + "attribute transformation logic.");
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Throws an exception if the given credentials provider is {@code null}.
     */
    private static AwsCredentialsProvider validate(
            final AwsCredentialsProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException(
                    "s3 credentials provider must not be null");
        }
        return provider;
    }

    private static <K, V> boolean isNullOrEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Determnes if any of the primary keys require auto-generation.
     */
    private static <T> boolean anyKeyGeneratable(
            final DynamoDbMapperTableModel<T> model,
            final T object,
            final SaveBehavior saveBehavior) {
        for (final DynamoDbMapperFieldModel<T, Object> field : model.keys()) {
            if (canGenerate(model, object, saveBehavior, field)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the mapping value can be auto-generated.
     */
    private static <T> boolean canGenerate(
            final DynamoDbMapperTableModel<T> model,
            final T object,
            final SaveBehavior saveBehavior,
            final DynamoDbMapperFieldModel<T, Object> field) {
        if (field.getGenerateStrategy() == null) {
            return false;
        } else if (field.getGenerateStrategy() == DynamoDbAutoGenerateStrategy.ALWAYS) {
            return true;
        } else if (field.get(object) != null) {
            return false;
        } else if (field.keyType() != null || field.indexed()) {
            return true;
        } else if (saveBehavior == SaveBehavior.CLOBBER) {
            return true;
        } else if (saveBehavior == SaveBehavior.UPDATE) {
            return true;
        } else if (anyKeyGeneratable(model, object, saveBehavior)) {
            return true;
        }
        return false;
    }

    /**
     * Utility method for checking the validity of both hash and range key
     * conditions. It also tries to infer the correct index name from the POJO
     * annotation, if such information is not directly specified by the user.
     *
     * @param clazz
     *            The domain class of the queried items.
     * @param queryRequest
     *            The QueryRequest object to be sent to service.
     * @param hashKeyConditions
     *            All the hash key EQ conditions extracted from the POJO object.
     *            The mapper will choose one of them that could be applied together with
     *            the user-specified (if any) index name and range key conditions. Or it
     *            throws error if more than one conditions are applicable for the query.
     * @param rangeKeyConditions
     *            The range conditions specified by the user. We currently only
     *            allow at most one range key condition.
     */
    private static <T> QueryRequest processKeyConditions(
            QueryRequest queryRequest,
            final DynamoDbQueryExpression<T> expression,
            final DynamoDbMapperTableModel<T> model) {
        // Hash key (primary or index) condition
        final Map<String, Condition> hashKeyConditions = new LinkedHashMap<String, Condition>();
        if (expression.getHashKeyValues() != null) {
            for (final DynamoDbMapperFieldModel<T, Object> field : model.fields()) {
                if (field.keyType() == HASH || !field.globalSecondaryIndexNames(HASH).isEmpty()) {
                    final Object value = field.get(expression.getHashKeyValues());
                    if (value != null) {
                        hashKeyConditions.put(field.name(), field.eq(value));
                    }
                }
            }
        }

        // Range key (primary or index) conditions
        final Map<String, Condition> rangeKeyConditions = expression.getRangeKeyConditions();

        // There should be least one hash key condition.
        final String keyCondExpression = queryRequest.keyConditionExpression();
        if (keyCondExpression == null) {
            if (isNullOrEmpty(hashKeyConditions)) {
                throw new IllegalArgumentException("Illegal query expression: No hash key condition is found in the query");
            }
        } else {
            if (!isNullOrEmpty(hashKeyConditions)) {
                throw new IllegalArgumentException("Illegal query expression: Either the hash key conditions or the key " +
                                                   "condition expression must be specified but not both.");
            }
            if (!isNullOrEmpty(rangeKeyConditions)) {
                throw new IllegalArgumentException("Illegal query expression: The range key conditions can only be specified " +
                                                   "when the key condition expression is not specified.");
            }
            // key condition expression is in use
            return queryRequest;
        }
        // We don't allow multiple range key conditions.
        if (rangeKeyConditions != null && rangeKeyConditions.size() > 1) {
            throw new IllegalArgumentException(
                    "Illegal query expression: Conditions on multiple range keys ("
                    + rangeKeyConditions.keySet().toString()
                    + ") are found in the query. DynamoDB service only accepts up to ONE range key condition.");
        }
        final boolean hasRangeKeyCondition = (rangeKeyConditions != null)
                                             && (!rangeKeyConditions.isEmpty());
        final String userProvidedIndexName = queryRequest.indexName();
        final String primaryHashKeyName = model.hashKey().name();

        // First collect the names of all the global/local secondary indexes that could be applied to this query.
        // If the user explicitly specified an index name, we also need to
        //   1) check the index is applicable for both hash and range key conditions
        //   2) choose one hash key condition if there are more than one of them
        boolean hasPrimaryHashKeyCondition = false;
        final Map<String, Set<String>> annotatedGsisOnHashKeys = new HashMap<String, Set<String>>();
        String hashKeyNameForThisQuery = null;

        boolean hasPrimaryRangeKeyCondition = false;
        final Set<String> annotatedLsisOnRangeKey = new HashSet<String>();
        final Set<String> annotatedGsisOnRangeKey = new HashSet<String>();

        // Range key condition
        String rangeKeyNameForThisQuery = null;
        if (hasRangeKeyCondition) {
            for (String rangeKeyName : rangeKeyConditions.keySet()) {
                rangeKeyNameForThisQuery = rangeKeyName;

                final DynamoDbMapperFieldModel<T, Object> rk = model.field(rangeKeyName);

                if (rk.keyType() == RANGE) {
                    hasPrimaryRangeKeyCondition = true;
                }

                annotatedLsisOnRangeKey.addAll(rk.localSecondaryIndexNames());
                annotatedGsisOnRangeKey.addAll(rk.globalSecondaryIndexNames(RANGE));
            }

            if (!hasPrimaryRangeKeyCondition
                && annotatedLsisOnRangeKey.isEmpty()
                && annotatedGsisOnRangeKey.isEmpty()) {
                throw new DynamoDbMappingException(
                        "The query contains a condition on a range key (" +
                        rangeKeyNameForThisQuery + ") " +
                        "that is not annotated with either @DynamoDBRangeKey or @DynamoDBIndexRangeKey.");
            }
        }

        final boolean userProvidedLsiWithRangeKeyCondition = (userProvidedIndexName != null)
                                                             && (annotatedLsisOnRangeKey.contains(userProvidedIndexName));
        final boolean hashOnlyLsiQuery = (userProvidedIndexName != null)
                                         && (!hasRangeKeyCondition)
                                         && model.localSecondaryIndex(userProvidedIndexName) != null;
        final boolean userProvidedLsi = userProvidedLsiWithRangeKeyCondition || hashOnlyLsiQuery;

        final boolean userProvidedGsiWithRangeKeyCondition = (userProvidedIndexName != null)
                                                             && (annotatedGsisOnRangeKey.contains(userProvidedIndexName));
        final boolean hashOnlyGsiQuery = (userProvidedIndexName != null)
                                         && (!hasRangeKeyCondition)
                                         && model.globalSecondaryIndex(userProvidedIndexName) != null;
        final boolean userProvidedGsi = userProvidedGsiWithRangeKeyCondition || hashOnlyGsiQuery;

        if (userProvidedLsi && userProvidedGsi) {
            throw new DynamoDbMappingException(
                    "Invalid query: " +
                    "Index \"" + userProvidedIndexName + "\" " +
                    "is annotateded as both a LSI and a GSI for attribute.");
        }

        // Hash key conditions
        for (String hashKeyName : hashKeyConditions.keySet()) {
            if (hashKeyName.equals(primaryHashKeyName)) {
                hasPrimaryHashKeyCondition = true;
            }

            final DynamoDbMapperFieldModel<T, Object> hk = model.field(hashKeyName);

            Collection<String> annotatedGsiNames = hk.globalSecondaryIndexNames(HASH);
            annotatedGsisOnHashKeys.put(hashKeyName,
                                        annotatedGsiNames == null ? new HashSet<>() : new HashSet<>(annotatedGsiNames));

            // Additional validation if the user provided an index name.
            if (userProvidedIndexName != null) {
                boolean foundHashKeyConditionValidWithUserProvidedIndex = false;
                if (userProvidedLsi && hashKeyName.equals(primaryHashKeyName)) {
                    // found an applicable hash key condition (primary hash + LSI range)
                    foundHashKeyConditionValidWithUserProvidedIndex = true;
                } else if (userProvidedGsi &&
                           annotatedGsiNames != null && annotatedGsiNames.contains(userProvidedIndexName)) {
                    // found an applicable hash key condition (GSI hash + range)
                    foundHashKeyConditionValidWithUserProvidedIndex = true;
                }
                if (foundHashKeyConditionValidWithUserProvidedIndex) {
                    if (hashKeyNameForThisQuery != null) {
                        throw new IllegalArgumentException(
                                "Ambiguous query expression: More than one hash key EQ conditions (" +
                                hashKeyNameForThisQuery + ", " + hashKeyName +
                                ") are applicable to the specified index ("
                                + userProvidedIndexName + "). " +
                                "Please provide only one of them in the query expression.");
                    } else {
                        // found an applicable hash key condition
                        hashKeyNameForThisQuery = hashKeyName;
                    }
                }
            }
        }

        // Collate all the key conditions
        Map<String, Condition> keyConditions = new HashMap<String, Condition>();

        // With user-provided index name
        if (userProvidedIndexName != null) {
            if (hasRangeKeyCondition
                && (!userProvidedLsi)
                && (!userProvidedGsi)) {
                throw new IllegalArgumentException(
                        "Illegal query expression: No range key condition is applicable to the specified index ("
                        + userProvidedIndexName + "). ");
            }
            if (hashKeyNameForThisQuery == null) {
                throw new IllegalArgumentException(
                        "Illegal query expression: No hash key condition is applicable to the specified index ("
                        + userProvidedIndexName + "). ");
            }

            keyConditions.put(hashKeyNameForThisQuery, hashKeyConditions.get(hashKeyNameForThisQuery));
            if (hasRangeKeyCondition) {
                keyConditions.putAll(rangeKeyConditions);
            }
        } else {
            // Infer the index name by finding the index shared by both hash and range key annotations.
            if (hasRangeKeyCondition) {
                String inferredIndexName = null;
                hashKeyNameForThisQuery = null;
                if (hasPrimaryHashKeyCondition && hasPrimaryRangeKeyCondition) {
                    // Found valid query: primary hash + range key conditions
                    hashKeyNameForThisQuery = primaryHashKeyName;
                } else {
                    // Intersect the set of all the indexes applicable to the range key
                    // with the set of indexes applicable to each hash key condition.
                    for (Map.Entry<String, Set<String>> indexedHashKeys : annotatedGsisOnHashKeys.entrySet()) {
                        String hashKeyName = indexedHashKeys.getKey();
                        Set<String> annotatedGsisOnHashKey = indexedHashKeys.getValue();

                        boolean foundValidQueryExpressionWithInferredIndex = false;
                        String indexNameInferredByThisHashKey = null;
                        if (hashKeyName.equals(primaryHashKeyName)) {
                            if (annotatedLsisOnRangeKey.size() == 1) {
                                // Found valid query (Primary hash + LSI range conditions)
                                foundValidQueryExpressionWithInferredIndex = true;
                                indexNameInferredByThisHashKey = annotatedLsisOnRangeKey.iterator().next();
                            }
                        }

                        // We don't need the data in annotatedGSIsOnHashKeys afterwards,
                        // so it's safe to do the intersection in-place.
                        annotatedGsisOnHashKey.retainAll(annotatedGsisOnRangeKey);
                        if (annotatedGsisOnHashKey.size() == 1) {
                            // Found valid query (Hash + range conditions on a GSI)
                            if (foundValidQueryExpressionWithInferredIndex) {
                                hashKeyNameForThisQuery = hashKeyName;
                                inferredIndexName = indexNameInferredByThisHashKey;
                            }

                            foundValidQueryExpressionWithInferredIndex = true;
                            indexNameInferredByThisHashKey = annotatedGsisOnHashKey.iterator().next();
                        }

                        if (foundValidQueryExpressionWithInferredIndex) {
                            if (hashKeyNameForThisQuery != null) {
                                throw new IllegalArgumentException(
                                        "Ambiguous query expression: Found multiple valid queries: " +
                                        "(Hash: \"" + hashKeyNameForThisQuery + "\", Range: \"" + rangeKeyNameForThisQuery +
                                        "\", Index: \"" + inferredIndexName + "\") and " +
                                        "(Hash: \"" + hashKeyName + "\", Range: \"" + rangeKeyNameForThisQuery +
                                        "\", Index: \"" + indexNameInferredByThisHashKey + "\").");
                            } else {
                                hashKeyNameForThisQuery = hashKeyName;
                                inferredIndexName = indexNameInferredByThisHashKey;
                            }
                        }
                    }
                }

                if (hashKeyNameForThisQuery != null) {
                    keyConditions.put(hashKeyNameForThisQuery, hashKeyConditions.get(hashKeyNameForThisQuery));
                    keyConditions.putAll(rangeKeyConditions);
                    queryRequest = queryRequest.toBuilder().indexName(inferredIndexName).build();
                } else {
                    throw new IllegalArgumentException(
                            "Illegal query expression: Cannot infer the index name from the query expression.");
                }

            } else {
                // No range key condition is specified.
                if (hashKeyConditions.size() > 1) {
                    if (hasPrimaryHashKeyCondition) {
                        keyConditions.put(primaryHashKeyName, hashKeyConditions.get(primaryHashKeyName));
                    } else {
                        throw new IllegalArgumentException(
                                "Ambiguous query expression: More than one index hash key EQ conditions (" +
                                hashKeyConditions.keySet() + ") are applicable to the query. Please provide only one of them " +
                                "in the query expression, or specify the appropriate index name.");
                    }

                } else {
                    // Only one hash key condition
                    Entry<String, Set<String>> entry = annotatedGsisOnHashKeys.entrySet().iterator().next();
                    String hashKeyName = entry.getKey();
                    Set<String> annotatedGsisOnHashkey = entry.getValue();
                    if (!hasPrimaryHashKeyCondition) {
                        if (annotatedGsisOnHashkey.size() == 1) {
                            // Set the index if the index hash key is only annotated with one GSI.
                            queryRequest = queryRequest.toBuilder().indexName(annotatedGsisOnHashkey.iterator().next()).build();
                        } else if (annotatedGsisOnHashkey.size() > 1) {
                            throw new IllegalArgumentException(
                                    "Ambiguous query expression: More than one GSIs (" +
                                    annotatedGsisOnHashkey +
                                    ") are applicable to the query. " +
                                    "Please specify one of them in your query expression.");
                        } else {
                            throw new IllegalArgumentException(
                                    "Illegal query expression: No GSI is found in the @DynamoDBIndexHashKey annotation for " +
                                    "attribute \"" + hashKeyName + "\".");
                        }
                    }
                    keyConditions.putAll(hashKeyConditions);
                }

            }
        }

        return queryRequest.toBuilder().keyConditions(keyConditions).build();
    }

    /**
     * Returns a new map object that merges the two sets of expected value
     * conditions (user-specified or imposed by the internal implementation of
     * DynamoDBMapper). Internal assertion on an attribute will be overridden by
     * any user-specified condition on the same attribute.
     * <p>
     * Exception is thrown if the two sets of conditions cannot be combined
     * together.
     */
    private static Map<String, ExpectedAttributeValue> mergeExpectedAttributeValueConditions(
            Map<String, ExpectedAttributeValue> internalAssertions,
            Map<String, ExpectedAttributeValue> userProvidedConditions,
            String userProvidedConditionOperator) {
        // If any of the condition map is null, simply return a copy of the other one.
        if ((internalAssertions == null || internalAssertions.isEmpty())
            && (userProvidedConditions == null || userProvidedConditions.isEmpty())) {
            return null;
        } else if (internalAssertions == null) {
            return new HashMap<>(userProvidedConditions);
        } else if (userProvidedConditions == null) {
            return new HashMap<>(internalAssertions);
        }

        // Start from a copy of the internal conditions
        Map<String, ExpectedAttributeValue> mergedExpectedValues =
                new HashMap<String, ExpectedAttributeValue>(internalAssertions);

        // Remove internal conditions that are going to be overlaid by user-provided ones.
        for (String attrName : userProvidedConditions.keySet()) {
            mergedExpectedValues.remove(attrName);
        }

        // All the generated internal conditions must be joined by AND.
        // Throw an exception if the user specifies an OR operator, and that the
        // internal conditions are not totally overlaid by the user-provided
        // ones.
        if (ConditionalOperator.OR.toString().equals(userProvidedConditionOperator)
            && !mergedExpectedValues.isEmpty()) {
            throw new IllegalArgumentException("Unable to assert the value of the fields " + mergedExpectedValues.keySet() +
                                               ", since the expected value conditions cannot be combined with user-specified " +
                                               "conditions joined by \"OR\". You can use SaveBehavior.CLOBBER to " +
                                               "skip the assertion on these fields.");
        }

        mergedExpectedValues.putAll(userProvidedConditions);

        return mergedExpectedValues;
    }

    static <X extends AmazonWebServiceRequest> X applyUserAgent(X request) {
        request.getRequestClientOptions().appendUserAgent(USER_AGENT);
        return request;
    }

    static <X extends AmazonWebServiceRequest> X applyBatchOperationUserAgent(X request) {
        request.getRequestClientOptions().appendUserAgent(USER_AGENT_BATCH_OPERATION);
        return request;
    }

    /**
     * Batch pause.
     */
    private static void pause(long delay) {
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SdkClientException(e.getMessage(), e);
        }
    }

    @Override
    public <T extends Object> DynamoDbMapperTableModel<T> getTableModel(Class<T> clazz, DynamoDbMapperConfig config) {
        return this.models.getTableFactory(config).getTable(clazz);
    }

    @Override
    public <T extends Object> T load(T keyObject, DynamoDbMapperConfig config) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) keyObject.getClass();

        config = mergeConfig(config);
        final DynamoDbMapperTableModel<T> model = getTableModel(clazz, config);

        String tableName = getTableName(clazz, keyObject, config);

        GetItemRequest.Builder rqBuilder = GetItemRequest.builder();

        Map<String, AttributeValue> key = model.convertKey(keyObject);

        rqBuilder.key(key);
        rqBuilder.tableName(tableName);
        rqBuilder.consistentRead(config.getConsistentReads() == ConsistentReads.CONSISTENT);

        GetItemRequest rq = rqBuilder.build();

        GetItemResponse item = db.getItem(applyUserAgent(rq));
        Map<String, AttributeValue> itemAttributes = item.item();
        if (itemAttributes == null) {
            return null;
        }

        T object = privateMarshallIntoObject(
                toParameters(itemAttributes, clazz, tableName, config));

        return object;
    }

    @Override
    public <T extends Object> T load(Class<T> clazz, Object hashKey, Object rangeKey, DynamoDbMapperConfig config) {
        config = mergeConfig(config);
        final DynamoDbMapperTableModel<T> model = getTableModel(clazz, config);
        T keyObject = model.createKey(hashKey, rangeKey);
        return load(keyObject, config);
    }

    @Override
    public <T> T marshallIntoObject(Class<T> clazz, Map<String, AttributeValue> itemAttributes, DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        String tableName = getTableName(clazz, config);

        return privateMarshallIntoObject(
                toParameters(itemAttributes, clazz, tableName, config));
    }

    /**
     * The one true implementation of marshallIntoObject.
     */
    private <T> T privateMarshallIntoObject(
            AttributeTransformer.Parameters<T> parameters) {

        Class<T> clazz = parameters.modelClass();
        Map<String, AttributeValue> values = untransformAttributes(parameters);

        final DynamoDbMapperTableModel<T> model = getTableModel(clazz, parameters.mapperConfig());
        return model.unconvert(values);
    }

    @Override
    public <T> List<T> marshallIntoObjects(Class<T> clazz, List<Map<String, AttributeValue>> itemAttributes,
                                           DynamoDbMapperConfig config) {
        // If config is used in the future, be sure to mergeConfig.
        // config = mergeConfig(config);

        List<T> result = new ArrayList<T>(itemAttributes.size());
        for (Map<String, AttributeValue> item : itemAttributes) {
            result.add(marshallIntoObject(clazz, item));
        }
        return result;
    }

    /**
     * A replacement for {@link #marshallIntoObjects(Class, List)} that takes
     * an extra set of parameters to be tunneled through to
     * {@code privateMarshalIntoObject} (if nothing along the way is
     * overridden). It's package-private because some of the Paginated*List
     * classes call back into it, but final because no one, even in this
     * package, should ever override it.
     */
    final <T> List<T> marshallIntoObjects(
            final List<AttributeTransformer.Parameters<T>> parameters) {
        List<T> result = new ArrayList<T>(parameters.size());

        for (AttributeTransformer.Parameters<T> entry : parameters) {
            result.add(privateMarshallIntoObject(entry));
        }

        return result;
    }

    @Override
    public <T extends Object> void save(T object,
                                        DynamoDbSaveExpression saveExpression,
                                        final DynamoDbMapperConfig config) {
        final DynamoDbMapperConfig finalConfig = mergeConfig(config);

        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) object.getClass();
        String tableName = getTableName(clazz, object, finalConfig);

        final DynamoDbMapperTableModel<T> model = getTableModel(clazz, finalConfig);

        /*
         * We force a putItem request instead of updateItem request either when
         * CLOBBER is configured, or part of the primary key of the object needs
         * to be auto-generated.
         */
        boolean forcePut = (finalConfig.saveBehavior() == SaveBehavior.CLOBBER)
                           || anyKeyGeneratable(model, object, finalConfig.saveBehavior());

        SaveObjectHandler saveObjectHandler;

        if (forcePut) {
            saveObjectHandler = this.new SaveObjectHandler(clazz, object,
                                                           tableName, finalConfig, saveExpression) {

                @Override
                protected void onPrimaryKeyAttributeValue(String attributeName,
                                                          AttributeValue keyAttributeValue) {
                    /* Treat key values as common attribute value updates. */
                    getAttributeValueUpdates().put(attributeName,
                                                   AttributeValueUpdate.builder()
                                                           .value(keyAttributeValue)
                                                           .action("PUT").build());
                }

                /* Use default implementation of onNonKeyAttribute(...) */

                @Override
                protected void onNullNonKeyAttribute(String attributeName) {
                    /* When doing a force put, we can safely ignore the null-valued attributes. */
                    return;
                }

                @Override
                protected void executeLowLevelRequest() {
                    /* Send a putItem request. */
                    doPutItem();
                }
            };
        } else {
            saveObjectHandler = this.new SaveObjectHandler(clazz, object,
                                                           tableName, finalConfig, saveExpression) {

                @Override
                protected void onPrimaryKeyAttributeValue(String attributeName,
                                                          AttributeValue keyAttributeValue) {
                    /* Put it in the key collection which is later used in the updateItem request. */
                    getPrimaryKeyAttributeValues().put(attributeName, keyAttributeValue);
                }


                @Override
                protected void onNonKeyAttribute(String attributeName,
                                                 AttributeValue currentValue) {
                    /* If it's a set attribute and the mapper is configured with APPEND_SET,
                     * we do an "ADD" update instead of the default "PUT".
                     */
                    if (localSaveBehavior() == SaveBehavior.APPEND_SET) {
                        if (currentValue.bs() != null
                            || currentValue.ns() != null
                            || currentValue.ss() != null) {
                            getAttributeValueUpdates().put(
                                    attributeName,
                                    AttributeValueUpdate.builder().value(
                                            currentValue).action("ADD").build());
                            return;
                        }
                    }
                    /* Otherwise, we do the default "PUT" update. */
                    super.onNonKeyAttribute(attributeName, currentValue);
                }

                @Override
                protected void onNullNonKeyAttribute(String attributeName) {
                    /*
                     * If UPDATE_SKIP_NULL_ATTRIBUTES or APPEND_SET is
                     * configured, we don't delete null value attributes.
                     */
                    if (localSaveBehavior() == SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES
                        || localSaveBehavior() == SaveBehavior.APPEND_SET) {
                        return;
                    } else {
                        /* Delete attributes that are set as null in the object. */
                        getAttributeValueUpdates()
                                .put(attributeName,
                                     AttributeValueUpdate.builder()
                                             .action("DELETE")
                                             .build());
                    }
                }

                @Override
                protected void executeLowLevelRequest() {
                    UpdateItemResponse updateItemResult = doUpdateItem();

                    // The UpdateItem request is specified to return ALL_NEW
                    // attributes of the affected item. So if the returned
                    // UpdateItemResponse does not include any ReturnedAttributes,
                    // it indicates the UpdateItem failed silently (e.g. the
                    // key-only-put nightmare -
                    // https://forums.aws.amazon.com/thread.jspa?threadID=86798&tstart=25),
                    // in which case we should re-send a PutItem
                    // request instead.
                    if (updateItemResult.attributes() == null
                        || updateItemResult.attributes().isEmpty()) {
                        // Before we proceed with PutItem, we need to put all
                        // the key attributes (prepared for the
                        // UpdateItemRequest) into the AttributeValueUpdates
                        // collection.
                        for (String keyAttributeName : getPrimaryKeyAttributeValues().keySet()) {
                            AttributeValueUpdate value = AttributeValueUpdate.builder()
                                    .value(getPrimaryKeyAttributeValues().get(keyAttributeName))
                                    .action("PUT").build();
                            getAttributeValueUpdates().put(keyAttributeName, value);
                        }

                        doPutItem();
                    }
                }
            };
        }

        saveObjectHandler.execute();
    }

    @Override
    public <T> void delete(T object, DynamoDbDeleteExpression deleteExpression, DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) object.getClass();
        final DynamoDbMapperTableModel<T> model = getTableModel(clazz, config);

        String tableName = getTableName(clazz, object, config);

        Map<String, AttributeValue> key = model.convertKey(object);

        /*
         * If there is a version field, make sure we assert its value. If the
         * version field is null (only should happen in unusual circumstances),
         * pretend it doesn't have a version field after all.
         */
        Map<String, ExpectedAttributeValue> internalAssertions = new HashMap<String, ExpectedAttributeValue>();
        if (config.saveBehavior() != SaveBehavior.CLOBBER && model.versioned()) {
            for (final DynamoDbMapperFieldModel<T, Object> field : model.versions()) {
                final AttributeValue current = field.getAndConvert(object);
                if (current == null) {
                    internalAssertions.put(field.name(), ExpectedAttributeValue.builder().exists(false).build());
                } else {
                    internalAssertions.put(field.name(), ExpectedAttributeValue.builder().exists(true).value(current).build());
                }
                break;
            }
        }

        DeleteItemRequest req = DeleteItemRequest.builder()
                .key(key)
                .tableName(tableName)
                .expected(internalAssertions)
                .build();

        if (deleteExpression != null) {
            String conditionalExpression = deleteExpression.getConditionExpression();

            if (conditionalExpression != null) {
                if (!internalAssertions.isEmpty()) {
                    throw new SdkClientException(
                            "Condition Expressions cannot be used if a versioned attribute is present");
                }

                req = req.toBuilder()
                        .conditionExpression(conditionalExpression)
                        .expressionAttributeNames(
                                deleteExpression.getExpressionAttributeNames())
                        .expressionAttributeValues(
                                deleteExpression.getExpressionAttributeValues())
                        .build();
            }

            req = req.toBuilder()
                    .expected(
                    mergeExpectedAttributeValueConditions(internalAssertions,
                                                          deleteExpression.getExpected(),
                                                          deleteExpression.getConditionalOperator()))
                     .conditionalOperator(
                             deleteExpression.getConditionalOperator())
                    .build();

        }
        db.deleteItem(applyUserAgent(req));
    }

    @Override
    public List<FailedBatch> batchWrite(Iterable<? extends Object> objectsToWrite,
                                        Iterable<? extends Object> objectsToDelete,
                                        DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        List<FailedBatch> totalFailedBatches = new LinkedList<FailedBatch>();

        StringListMap<WriteRequest> requestItems = new StringListMap<WriteRequest>();

        List<ValueUpdate> inMemoryUpdates = new LinkedList<ValueUpdate>();
        for (Object toWrite : objectsToWrite) {
            Class<Object> clazz = (Class<Object>) toWrite.getClass();
            String tableName = getTableName(clazz, toWrite, config);

            Map<String, AttributeValue> attributeValues = new HashMap<String, AttributeValue>();

            // Look at every getter and construct a value object for it
            final DynamoDbMapperTableModel<Object> model = getTableModel(clazz, config);
            for (final DynamoDbMapperFieldModel<Object, Object> field : model.fields()) {
                AttributeValue currentValue;
                if (canGenerate(model, toWrite, config.saveBehavior(), field) && !field.versioned()) {
                    currentValue = field.convert(field.generate(field.get(toWrite)));
                    inMemoryUpdates.add(new ValueUpdate(field, currentValue, toWrite));
                } else {
                    currentValue = field.convert(field.get(toWrite));
                }
                if (currentValue != null) {
                    attributeValues.put(field.name(), currentValue);
                }
            }

            if (!requestItems.containsKey(tableName)) {
                requestItems.put(tableName, new LinkedList<WriteRequest>());
            }

            AttributeTransformer.Parameters<?> parameters =
                    toParameters(attributeValues, clazz, tableName, config);

            requestItems.add(tableName,
                    WriteRequest.builder()
                            .putRequest(PutRequest.builder()
                                    .item(transformAttributes(parameters))
                                    .build())
                            .build());
        }

        for (Object toDelete : objectsToDelete) {
            Class<Object> clazz = (Class<Object>) toDelete.getClass();

            String tableName = getTableName(clazz, toDelete, config);
            final DynamoDbMapperTableModel<Object> model = getTableModel(clazz, config);

            Map<String, AttributeValue> key = model.convertKey(toDelete);

            requestItems.add(tableName, WriteRequest.builder()
                    .deleteRequest(DeleteRequest.builder()
                            .key(key)
                            .build())
                    .build());
        }

        // Break into chunks of 25 items and make service requests to DynamoDB
        for (final StringListMap<WriteRequest> batch : requestItems.subMaps(MAX_ITEMS_PER_BATCH, true)) {
            List<FailedBatch> failedBatches = writeOneBatch(batch, config.batchWriteRetryStrategy());
            totalFailedBatches.addAll(failedBatches);

            // If contains throttling exception, we do a backoff
            if (containsThrottlingException(failedBatches)) {
                pause(config.batchWriteRetryStrategy().getDelayBeforeRetryUnprocessedItems(
                        Collections.unmodifiableMap(batch), 0));
            }
        }

        // Once the entire batch is processed, update assigned keys in memory
        for (ValueUpdate update : inMemoryUpdates) {
            update.apply();
        }

        return totalFailedBatches;
    }

    /**
     * Process one batch of requests(max 25). It will divide the batch if
     * receives request too large exception(the total size of the request is beyond 1M).
     */
    private List<FailedBatch> writeOneBatch(
            StringListMap<WriteRequest> batch,
            BatchWriteRetryStrategy batchWriteRetryStrategy) {

        List<FailedBatch> failedBatches = new LinkedList<FailedBatch>();
        FailedBatch failedBatch = doBatchWriteItemWithRetry(batch, batchWriteRetryStrategy);

        if (failedBatch != null) {
            // If the exception is request entity too large, we divide the batch
            // into smaller parts.

            if (failedBatch.isRequestEntityTooLarge()) {

                // If only one item left, the item size must beyond 64k, which
                // exceedes the limit.

                if (failedBatch.size() == 1) {
                    failedBatches.add(failedBatch);
                } else {
                    for (final StringListMap<WriteRequest> subBatch : batch.subMaps(2, false)) {
                        failedBatches.addAll(writeOneBatch(subBatch, batchWriteRetryStrategy));
                    }
                }

            } else {
                failedBatches.add(failedBatch);
            }

        }
        return failedBatches;
    }

    /**
     * Check whether there are throttling exception in the failed batches.
     */
    private boolean containsThrottlingException(List<FailedBatch> failedBatches) {
        for (FailedBatch failedBatch : failedBatches) {
            if (failedBatch.isThrottling()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Continue trying to process the batch and retry on UnproccessedItems as
     * according to the specified BatchWriteRetryStrategy
     */
    private FailedBatch doBatchWriteItemWithRetry(
            Map<String, List<WriteRequest>> batch,
            BatchWriteRetryStrategy batchWriteRetryStrategy) {

        BatchWriteItemResponse result = null;
        int retries = 0;
        int maxRetries = batchWriteRetryStrategy
                .maxRetryOnUnprocessedItems(Collections
                                                       .unmodifiableMap(batch));

        FailedBatch failedBatch = null;
        Map<String, List<WriteRequest>> pendingItems = batch;

        while (true) {
            try {
                result = db.batchWriteItem(applyBatchOperationUserAgent(
                        BatchWriteItemRequest.builder().requestItems(pendingItems).build()));
            } catch (Exception e) {
                failedBatch = new FailedBatch();
                failedBatch.setUnprocessedItems(pendingItems);
                failedBatch.setException(e);
                return failedBatch;
            }
            pendingItems = result.unprocessedItems();

            if (pendingItems.size() > 0) {

                // return pendingItems as a FailedBatch if we have exceeded max retry
                if (maxRetries >= 0 && retries >= maxRetries) {
                    failedBatch = new FailedBatch();
                    failedBatch.setUnprocessedItems(pendingItems);
                    failedBatch.setException(null);
                    return failedBatch;
                }

                pause(batchWriteRetryStrategy.getDelayBeforeRetryUnprocessedItems(
                        Collections.unmodifiableMap(pendingItems), retries));
                retries++;
            } else {
                break;
            }
        }
        return failedBatch;
    }

    @Override
    public Map<String, List<Object>> batchLoad(Iterable<? extends Object> itemsToGet, DynamoDbMapperConfig config) {
        config = mergeConfig(config);
        boolean consistentReads = (config.getConsistentReads() == ConsistentReads.CONSISTENT);

        if (itemsToGet == null) {
            return new HashMap<>();
        }

        Map<String, Collection<Map<String, AttributeValue>>> requestItemLists = new HashMap<>();
        Map<String, Class<?>> classesByTableName = new HashMap<String, Class<?>>();
        Map<String, List<Object>> resultSet = new HashMap<String, List<Object>>();
        int count = 0;

        for (Object keyObject : itemsToGet) {
            Class<Object> clazz = (Class<Object>) keyObject.getClass();
            final DynamoDbMapperTableModel<Object> model = getTableModel(clazz, config);

            String tableName = getTableName(clazz, keyObject, config);
            classesByTableName.put(tableName, clazz);

            requestItemLists.computeIfAbsent(tableName, ignored -> new LinkedList<>()).add(model.convertKey(keyObject));

            // Reach the maximum number which can be handled in a single batchGet
            if (++count == 100) {
                Map<String, KeysAndAttributes> requestItems = batchRequestItems(consistentReads, requestItemLists);
                processBatchGetRequest(classesByTableName, requestItems, resultSet, config);
                requestItemLists.clear();
                count = 0;
            }
        }

        if (count > 0) {
            Map<String, KeysAndAttributes> requestItems = batchRequestItems(consistentReads, requestItemLists);
            processBatchGetRequest(classesByTableName, requestItems, resultSet, config);
        }

        return resultSet;
    }

    private Map<String, KeysAndAttributes> batchRequestItems(
            boolean consistentReads,
            Map<String, Collection<Map<String, AttributeValue>>> requestItemLists) {
        return requestItemLists.entrySet().stream()
                               .collect(toMap(Entry::getKey, e -> KeysAndAttributes.builder()
                                                                                   .consistentRead(consistentReads)
                                                                                   .keys(e.getValue())
                                                                                   .build()));
    }

    @Override
    public Map<String, List<Object>> batchLoad(Map<Class<?>, List<KeyPair>> itemsToGet, DynamoDbMapperConfig config) {
        config = mergeConfig(config);
        List<Object> keys = new ArrayList<Object>();
        if (itemsToGet != null) {
            for (Map.Entry<Class<?>, List<KeyPair>> item : itemsToGet.entrySet()) {
                Class<?> clazz = item.getKey();
                List<KeyPair> value = item.getValue();
                if (value != null) {
                    final DynamoDbMapperTableModel model = getTableModel(clazz, config);
                    for (KeyPair keyPair : value) {
                        keys.add(model.createKey(keyPair.getHashKey(), keyPair.getRangeKey()));
                    }
                }
            }
        }
        return batchLoad(keys, config);
    }

    /**
     * @param config never null
     */
    private void processBatchGetRequest(
            final Map<String, Class<?>> classesByTableName,
            final Map<String, KeysAndAttributes> requestItems,
            final Map<String, List<Object>> resultSet,
            final DynamoDbMapperConfig config) {

        BatchGetItemResponse batchGetItemResponse = null;
        BatchGetItemRequest batchGetItemRequest = BatchGetItemRequest.builder()
                .requestItems(requestItems)
                .build();

        BatchLoadRetryStrategy batchLoadStrategy = config.batchLoadRetryStrategy();

        BatchLoadContext batchLoadContext = new BatchLoadContext(batchGetItemRequest);

        int retries = 0;

        do {
            if (batchGetItemResponse != null) {
                retries++;
                batchLoadContext.setRetriesAttempted(retries);
                if (!isNullOrEmpty(batchGetItemResponse.unprocessedKeys())) {
                    pause(batchLoadStrategy.getDelayBeforeNextRetry(batchLoadContext));
                    batchGetItemRequest = batchGetItemRequest.toBuilder()
                            .requestItems(batchGetItemResponse.unprocessedKeys())
                            .build();
                    batchLoadContext.setBatchGetItemRequest(batchGetItemRequest);
                }
            }

            batchGetItemResponse = db.batchGetItem(applyBatchOperationUserAgent(batchGetItemRequest));

            Map<String, List<Map<String, AttributeValue>>> responses = batchGetItemResponse.responses();
            for (Map.Entry<String, List<Map<String, AttributeValue>>> entries : responses.entrySet()) {
                String tableName = entries.getKey();
                List<Map<String, AttributeValue>> items = entries.getValue();

                List<Object> objects = resultSet.getOrDefault(tableName, new LinkedList<>());
                Class<?> clazz = classesByTableName.get(tableName);

                for (Map<String, AttributeValue> item : items) {
                    AttributeTransformer.Parameters<?> parameters = toParameters(item, clazz, tableName, config);
                    objects.add(privateMarshallIntoObject(parameters));
                }

                resultSet.put(tableName, objects);
            }

            batchLoadContext.setBatchGetItemResponse(batchGetItemResponse);

            // the number of unprocessed keys and  Batch Load Strategy will drive the number of retries
        } while (batchLoadStrategy.shouldRetry(batchLoadContext));

        if (!isNullOrEmpty(batchGetItemResponse.unprocessedKeys())) {
            throw new BatchGetItemException("The BatchGetItemResponse has unprocessed keys after max retry attempts. Catch the " +
                                            "BatchGetItemException to get the list of unprocessed keys.",
                                            batchGetItemResponse.unprocessedKeys(), resultSet);
        }
    }

    @Override
    public <T> PaginatedScanList<T> scan(Class<T> clazz,
                                         DynamoDbScanExpression scanExpression,
                                         DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        ScanRequest scanRequest = createScanRequestFromExpression(clazz, scanExpression, config);

        ScanResponse scanResult = db.scan(applyUserAgent(scanRequest));
        return new PaginatedScanList<>(this, clazz, db, scanRequest, scanResult, config.getPaginationLoadingStrategy(), config);
    }

    @Override
    public <T> PaginatedParallelScanList<T> parallelScan(Class<T> clazz,
                                                         DynamoDbScanExpression scanExpression,
                                                         int totalSegments,
                                                         DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        // Create hard copies of the original scan request with difference segment number.
        List<ScanRequest> parallelScanRequests = createParallelScanRequestsFromExpression(clazz, scanExpression,
                                                                                          totalSegments, config);
        ParallelScanTask parallelScanTask = new ParallelScanTask(db, parallelScanRequests);

        return new PaginatedParallelScanList<T>(this, clazz, db, parallelScanTask, config.getPaginationLoadingStrategy(), config);
    }

    @Override
    public <T> ScanResultPage<T> scanPage(Class<T> clazz,
                                          DynamoDbScanExpression scanExpression,
                                          DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        ScanRequest scanRequest = createScanRequestFromExpression(clazz, scanExpression, config);

        ScanResponse scanResult = db.scan(applyUserAgent(scanRequest));
        ScanResultPage<T> result = new ScanResultPage<T>();
        List<AttributeTransformer.Parameters<T>> parameters =
                toParameters(scanResult.items(), clazz, scanRequest.tableName(), config);

        result.setResults(marshallIntoObjects(parameters));
        result.setLastEvaluatedKey(scanResult.lastEvaluatedKey());
        result.setCount(scanResult.count());
        result.setScannedCount(scanResult.scannedCount());
        result.setConsumedCapacity(scanResult.consumedCapacity());

        return result;
    }

    @Override
    public <T> PaginatedQueryList<T> query(Class<T> clazz,
                                           DynamoDbQueryExpression<T> queryExpression,
                                           DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        QueryRequest queryRequest = createQueryRequestFromExpression(clazz, queryExpression, config);

        QueryResponse queryResult = db.query(applyUserAgent(queryRequest));
        return new PaginatedQueryList<T>(this, clazz, db, queryRequest, queryResult,
                                         config.getPaginationLoadingStrategy(), config);
    }

    @Override
    public <T> QueryResultPage<T> queryPage(Class<T> clazz,
                                            DynamoDbQueryExpression<T> queryExpression,
                                            DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        QueryRequest queryRequest = createQueryRequestFromExpression(clazz, queryExpression, config);

        QueryResponse queryResult = db.query(applyUserAgent(queryRequest));
        QueryResultPage<T> result = new QueryResultPage<T>();
        List<AttributeTransformer.Parameters<T>> parameters =
                toParameters(queryResult.items(), clazz, queryRequest.tableName(), config);

        result.setResults(marshallIntoObjects(parameters));
        result.setLastEvaluatedKey(queryResult.lastEvaluatedKey());
        result.setCount(queryResult.count());
        result.setScannedCount(queryResult.scannedCount());
        result.setConsumedCapacity(queryResult.consumedCapacity());

        return result;
    }

    @Override
    public int count(Class<?> clazz, DynamoDbScanExpression scanExpression, DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        ScanRequest scanRequest = createScanRequestFromExpression(clazz, scanExpression, config);
        scanRequest = scanRequest.toBuilder().select(Select.COUNT).build();

        // Count scans can also be truncated for large datasets
        int count = 0;
        ScanResponse scanResult;
        do {
            scanResult = db.scan(applyUserAgent(scanRequest));
            count += scanResult.count();
            scanRequest = scanRequest.toBuilder().exclusiveStartKey(scanResult.lastEvaluatedKey()).build();
        } while (scanResult.lastEvaluatedKey() != null);

        return count;
    }

    @Override
    public <T> int count(Class<T> clazz, DynamoDbQueryExpression<T> queryExpression, DynamoDbMapperConfig config) {
        config = mergeConfig(config);

        QueryRequest queryRequest = createQueryRequestFromExpression(clazz, queryExpression, config);
        queryRequest = queryRequest.toBuilder().select(Select.COUNT).build();

        // Count queries can also be truncated for large datasets
        int count = 0;
        QueryResponse queryResult;
        do {
            queryResult = db.query(applyUserAgent(queryRequest));
            count += queryResult.count();
            queryRequest = queryRequest.toBuilder().exclusiveStartKey(queryResult.lastEvaluatedKey()).build();
        } while (queryResult.lastEvaluatedKey() != null);

        return count;
    }

    /**
     * @param config never null
     */
    private ScanRequest createScanRequestFromExpression(Class<?> clazz, DynamoDbScanExpression scanExpression,
                                                        DynamoDbMapperConfig config) {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(getTableName(clazz, config))
                .indexName(scanExpression.getIndexName())
                .scanFilter(scanExpression.scanFilter())
                .limit(scanExpression.limit())
                .exclusiveStartKey(scanExpression.getExclusiveStartKey())
                .totalSegments(scanExpression.getTotalSegments())
                .segment(scanExpression.segment())
                .conditionalOperator(scanExpression.getConditionalOperator())
                .filterExpression(scanExpression.getFilterExpression())
                .expressionAttributeNames(scanExpression.getExpressionAttributeNames())
                .expressionAttributeValues(scanExpression.getExpressionAttributeValues())
                .select(scanExpression.select())
                .projectionExpression(scanExpression.getProjectionExpression())
                .returnConsumedCapacity(scanExpression.getReturnConsumedCapacity())
                .consistentRead(scanExpression.isConsistentRead())
                .build();

        return applyUserAgent(scanRequest);
    }

    /**
     * @param config never null
     */
    private List<ScanRequest> createParallelScanRequestsFromExpression(Class<?> clazz, DynamoDbScanExpression scanExpression,
                                                                       int totalSegments, DynamoDbMapperConfig config) {
        if (totalSegments < 1) {
            throw new IllegalArgumentException("Parallel scan should have at least one scan segment.");
        }
        if (scanExpression.getExclusiveStartKey() != null) {
            log.info("The ExclusiveStartKey parameter specified in the DynamoDBScanExpression is ignored,"
                     + " since the individual parallel scan request on each segment is applied on a separate key scope.");
        }
        if (scanExpression.segment() != null || scanExpression.getTotalSegments() != null) {
            log.info("The Segment and TotalSegments parameters specified in the DynamoDBScanExpression are ignored.");
        }

        List<ScanRequest> parallelScanRequests = new LinkedList<ScanRequest>();
        for (int segment = 0; segment < totalSegments; segment++) {
            ScanRequest scanRequest = createScanRequestFromExpression(clazz, scanExpression, config)
                    .toBuilder()
                    .segment(segment)
                    .totalSegments(totalSegments)
                    .exclusiveStartKey(null)
                    .build();
            parallelScanRequests.add(scanRequest);
        }
        return parallelScanRequests;
    }

    private <T> QueryRequest createQueryRequestFromExpression(Class<T> clazz,
                                                              DynamoDbQueryExpression<T> xpress, DynamoDbMapperConfig config) {

        final DynamoDbMapperTableModel<T> model = getTableModel(clazz, config);

        QueryRequest request = QueryRequest.builder()
                .consistentRead(xpress.isConsistentRead())
                .tableName(getTableName(clazz, xpress.getHashKeyValues(), config))
                .indexName(xpress.getIndexName())
                .keyConditionExpression(xpress.getKeyConditionExpression())
                .build();

        request = processKeyConditions(request, xpress, model);

        request = request.toBuilder()
                .scanIndexForward(xpress.isScanIndexForward())
               .limit(xpress.limit())
               .exclusiveStartKey(xpress.getExclusiveStartKey())
               .queryFilter(xpress.getQueryFilter())
               .conditionalOperator(xpress.getConditionalOperator())
               .select(xpress.select())
               .projectionExpression(xpress.getProjectionExpression())
               .filterExpression(xpress.getFilterExpression())
               .expressionAttributeNames(xpress.getExpressionAttributeNames())
               .expressionAttributeValues(xpress.getExpressionAttributeValues())
               .returnConsumedCapacity(xpress.getReturnConsumedCapacity())
               .build();

        return applyUserAgent(request);
    }

    private <T> AttributeTransformer.Parameters<T> toParameters(
            final Map<String, AttributeValue> attributeValues,
            final Class<T> modelClass,
            final String tableName,
            final DynamoDbMapperConfig mapperConfig) {

        return toParameters(attributeValues, false, modelClass, tableName, mapperConfig);
    }

    private <T> AttributeTransformer.Parameters<T> toParameters(
            final Map<String, AttributeValue> attributeValues,
            final boolean partialUpdate,
            final Class<T> modelClass,
            final String tableName,
            final DynamoDbMapperConfig mapperConfig) {

        return new TransformerParameters<T>(
                getTableModel(modelClass, mapperConfig),
                attributeValues,
                partialUpdate,
                modelClass,
                mapperConfig,
                tableName);
    }

    final <T> List<AttributeTransformer.Parameters<T>> toParameters(
            final List<Map<String, AttributeValue>> attributeValues,
            final Class<T> modelClass,
            final String tableName,
            final DynamoDbMapperConfig mapperConfig) {
        List<AttributeTransformer.Parameters<T>> rval =
                new ArrayList<AttributeTransformer.Parameters<T>>(
                        attributeValues.size());

        for (Map<String, AttributeValue> item : attributeValues) {
            rval.add(toParameters(item, modelClass, tableName, mapperConfig));
        }

        return rval;
    }

    private Map<String, AttributeValue> untransformAttributes(
            final AttributeTransformer.Parameters<?> parameters) {
        if (transformer != null) {
            return transformer.untransform(parameters);
        } else {
            return parameters.getAttributeValues();
        }
    }

    private Map<String, AttributeValue> transformAttributes(
            final AttributeTransformer.Parameters<?> parameters) {

        if (transformer != null) {
            return transformer.transform(parameters);
        } else {
            return parameters.getAttributeValues();
        }
    }

    @Override
    public S3ClientCache s3ClientCache() {
        return s3Links.s3ClientCache();
    }

    @Override
    public S3Link createS3Link(Region s3region, String bucketName, String key) {
        return s3Links.createS3Link(s3region, bucketName, key);
    }

    @Override
    public S3Link createS3Link(String s3region, String bucketName, String key) {
        return s3Links.createS3Link(s3region, bucketName, key);
    }

    @Override
    public <T> CreateTableRequest generateCreateTableRequest(Class<T> clazz, DynamoDbMapperConfig config) {
        config = mergeConfig(config);
        final DynamoDbMapperTableModel<T> model = getTableModel(clazz, config);

        List<KeySchemaElement> keySchemas = new ArrayList<>();
        keySchemas.add(KeySchemaElement.builder().attributeName(model.hashKey().name()).keyType(HASH).build());

        final CreateTableRequest.Builder requestBuilder = CreateTableRequest.builder()
                .tableName(getTableName(clazz, config));

        if (model.rangeKeyIfExists() != null) {
            keySchemas.add(KeySchemaElement.builder()
                                           .attributeName(model.rangeKey().name())
                                           .keyType(RANGE)
                                           .build());
        }
        requestBuilder.globalSecondaryIndexes(model.globalSecondaryIndexes())
            .localSecondaryIndexes(model.localSecondaryIndexes());

        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        for (final DynamoDbMapperFieldModel<T, Object> field : model.fields()) {
            if (field.keyType() != null || field.indexed()) {
                AttributeDefinition attributeDefinition = AttributeDefinition.builder()
                        .attributeType(ScalarAttributeType.valueOf(field.attributeType().name()))
                        .attributeName(field.name())
                        .build();

                attributeDefinitions.add(attributeDefinition);
            }
        }

        requestBuilder.keySchema(keySchemas);
        requestBuilder.attributeDefinitions(attributeDefinitions);
        return requestBuilder.build();
    }

    @Override
    public <T> DeleteTableRequest generateDeleteTableRequest(Class<T> clazz, DynamoDbMapperConfig config) {
        config = mergeConfig(config);
        DeleteTableRequest deleteTableRequest = DeleteTableRequest.builder()
                .tableName(getTableName(clazz, config))
                .build();
        return deleteTableRequest;
    }

    /**
     * Creates a new table mapper using this mapper to perform operations.
     * @param <T> The object type which this mapper operates.
     * @param <H> The hash key value type.
     * @param <R> The range key value type; use <code>?</code> if no range key.
     * @param clazz The object class.
     * @return The table mapper.
     */
    public <T, H, R> DynamoDbTableMapper<T, H, R> newTableMapper(Class<T> clazz) {
        DynamoDbMapperConfig config = mergeConfig(null);
        return new DynamoDbTableMapper<T, H, R>(this.db, this, config, getTableModel(clazz, config));
    }

    /**
     * The one true implementation of AttributeTransformer.Parameters.
     */
    private static class TransformerParameters<T>
            implements AttributeTransformer.Parameters<T> {

        private final DynamoDbMapperTableModel<T> model;
        private final Map<String, AttributeValue> attributeValues;
        private final boolean partialUpdate;
        private final Class<T> modelClass;
        private final DynamoDbMapperConfig mapperConfig;
        private final String tableName;

        TransformerParameters(
                final DynamoDbMapperTableModel<T> model,
                final Map<String, AttributeValue> attributeValues,
                final boolean partialUpdate,
                final Class<T> modelClass,
                final DynamoDbMapperConfig mapperConfig,
                final String tableName) {

            this.model = model;
            this.attributeValues =
                    Collections.unmodifiableMap(attributeValues);
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
        public Class<T> modelClass() {
            return modelClass;
        }

        @Override
        public DynamoDbMapperConfig mapperConfig() {
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

    /**
     * The return type of batchWrite, batchDelete and batchSave.
     *
     * It contains the information about the unprocessed items and the
     * exception causing the failure.
     */
    public static class FailedBatch {
        private Map<String, List<WriteRequest>> unprocessedItems;
        private Exception exception;

        public Map<String, List<WriteRequest>> getUnprocessedItems() {
            return unprocessedItems;
        }

        public void setUnprocessedItems(Map<String, List<WriteRequest>> unprocessedItems) {
            this.unprocessedItems = unprocessedItems;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception excetpion) {
            this.exception = excetpion;
        }

        private boolean isRequestEntityTooLarge() {
            return exception instanceof AmazonServiceException &&
                   RetryUtils.isRequestEntityTooLargeException((AmazonServiceException) exception);
        }

        private boolean isThrottling() {
            return exception instanceof AmazonServiceException &&
                   RetryUtils.isThrottlingException((AmazonServiceException) exception);
        }

        private int size() {
            int size = 0;
            for (final List<WriteRequest> values : unprocessedItems.values()) {
                size += values.size();
            }
            return size;
        }
    }

    /**
     * Used for batch operations where request data is grouped by table name.
     */
    static final class StringListMap<T> extends LinkedHashMap<String, List<T>> {
        private static final long serialVersionUID = -1L;

        public List<T> getPutIfNotExists(final String key) {
            List<T> list = get(key);
            if (list == null) {
                list = new LinkedList<>();
                put(key, list);
            }
            return list;
        }

        public boolean add(final String key, final T value) {
            return getPutIfNotExists(key).add(value);
        }

        public List<StringListMap<T>> subMaps(final int size, boolean perMap) {
            final LinkedList<StringListMap<T>> maps = new LinkedList<StringListMap<T>>();
            int index = 0;
            int count = 0;
            for (final Entry<String, List<T>> entry : entrySet()) {
                for (final T value : entry.getValue()) {
                    if (index == maps.size()) {
                        maps.add(new StringListMap<T>());
                    }
                    maps.get(index).add(entry.getKey(), value);
                    index = perMap ? (++count / size) : (++index % size);
                }
            }
            return maps;
        }
    }

    public static final class BatchGetItemException extends SdkClientException {
        private transient Map<String, KeysAndAttributes> unprocessedKeys;
        private transient Map<String, List<Object>> responses;

        public BatchGetItemException(String message, Map<String, KeysAndAttributes> unprocessedKeys,
                                     Map<String, List<Object>> responses) {
            super(message);
            this.unprocessedKeys = unprocessedKeys;
            this.responses = responses;
        }

        /**
         * Returns a map of tables and their respective keys that were not processed during the operation..
         */
        public Map<String, KeysAndAttributes> getUnprocessedKeys() {
            return unprocessedKeys;
        }

        /**
         * Returns a map of the loaded objects. Each key in the map is the name of a DynamoDB table.
         * Each value in the map is a list of objects that have been loaded from that table. All
         * objects for each table can be cast to the associated user defined type that is
         * annotated as mapping that table.
         */
        public Map<String, List<Object>> getResponses() {
            return responses;
        }
    }

    /**
     * The handler for saving object using DynamoDBMapper. Caller should
     * implement the abstract methods to provide the expected behavior on each
     * scenario, and this handler will take care of all the other basic workflow
     * and common operations.
     */
    protected abstract class SaveObjectHandler {

        protected final Object object;
        protected final Class<?> clazz;
        /**
         * Additional expected value conditions specified by the user.
         */
        protected final Map<String, ExpectedAttributeValue> userProvidedExpectedValueConditions;
        /**
         * Condition operator on the additional expected value conditions
         * specified by the user.
         */
        protected final String userProvidedConditionOperator;
        private final String tableName;
        private final DynamoDbMapperConfig saveConfig;
        private final Map<String, AttributeValue> primaryKeys;
        private final Map<String, AttributeValueUpdate> updateValues;
        /**
         * Any expected value conditions specified by the implementation of
         * DynamoDBMapper, e.g. value assertions on versioned attributes.
         */
        private final Map<String, ExpectedAttributeValue> internalExpectedValueAssertions;
        private final List<ValueUpdate> inMemoryUpdates;

        /**
         * Constructs a handler for saving the specified model object.
         *
         * @param object            The model object to be saved.
         * @param clazz             The domain class of the object.
         * @param tableName         The table name.
         * @param saveConfig        The mapper configuration used for this save.
         * @param saveExpression    The save expression, including the user-provided conditions and an optional logic operator.
         */
        public SaveObjectHandler(
                Class<?> clazz,
                Object object,
                String tableName,
                DynamoDbMapperConfig saveConfig,
                DynamoDbSaveExpression saveExpression) {

            this.clazz = clazz;
            this.object = object;
            this.tableName = tableName;
            this.saveConfig = saveConfig;

            if (saveExpression != null) {
                userProvidedExpectedValueConditions = saveExpression
                        .getExpected();
                userProvidedConditionOperator = saveExpression
                        .getConditionalOperator();
            } else {
                userProvidedExpectedValueConditions = null;
                userProvidedConditionOperator = null;
            }

            updateValues = new HashMap<>();
            internalExpectedValueAssertions = new HashMap<>();
            inMemoryUpdates = new LinkedList<>();
            primaryKeys = new HashMap<>();
        }

        /**
         * The general workflow of a save operation.
         */
        public void execute() {
            final DynamoDbMapperTableModel<Object> model = getTableModel((Class<Object>) clazz, saveConfig);
            for (final DynamoDbMapperFieldModel<Object, Object> field : model.fields()) {
                if (canGenerate(model, object, localSaveBehavior(), field)) {
                    if (field.keyType() != null || field.indexed()) {
                        onAutoGenerateAssignableKey(field);
                    } else if (field.versioned()) {
                        onVersionAttribute(field);
                    } else {
                        onAutoGenerate(field);
                    }
                } else if (field.keyType() != null) {
                    AttributeValue newAttributeValue = field.convert(field.get(object));
                    if (newAttributeValue == null) {
                        throw new DynamoDbMappingException(
                                clazz.getSimpleName() + "[" + field.name() + "]; null or empty value for primary key"
                        );
                    }
                    onPrimaryKeyAttributeValue(field.name(), newAttributeValue);
                } else {
                    AttributeValue currentValue = field.convert(field.get(object));
                    if (currentValue != null) {
                        onNonKeyAttribute(field.name(), currentValue);
                    } else {
                        onNullNonKeyAttribute(field.name());
                    }
                }
            }

            /*
             * Execute the implementation of the low level request.
             */
            executeLowLevelRequest();

            /*
             * Finally, after the service call has succeeded, update the
             * in-memory object with new field values as appropriate. This
             * currently takes into account of auto-generated keys and versioned
             * attributes.
             */
            for (ValueUpdate update : inMemoryUpdates) {
                update.apply();
            }
        }

        /**
         * Implement this method to do the necessary operations when a primary key
         * attribute is set with some value.
         *
         * @param attributeName
         *            The name of the primary key attribute.
         * @param keyAttributeValue
         *            The AttributeValue of the primary key attribute as specified in
         *            the object.
         */
        protected abstract void onPrimaryKeyAttributeValue(String attributeName, AttributeValue keyAttributeValue);

        /**
         * Implement this method for necessary operations when a non-key
         * attribute is set a non-null value in the object.
         * The default implementation simply adds a "PUT" update for the given attribute.
         *
         * @param attributeName
         *            The name of the non-key attribute.
         * @param currentValue
         *            The updated value of the given attribute.
         */
        protected void onNonKeyAttribute(String attributeName, AttributeValue currentValue) {
            updateValues.put(attributeName, AttributeValueUpdate.builder()
                    .value(currentValue)
                    .action("PUT")
                    .build());
        }

        /**
         * Implement this method for necessary operations when a non-key
         * attribute is set null in the object.
         *
         * @param attributeName
         *            The name of the non-key attribute.
         */
        protected abstract void onNullNonKeyAttribute(String attributeName);

        /**
         * Implement this method to send the low-level request that is necessary
         * to complete the save operation.
         */
        protected abstract void executeLowLevelRequest();

        /** Get the SaveBehavior used locally for this save operation. **/
        protected SaveBehavior localSaveBehavior() {
            return saveConfig.saveBehavior();
        }

        /** Get the table name **/
        protected String getTableName() {
            return tableName;
        }

        /** Get the map of all the specified primamry keys of the saved object. **/
        protected Map<String, AttributeValue> getPrimaryKeyAttributeValues() {
            return primaryKeys;
        }

        /** Get the map of AttributeValueUpdate on each modeled attribute. **/
        protected Map<String, AttributeValueUpdate> getAttributeValueUpdates() {
            return updateValues;
        }

        /**
         * Merge and return all the expected value conditions (either
         * user-specified or imposed by the internal implementation of
         * DynamoDBMapper) for this save operation.
         */
        protected Map<String, ExpectedAttributeValue> mergeExpectedAttributeValueConditions() {
            return DynamoDbMapper.mergeExpectedAttributeValueConditions(
                    internalExpectedValueAssertions,
                    userProvidedExpectedValueConditions,
                    userProvidedConditionOperator);
        }

        /** Get the list of all the necessary in-memory update on the object. **/
        protected List<ValueUpdate> getInMemoryUpdates() {
            return inMemoryUpdates;
        }

        /**
         * Save the item using a UpdateItem request. The handler will call this
         * method if
         * <ul>
         * <li>CLOBBER configuration is not being used;
         * <li>AND the item does not contain auto-generated key value;
         * </ul>
         * <p>
         * The ReturnedValues parameter for the UpdateItem request is set as
         * ALL_NEW, which means the service should return all of the attributes
         * of the new version of the item after the update. The handler will use
         * the returned attributes to detect silent failure on the server-side.
         */
        protected UpdateItemResponse doUpdateItem() {
            UpdateItemRequest req = UpdateItemRequest.builder()
                    .tableName(getTableName())
                    .key(getPrimaryKeyAttributeValues())
                    .attributeUpdates(
                            transformAttributeUpdates(
                                    this.clazz,
                                    getTableName(),
                                    getPrimaryKeyAttributeValues(),
                                    getAttributeValueUpdates(),
                                    saveConfig))
                    .expected(mergeExpectedAttributeValueConditions())
                    .conditionalOperator(userProvidedConditionOperator)
                    .returnValues(ReturnValue.ALL_NEW)
                    .build();

            return db.updateItem(applyUserAgent(req));
        }

        /**
         * Save the item using a PutItem request. The handler will call this
         * method if
         * <ul>
         *  <li> CLOBBER configuration is being used;
         *  <li> OR the item contains auto-generated key value;
         *  <li> OR an UpdateItem request has silently failed (200 response with
         *          no affected attribute), which indicates the key-only-put scenario
         *          that we used to handle by the keyOnlyPut(...) hack.
         * </ul>
         */
        protected PutItemResponse doPutItem() {
            Map<String, AttributeValue> attributeValues = convertToItem(getAttributeValueUpdates());

            attributeValues = transformAttributes(
                    toParameters(attributeValues,
                                 this.clazz,
                                 getTableName(),
                                 saveConfig));
            PutItemRequest req = PutItemRequest.builder()
                    .tableName(getTableName())
                    .item(attributeValues)
                    .expected(mergeExpectedAttributeValueConditions())
                    .conditionalOperator(userProvidedConditionOperator)
                    .build();

            return db.putItem(applyUserAgent(req));
        }

        /**
         * Auto-generates the attribute value.
         * @param field The mapping details.
         */
        private void onAutoGenerate(DynamoDbMapperFieldModel<Object, Object> field) {
            AttributeValue value = field.convert(field.generate(field.get(object)));
            updateValues.put(field.name(), AttributeValueUpdate.builder().action("PUT").value(value).build());
            inMemoryUpdates.add(new ValueUpdate(field, value, object));
        }

        /**
         * Auto-generates the key.
         */
        private void onAutoGenerateAssignableKey(DynamoDbMapperFieldModel<Object, Object> field) {
            // Generate the new key value first, then ensure it doesn't exist.
            onAutoGenerate(field);

            if (localSaveBehavior() != SaveBehavior.CLOBBER
                && !internalExpectedValueAssertions.containsKey(field.name())
                && field.getGenerateStrategy() != DynamoDbAutoGenerateStrategy.ALWAYS) {
                // Add an expect clause to make sure that the item
                // doesn't already exist, since it's supposed to be new
                internalExpectedValueAssertions.put(field.name(),
                                                    ExpectedAttributeValue.builder()
                                                            .exists(false)
                                                            .build());
            }
        }

        /**
         * Auto-generates the version.
         * @param field The mapping details.
         */
        private void onVersionAttribute(DynamoDbMapperFieldModel<Object, Object> field) {
            if (localSaveBehavior() != SaveBehavior.CLOBBER
                && !internalExpectedValueAssertions.containsKey(field.name())) {
                // First establish the expected (current) value for the
                // update call
                // For new objects, insist that the value doesn't exist.
                // For existing ones, insist it has the old value.
                final Object current = field.get(object);
                if (current == null) {
                    internalExpectedValueAssertions.put(field.name(),
                                                        ExpectedAttributeValue.builder()
                                                                .exists(false)
                                                                .build());
                } else {
                    internalExpectedValueAssertions.put(field.name(),
                                                        ExpectedAttributeValue.builder()
                                                                .exists(true)
                                                                .value(field.convert(current))
                                                                .build());
                }
            }

            // Generate the new version value
            onAutoGenerate(field);
        }

        /**
         * Converts the {@link AttributeValueUpdate} map given to an equivalent
         * {@link AttributeValue} map.
         */
        private Map<String, AttributeValue> convertToItem(Map<String, AttributeValueUpdate> putValues) {
            Map<String, AttributeValue> map = new HashMap<String, AttributeValue>();
            for (Entry<String, AttributeValueUpdate> entry : putValues.entrySet()) {
                String attributeName = entry.getKey();
                AttributeValue attributeValue = entry.getValue().value();
                String attributeAction = entry.getValue().action();

                /*
                 * AttributeValueUpdate allows nulls for its values, since they are
                 * semantically meaningful. AttributeValues never have null values.
                 */
                if (attributeValue != null
                    && !AttributeAction.DELETE.toString().equals(attributeAction)) {
                    map.put(attributeName, attributeValue);
                }
            }
            return map;
        }

        private Map<String, AttributeValueUpdate> transformAttributeUpdates(
                final Class<?> clazz,
                final String tableName,
                final Map<String, AttributeValue> keys,
                final Map<String, AttributeValueUpdate> updateValues,
                final DynamoDbMapperConfig config) {
            Map<String, AttributeValue> item = convertToItem(updateValues);

            HashSet<String> keysAdded = new HashSet<String>();
            for (Map.Entry<String, AttributeValue> e : keys.entrySet()) {
                if (!item.containsKey(e.getKey())) {
                    keysAdded.add(e.getKey());
                    item.put(e.getKey(), e.getValue());
                }
            }

            AttributeTransformer.Parameters<?> parameters =
                    toParameters(item, true, clazz, tableName, config);

            String hashKey = parameters.getHashKeyName();

            if (!item.containsKey(hashKey)) {
                item.put(hashKey, keys.get(hashKey));
            }

            item = transformAttributes(parameters);

            for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                if (keysAdded.contains(entry.getKey())) {
                    // This was added in for context before calling
                    // transformAttributes, but isn't actually being changed.
                    continue;
                }

                AttributeValueUpdate update = updateValues.get(entry.getKey());
                if (update != null) {
                    AttributeValue value = update.value().toBuilder()
                          .b(entry.getValue().b())
                          .bs(entry.getValue().bs())
                          .n(entry.getValue().n())
                          .ns(entry.getValue().ns())
                          .s(entry.getValue().s())
                          .ss(entry.getValue().ss())
                          .m(entry.getValue().m())
                          .l(entry.getValue().l())
                          .nul(entry.getValue().nul())
                          .bool(entry.getValue().bool()).build();

                    update = update.toBuilder().value(value).build();
                    updateValues.put(entry.getKey(), update);
                } else {
                    updateValues.put(entry.getKey(), AttributeValueUpdate.builder()
                            .value(entry.getValue())
                            .action("PUT")
                            .build());
                }
            }

            return updateValues;
        }
    }

    private static final class ValueUpdate {
        private final DynamoDbMapperFieldModel<Object, Object> field;
        private final AttributeValue newValue;
        private final Object target;

        ValueUpdate(
            DynamoDbMapperFieldModel<Object, Object> field,
            AttributeValue newValue,
            Object target) {

            this.field = field;
            this.newValue = newValue;
            this.target = target;
        }

        public void apply() {
            field.set(target, field.unconvert(newValue));
        }
    }

}
