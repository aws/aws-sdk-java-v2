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

package software.amazon.awssdk.services.dynamodb.document.api;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.document.BatchWriteItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.TableWriteItems;
import software.amazon.awssdk.services.dynamodb.document.spec.BatchWriteItemSpec;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * DynamoDB BatchWriteItem API that can be used to put multiple items to and/or
 * delete multiple items from multiple tables in a single request-response
 * to/from DynamoDB.
 */
@ThreadSafe
public interface BatchWriteItemApi {

    /**
     * Used to perform a batch write operation to DynamoDB.
     *
     * @param tableWriteItems
     *            the tables and the respective keys to delete from and/or the
     *            respective items to be put.
     */
    BatchWriteItemOutcome batchWriteItem(
        TableWriteItems... tableWriteItems);

    /**
     * Used to perform a batch write operation to DynamoDB with full parameter
     * specification.
     */
    BatchWriteItemOutcome batchWriteItem(BatchWriteItemSpec spec);

    /**
     * Used to perform a batch write operation for the unprocessed items
     * returned from a previous batch write operation.
     *
     * @param unprocessedItems
     *            the unprocessed items returned from the result of a previous
     *            batch write operation
     *
     * @see BatchWriteItemOutcome#getUnprocessedItems()
     */
    BatchWriteItemOutcome batchWriteItemUnprocessed(
        Map<String, List<WriteRequest>> unprocessedItems);
}
