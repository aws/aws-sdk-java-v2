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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Interface for an object the stores structural information about a DynamoDb table.
 */
@SdkPublicApi
public interface TableMetadata {
    /**
     * Returns the attribute name of the partition key for an index.
     *
     * @param indexName The name of the index.
     * @return The attribute name representing the partition key for this index.
     * @throws IllegalArgumentException if the index does not exist in the metadata or does not have a partition key
     * associated with it..
     */
    String indexPartitionKey(String indexName);

    /**
     * Returns the attribute name of the sort key for an index.
     *
     * @param indexName The name of the index.
     * @return Optional of the attribute name representing the sort key for this index; empty if the index does not
     * have a sort key.
     */
    Optional<String> indexSortKey(String indexName);

    /**
     * Returns a custom metadata object. These objects are used by extensions to the library, therefore the type of
     * object stored is flexible and does not need to be known by the interface.
     *
     * @param key A unique key for the metadata object. This namespace is shared by all extensions, so it is
     *            recommended best practice to qualify it with the name of your extension.
     * @param objectClass The java class that the object will be cast to before returning. An exception will be
     *                    thrown if the stored object cannot be cast to this class.
     * @param <T> The flexible type for the object being returned. The compiler will typically infer this.
     * @return An optional containing custom metadata object or empty if the object was not found.
     */
    <T> Optional<T> customMetadataObject(String key, Class<? extends T> objectClass);

    /**
     * Returns all the names of attributes associated with the keys of a specified index.
     *
     * @param indexName The name of the index.
     * @return A collection of all key attribute names for that index.
     */
    Collection<String> indexKeys(String indexName);

    /**
     * Returns all the names of attributes associated with any index (primary or secondary) known for this table.
     * Additionally any additional attributes that are deemed to be 'key-like' in how they should be treated will
     * also be returned. An example of a 'key-like' attribute that is not actually a key is one tagged as a 'version'
     * attribute when using the versioned record extension.
     *
     * @return A collection of all key attribute names for the table.
     *
     * @deprecated Use {@link #keyAttributes()} instead.
     */
    @Deprecated
    Collection<String> allKeys();

    /**
     * Returns metadata about all the known indices for this table.
     * @return A collection of {@link IndexMetadata} containing information about the indices.
     */
    Collection<IndexMetadata> indices();

    /**
     * Returns all custom metadata for this table. These entries are used by extensions to the library, therefore the
     * value type of each metadata object stored in the map is not known and is provided as {@link Object}.
     * <p>
     * This method should not be used to inspect individual custom metadata objects, instead use
     * {@link TableMetadata#customMetadataObject(String, Class)} ()} as that will perform a type-safety check on the
     * retrieved object.
     * @return A map of all the custom metadata for this table.
     */
    Map<String, Object> customMetadata();

    /**
     * Returns metadata about all the known 'key' attributes for this table, such as primary and secondary index keys,
     * or any other attribute that forms part of the structure of the table.
     * @return A collection of {@link KeyAttributeMetadata} containing information about the keys.
     */
    Collection<KeyAttributeMetadata> keyAttributes();

    /**
     * Returns the DynamoDb scalar attribute type associated with a key attribute if one is applicable.
     * @param keyAttribute The key attribute name to return the scalar attribute type of.
     * @return Optional {@link ScalarAttributeType} of the attribute, or empty if attribute is a non-scalar type.
     * @throws IllegalArgumentException if the keyAttribute is not found.
     */
    Optional<ScalarAttributeType> scalarAttributeType(String keyAttribute);

    /**
     * Returns the attribute name used as the primary partition key for the table.
     *
     * @return The primary partition key attribute name.
     * @throws IllegalArgumentException if the primary partition key is not known.
     */
    default String primaryPartitionKey() {
        return indexPartitionKey(primaryIndexName());
    }

    /**
     * Returns the attribute name used as the primary sort key for the table.
     *
     * @return An optional of the primary sort key attribute name; empty if this key is not known.
     */
    default Optional<String> primarySortKey() {
        return indexSortKey(primaryIndexName());
    }

    /**
     * Returns the names of the attributes that make up the primary key for the table.
     *
     * @return A collection of attribute names that make up the primary key for the table.
     */
    default Collection<String> primaryKeys() {
        return indexKeys(primaryIndexName());
    }

    /**
     * Returns an arbitrary constant that should be used as the primary index name. This pattern creates a
     * common abstraction and simplifies the implementation of operations that also work on secondary indices such as
     * scan() and query().
     *
     * @return An arbitrary constant that internally represents the primary index name.
     */
    static String primaryIndexName() {
        // Must include illegal symbols that cannot be used by a real index.
        // This value is arbitrary and ephemeral but could end up being serialized with TableMetadata through the
        // actions of a client, so it should not be altered unless absolutely necessary.
        return "$PRIMARY_INDEX";
    }
}
