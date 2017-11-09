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
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.Item;
import software.amazon.awssdk.services.dynamodb.document.PutItemOutcome;
import software.amazon.awssdk.services.dynamodb.document.spec.PutItemSpec;

/**
 * A Table-centric PutItem API.
 */
@ThreadSafe
public interface PutItemApi {
    /**
     * Unconditional put.
     */
    PutItemOutcome putItem(Item item);

    /**
     * Conditional put.
     */
    PutItemOutcome putItem(Item item, Expected... expected);

    /**
     * Conditional put via condition expression.
     */
    PutItemOutcome putItem(Item item, String conditionExpression,
                           Map<String, String> nameMap, Map<String, Object> valueMap);

    /** Puts an item by specifying all the details. */
    PutItemOutcome putItem(PutItemSpec spec);
}
