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

package software.amazon.awssdk.services.dynamodb.document.api;

import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.document.TableCollection;
import software.amazon.awssdk.services.dynamodb.document.spec.ListTablesSpec;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

/**
 * DynamoDB ListTables API.
 */
@ThreadSafe
public interface ListTablesApi {
    /**
     * Returns a collection of all the tables (initialized with the respective
     * table names) associated with the current account and endpoint. The
     * underlying collection is paginated with a page size of 100. A network
     * call is made whenever the collection is iterated across a page boundary.
     */
    TableCollection<ListTablesResponse> listTables();

    /**
     * Returns a collection of tables (initialized with the respective table
     * names) associated with the current account and endpoint, starting with a
     * name after the specified <code>exclusiveStartTableName</code> . The
     * underlying collection is paginated with a page size of 100. A network
     * call is made whenever the collection is iterated across a page boundary.
     *
     * @param exclusiveStartTableName
     *            The first table name that this operation will evaluate,
     *            exclusive of the specified
     *            <code>exclusiveStartTableName</code>. Use the value that was
     *            returned for <i>LastEvaluatedTableName</i> in a previous
     *            operation, so that you can obtain the next page of results.
     */
    TableCollection<ListTablesResponse> listTables(String exclusiveStartTableName);

    /**
     * Returns a collection of tables (initialized with the respective table
     * names) up to the specified <code>maxResultSize</code> associated with
     * the current account and endpoint, starting with a name after the
     * specified <code>exclusiveStartTableName</code>. The underlying collection
     * is paginated with a page size of 100. A network call is made whenever the
     * collection is iterated across a page boundary.
     *
     * @param exclusiveStartTableName
     *            The first table name that this operation will evaluate
     *            exclusive of the specified
     *            <code>exclusiveStartTableName</code>. Use the value that was
     *            returned for <i>LastEvaluatedTableName</i> in a previous
     *            operation, so that you can obtain the next page of results.
     * @param maxResultSize
     *            A maximum number of table names to return.
     */
    TableCollection<ListTablesResponse> listTables(String exclusiveStartTableName, int maxResultSize);

    /**
     * Returns a collection of tables (initialized with the respective table
     * names) up to the specified <code>maxResultSize</code> associated with
     * the current account and endpoint. The underlying collection
     * is paginated with a page size of 100. A network call is made whenever the
     * collection is iterated across a page boundary.
     *
     * @param maxResultSize
     *            A maximum number of table names to return.
     */
    TableCollection<ListTablesResponse> listTables(int maxResultSize);

    /**
     * List tables by specifying all the details. The underlying collection is
     * paginated with the specified page size (which defaults to 100). A network
     * call is made whenever the collection is iterated across a page boundary.
     *
     *
     * @param spec
     *            can be used to specify all the detailed parameters of listing
     *            tables.
     *
     * @return a collection of tables associated with the current account and
     *         endpoint.
     */
    TableCollection<ListTablesResponse> listTables(ListTablesSpec spec);
}
