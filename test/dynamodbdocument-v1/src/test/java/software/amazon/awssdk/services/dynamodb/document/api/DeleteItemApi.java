/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.document.DeleteItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.document.spec.DeleteItemSpec;

/**
 * A Table-centric DeleteItem API.
 */
@ThreadSafe
public interface DeleteItemApi {
    /** Deletes an item by primary key. */
    DeleteItemOutcome deleteItem(KeyAttribute... primaryKeyComponents);

    /** Deletes an item by primary key. */
    DeleteItemOutcome deleteItem(PrimaryKey primaryKey);

    /** Deletes an item by hash-only primary key. */
    DeleteItemOutcome deleteItem(String hashKeyName, Object hashKeyValue);

    /** Deletes an item by hash key-and-range primary key. */
    DeleteItemOutcome deleteItem(String hashKeyName, Object hashKeyValue,
                                 String rangeKeyName, Object rangeKeyValue);

    /**
     * Conditional delete with the specified primary key and expected
     * conditions.
     */
    DeleteItemOutcome deleteItem(PrimaryKey primaryKey,
                                 Expected... expected);

    /**
     * Conditional delete with the specified hash-only primary key and expected
     * conditions.
     */
    DeleteItemOutcome deleteItem(String hashKeyName, Object hashKeyValue,
                                 Expected... expected);

    /**
     * Conditional delete with the specified hash-and-range primary key and
     * expected conditions.
     */
    DeleteItemOutcome deleteItem(String hashKeyName, Object hashKeyValue,
                                 String rangeKeyName, Object rangeKeyValue,
                                 Expected... expected);

    /**
     * Conditional delete with the specified primary key and condition
     * expression.
     */
    DeleteItemOutcome deleteItem(PrimaryKey primaryKey,
                                 String conditionExpression,
                                 Map<String, String> nameMap,
                                 Map<String, Object> valueMap);

    /**
     * Conditional delete with the specified hash-only primary key and condition
     * expression.
     */
    DeleteItemOutcome deleteItem(String hashKeyName, Object hashKeyValue,
                                 String conditionExpression,
                                 Map<String, String> nameMap,
                                 Map<String, Object> valueMap);

    /**
     * Conditional delete with the specified hash-and-range primary key and
     * condition expression.
     */
    DeleteItemOutcome deleteItem(String hashKeyName, Object hashKeyValue,
                                 String rangeKeyName, Object rangeKeyValue,
                                 String conditionExpression,
                                 Map<String, String> nameMap,
                                 Map<String, Object> valueMap);

    /** Deletes an item by specifying all the details. */
    DeleteItemOutcome deleteItem(DeleteItemSpec spec);
}
