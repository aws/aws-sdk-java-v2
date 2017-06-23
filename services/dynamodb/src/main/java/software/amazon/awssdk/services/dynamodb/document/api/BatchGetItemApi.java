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

import java.util.Map;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.document.BatchGetItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.TableKeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.document.spec.BatchGetItemSpec;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

/**
 * DynamoDB BatchGetItem API that can be used to retrieve multiple items from
 * multiple tables in one request/response by specifying one or multiple primary
 * keys per table in the request.
 */
@ThreadSafe
public interface BatchGetItemApi {
    /**
     * Used to perform a batch get-item operation from DynamoDB.
     *
     * @param returnConsumedCapacity
     *            returned capacity to be returned
     * @param tableKeyAndAttributes
     *            the tables, keys, and attributes specification to be used to
     *            retrieve the items.
     */
    public BatchGetItemOutcome batchGetItem(
            ReturnConsumedCapacity returnConsumedCapacity,
            TableKeysAndAttributes... tableKeyAndAttributes);

    /**
     * Used to perform a batch get-item operation from DynamoDB.
     *
     * @param tableKeyAndAttributes
     *            the tables, keys, and attributes specification to be used to
     *            retrieve the items.
     */
    public BatchGetItemOutcome batchGetItem(
            TableKeysAndAttributes... tableKeyAndAttributes);

    /**
     * Used to perform a batch get-item operation from DynamoDB with full
     * parameter specification.
     */
    public BatchGetItemOutcome batchGetItem(BatchGetItemSpec spec);

    /**
     * Used to perform a batch get-item for the unprocessed keys returned from a
     * previous batch get-item operation.
     *
     * @param returnConsumedCapacity
     *            returned capacity to be returned
     * @param unprocessedKeys
     *            the unprocessed keys returned from the result of a previous
     *            batch-get-item operation.
     *
     * @see BatchGetItemOutcome#getUnprocessedKeys()
     */
    public BatchGetItemOutcome batchGetItemUnprocessed(
            ReturnConsumedCapacity returnConsumedCapacity,
            Map<String, KeysAndAttributes> unprocessedKeys);

    /**
     * Used to perform a batch get-item for the unprocessed keys returned from a
     * previous batch get-item operation.
     *
     * @param unprocessedKeys
     *            the unprocessed keys returned from the result of a previous
     *            batch-get-item operation.
     *
     * @see BatchGetItemOutcome#getUnprocessedKeys()
     */
    public BatchGetItemOutcome batchGetItemUnprocessed(
            Map<String, KeysAndAttributes> unprocessedKeys);
}
