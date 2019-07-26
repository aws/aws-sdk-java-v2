/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Interface for running commands against an object that is linked to a specific DynamoDb table and knows how to map
 * records from that table into a modelled object.
 *
 * Typically an implementation for this interface can be obtained from a {@link MappedDatabase}:
 *
 * mappedTable = mappedDatabase.table(tableSchema);
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public interface MappedTable<T> extends MappedIndex<T> {
    /**
     * Returns a mapped index that can be used to execute commands against a secondary index belonging to the table
     * being mapped by this object. Note that only a subset of the commands that work against a table will work
     * against a secondary index.
     *
     * @param indexName The name of the secondary index to build the command interface for.
     * @return A {@link MappedIndex} object that can be used to execute database commands against.
     */
    MappedIndex<T> index(String indexName);
}
