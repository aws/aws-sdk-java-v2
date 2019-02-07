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

import java.util.Collection;
import java.util.Map;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.document.AttributeUpdate;
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.document.UpdateItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.spec.UpdateItemSpec;

/**
 * A Table-centric UpdateItem API.
 */
@ThreadSafe
public interface UpdateItemApi {

    /**
     * Updates an item with the attributes specified.
     *
     * @param primaryKey
     *            primary key of the item to be updated
     * @param attributeUpdates
     *            attributes to be updated
     */
    UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                 AttributeUpdate... attributeUpdates);

    UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                 AttributeUpdate... attributeUpdates);

    UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                 String rangeKeyName, Object rangeKeyValue,
                                 AttributeUpdate... attributeUpdates);

    /**
     * Updates an item with the attributes specified.
     *
     * @param primaryKey
     *            primary key of the item to be updated
     * @param expected
     *            the condition to match for the update to succeed.
     * @param attributeUpdates
     *            attributes to be updated
     */
    UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                 Collection<Expected> expected, AttributeUpdate... attributeUpdates);

    /**
     * Updates an item with the specified hash-only key and attributes.
     */
    UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                 Collection<Expected> expected, AttributeUpdate... attributeUpdates);

    /**
     * Updates an item with the specified hash key, range key and attributes.
     */
    UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                 String rangeKeyName, Object rangeKeyValue,
                                 Collection<Expected> expected, AttributeUpdate... attributeUpdates);

    /**
     * Performs an update on an item in the table using the given update
     * expression string.
     *
     * @param primaryKey
     *            primary key of the item to be updated
     * @param updateExpression
     *            the update expression that specifies the attributes to be
     *            updated.
     * @param nameMap
     *            the map containing the mapping between attribute names used in
     *            update expression and the actual name of the attributes
     * @param valueMap
     *            the map containing the mapping between the attribute value
     *            used in update expression and the actual value of the
     *            attribute
     */
    UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                 String updateExpression, Map<String, String> nameMap,
                                 Map<String, Object> valueMap);

    UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                 String updateExpression, Map<String, String> nameMap,
                                 Map<String, Object> valueMap);

    UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                 String rangeKeyName, Object rangeKeyValue,
                                 String updateExpression, Map<String, String> nameMap,
                                 Map<String, Object> valueMap);

    /**
     * Updates an item with the specified primary key using the given
     * update expression provided the condition expression evaluates to true.
     *
     * @param primaryKey
     *            primary key of the item to be updated
     * @param updateExpression
     *            the update expression that specifies the attributes to be
     *            updated.
     * @param conditionExpression
     *            the condition expression that specifies the condition that
     *            needs to be evaluated to true
     * @param nameMap
     *            the map containing the mapping between attribute names used in
     *            update and condition expression and the actual name of the
     *            attributes
     * @param valueMap
     *            the map containing the mapping between the attribute value
     *            used in update and condition expression and the actual value
     *            of the attribute
     */
    UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                 String updateExpression, String conditionExpression,
                                 Map<String, String> nameMap, Map<String, Object> valueMap);

    /**
     * Updates an item with the specified hash key using the given
     * update expression provided the condition expression evaluates to true.
     */
    UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                 String updateExpression, String conditionExpression,
                                 Map<String, String> nameMap, Map<String, Object> valueMap);

    /**
     * Updates an item with the specified hash key and range key using the given
     * update expression provided the condition expression evaluates to true.
     */
    UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                 String rangeKeyName, Object rangeKeyValue,
                                 String updateExpression, String conditionExpression,
                                 Map<String, String> nameMap, Map<String, Object> valueMap);

    /**
     * Performs an update on an item in the table by specifying all the details.
     *
     * @param updateItemSpec
     *            the update specification for the item to be updated.
     */
    UpdateItemOutcome updateItem(UpdateItemSpec updateItemSpec);
}
