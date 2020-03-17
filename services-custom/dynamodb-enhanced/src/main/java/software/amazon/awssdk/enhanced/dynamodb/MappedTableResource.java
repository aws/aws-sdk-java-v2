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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Interface for a resource object that is part of either a {@link DynamoDbTable} or {@link DynamoDbAsyncTable}. This
 * part of the interface is common between both of those higher order interfaces and has methods to access the
 * metadata associated with the mapped entity, such as the schema and the table name, but knows nothing about how to
 * actually execute operations against it.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public interface MappedTableResource<T> {
    /**
     * Gets the {@link DynamoDbEnhancedClientExtension} associated with this mapped resource.
     * @return The {@link DynamoDbEnhancedClientExtension} associated with this mapped resource.
     */
    DynamoDbEnhancedClientExtension mapperExtension();

    /**
     * Gets the {@link TableSchema} object that this mapped table was built with.
     * @return The {@link TableSchema} object for this mapped table.
     */
    TableSchema<T> tableSchema();

    /**
     * Gets the physical table name that operations performed by this object will be executed against.
     * @return The physical table name.
     */
    String tableName();

    /**
     * Creates a {@link Key} object from a modelled item. This key can be used in query conditionals and get
     * operations to locate a specific record.
     * @param item The item to extract the key fields from.
     * @return A key that has been initialized with the index values extracted from the modelled object.
     */
    Key keyFrom(T item);
}
