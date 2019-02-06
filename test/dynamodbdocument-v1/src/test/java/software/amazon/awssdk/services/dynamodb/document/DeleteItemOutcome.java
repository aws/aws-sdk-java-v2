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

package software.amazon.awssdk.services.dynamodb.document;

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;

/**
 * The outcome of deleting an item from a DynamoDB table.
 */
public class DeleteItemOutcome {
    private final DeleteItemResponse result;

    /**
     * @param result the low-level result; must not be null
     */
    public DeleteItemOutcome(DeleteItemResponse result) {
        if (result == null) {
            throw new IllegalArgumentException();
        }
        this.result = result;
    }

    /**
     * Returns all the returned attributes as a (non-null) {@link Item}.
     */
    public Item getItem() {
        Map<String, Object> attributes =
                InternalUtils.toSimpleMapValue(result.attributes());
        Item item = Item.fromMap(attributes);
        return item;
    }

    /**
     * Returns a non-null low-level result returned from the server side.
     */
    public DeleteItemResponse getDeleteItemResponse() {
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(result);
    }
}
