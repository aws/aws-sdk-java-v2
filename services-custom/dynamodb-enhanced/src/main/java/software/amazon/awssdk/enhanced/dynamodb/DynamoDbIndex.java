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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

/**
 * Synchronous interface for running commands against an object that is linked to a specific DynamoDb secondary index
 * and knows how to map records from the table that index is linked to into a modelled object.
 * <p>
 * By default, all command methods throw an {@link UnsupportedOperationException} to prevent interface extensions from breaking
 * implementing classes.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public interface DynamoDbIndex<T> {

    /**
     * Executes a query against a secondary index using a {@link QueryConditional} expression to retrieve a list of
     * items matching the given conditions.
     * <p>
     * The result is accessed through iterable pages (see {@link Page}) in an interactive way; each time a
     * result page is retrieved, a query call is made to DynamoDb to get those entries. If no matches are found,
     * the resulting iterator will contain an empty page. Results are sorted by sort key value in
     * ascending order by default; this behavior can be overridden in the {@link QueryEnhancedRequest}.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link QueryEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API Query operation. Consult the Query documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue("id-value").build());
     * Iterator<Page<MyItem>> results = mappedIndex.query(QueryEnhancedRequest.builder()
     *                                                                        .queryConditional(queryConditional)
     *                                                                        .build());
     * }
     * </pre>
     *
     * @param request A {@link QueryEnhancedRequest} defining the query conditions and how
     * to handle the results.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default SdkIterable<Page<T>> query(QueryEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes a query against a secondary index using a {@link QueryConditional} expression to retrieve a list of
     * items matching the given conditions.
     * <p>
     * The result is accessed through iterable pages (see {@link Page}) in an interactive way; each time a
     * result page is retrieved, a query call is made to DynamoDb to get those entries. If no matches are found,
     * the resulting iterator will contain an empty page. Results are sorted by sort key value in
     * ascending order by default; this behavior can be overridden in the {@link QueryEnhancedRequest}.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link QueryEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API Query operation. Consult the Query documentation for
     * further details and constraints.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to create one
     * manually via {@link QueryEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Iterator<Page<MyItem>> results =
     *     mappedIndex.query(r -> r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue("id-value"))));
     * }
     * </pre>
     *
     * @param requestConsumer A {@link Consumer} of {@link QueryEnhancedRequest} defining the query conditions and how to
     * handle the results.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default SdkIterable<Page<T>> query(Consumer<QueryEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes a query against the secondary index of the table using a {@link QueryConditional} expression to retrieve
     * a list of items matching the given conditions.
     * <p>
     * The result is accessed through iterable pages (see {@link Page}) in an interactive way; each time a
     * result page is retrieved, a query call is made to DynamoDb to get those entries. If no matches are found,
     * the resulting iterator will contain an empty page. Results are sorted by sort key value in
     * ascending order.
     * <p>
     * This operation calls the low-level DynamoDB API Query operation. Consult the Query documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Iterator<Page<MyItem>> results =
     *     mappedIndex.query(QueryConditional.keyEqualTo(Key.builder().partitionValue("id-value").build()));
     * }
     * </pre>
     *
     * @param queryConditional A {@link QueryConditional} defining the matching criteria for records to be queried.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default SdkIterable<Page<T>> query(QueryConditional queryConditional) {
        throw new UnsupportedOperationException();
    }

    /**
     * Scans the table against a secondary index and retrieves all items.
     * <p>
     * The result is accessed through iterable pages (see {@link Page}) in an interactive way; each time a
     * result page is retrieved, a scan call is made to DynamoDb to get those entries. If no matches are found,
     * the resulting iterator will contain an empty page.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link ScanEnhancedRequest}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Iterator<Page<MyItem>> results = mappedTable.scan(ScanEnhancedRequest.builder().consistentRead(true).build());
     * }
     * </pre>
     *
     * @param request A {@link ScanEnhancedRequest} defining how to handle the results.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default SdkIterable<Page<T>> scan(ScanEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Scans the table against a secondary index and retrieves all items.
     * <p>
     * The result is accessed through iterable pages (see {@link Page}) in an interactive way; each time a
     * result page is retrieved, a scan call is made to DynamoDb to get those entries. If no matches are found,
     * the resulting iterator will contain an empty page.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link ScanEnhancedRequest}.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to create one
     * manually via {@link ScanEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Iterator<Page<MyItem>> results = mappedTable.scan(r -> r.limit(5));
     * }
     * </pre>
     *
     * @param requestConsumer A {@link Consumer} of {@link ScanEnhancedRequest} defining the query conditions and how to
     * handle the results.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default SdkIterable<Page<T>> scan(Consumer<ScanEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Scans the table against a secondary index and retrieves all items using default settings.
     * <p>
     * The result is accessed through iterable pages (see {@link Page}) in an interactive way; each time a
     * result page is retrieved, a scan call is made to DynamoDb to get those entries. If no matches are found,
     * the resulting iterator will contain an empty page.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Iterator<Page<MyItem>> results = mappedTable.scan();
     * }
     * </pre>
     *
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default SdkIterable<Page<T>> scan() {
        throw new UnsupportedOperationException();
    }

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
     * Gets the physical secondary index name that operations performed by this object will be executed against.
     * @return The physical secondary index name.
     */
    String indexName();

    /**
     * Creates a {@link Key} object from a modelled item. This key can be used in query conditionals and get
     * operations to locate a specific record.
     * @param item The item to extract the key fields from.
     * @return A key that has been initialized with the index values extracted from the modelled object.
     */
    Key keyFrom(T item);
}
