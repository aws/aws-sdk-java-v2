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
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Simple object for storing a modification to a write operation.
 * <p>
 * If a transformedItem is supplied then this item will be completely substituted in place of the item that was
 * previously going to be written.
 * <p>
 * If an additionalConditionalExpression is supplied then this condition will be coalesced with any other conditions
 * and added as a parameter to the write operation.
 */
@SdkPublicApi
public final class WriteModification {
    private final Map<String, AttributeValue> transformedItem;
    private final Expression additionalConditionalExpression;

    private WriteModification(Map<String, AttributeValue> transformedItem, Expression additionalConditionalExpression) {
        this.transformedItem = transformedItem;
        this.additionalConditionalExpression = additionalConditionalExpression;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, AttributeValue> transformedItem() {
        return transformedItem;
    }

    public Expression additionalConditionalExpression() {
        return additionalConditionalExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WriteModification that = (WriteModification) o;

        if (transformedItem != null ? ! transformedItem.equals(that.transformedItem) : that.transformedItem != null) {
            return false;
        }
        return additionalConditionalExpression != null ?
            additionalConditionalExpression.equals(that.additionalConditionalExpression) :
            that.additionalConditionalExpression == null;
    }

    @Override
    public int hashCode() {
        int result = transformedItem != null ? transformedItem.hashCode() : 0;
        result = 31 * result + (additionalConditionalExpression != null ? additionalConditionalExpression.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private Map<String, AttributeValue> transformedItem;
        private Expression additionalConditionalExpression;

        private Builder() {
        }

        public Builder transformedItem(Map<String, AttributeValue> transformedItem) {
            this.transformedItem = transformedItem;
            return this;
        }

        public Builder additionalConditionalExpression(Expression additionalConditionalExpression) {
            this.additionalConditionalExpression = additionalConditionalExpression;
            return this;
        }

        public WriteModification build() {
            return new WriteModification(transformedItem, additionalConditionalExpression);
        }
    }
}
