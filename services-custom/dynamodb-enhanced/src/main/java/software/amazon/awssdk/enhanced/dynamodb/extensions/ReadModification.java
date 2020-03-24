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

package software.amazon.awssdk.enhanced.dynamodb.extensions;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Simple object for storing a modification to a read operation. If a transformedItem is supplied then this item will
 * be completely substituted in place of the item that was actually read.
 */
@SdkPublicApi
public final class ReadModification {
    private final Map<String, AttributeValue> transformedItem;

    private ReadModification(Map<String, AttributeValue> transformedItem) {
        this.transformedItem = transformedItem;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, AttributeValue> transformedItem() {
        return transformedItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReadModification that = (ReadModification) o;

        return transformedItem != null ? transformedItem.equals(that.transformedItem) : that.transformedItem == null;
    }

    @Override
    public int hashCode() {
        return transformedItem != null ? transformedItem.hashCode() : 0;
    }

    public static final class Builder {
        private Map<String, AttributeValue> transformedItem;

        private Builder() {
        }

        public Builder transformedItem(Map<String, AttributeValue> transformedItem) {
            this.transformedItem = transformedItem;
            return this;
        }

        public ReadModification build() {
            return new ReadModification(transformedItem);
        }
    }
}
