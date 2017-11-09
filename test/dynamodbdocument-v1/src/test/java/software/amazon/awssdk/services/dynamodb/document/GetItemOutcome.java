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

package software.amazon.awssdk.services.dynamodb.document;

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

/**
 * The outcome of getting an item from DynamoDB table.
 */
public class GetItemOutcome {
    private final GetItemResponse result;

    /**
     * @param result the low-level result; must not be null
     */
    public GetItemOutcome(GetItemResponse result) {
        if (result == null) {
            throw new IllegalArgumentException();
        }
        this.result = result;
    }

    /**
     * Returns all the returned attributes as an {@link Item}; or null if the
     * item doesn't exist.
     */
    public Item getItem() {
        Map<String, Object> attributes =
                InternalUtils.toSimpleMapValue(result.item());
        Item item = Item.fromMap(attributes);
        return item;
    }

    /**
     * Returns a non-null low-level result returned from the server side.
     */
    public GetItemResponse getGetItemResponse() {
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(result);
    }
}
