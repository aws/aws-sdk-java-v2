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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.Objects;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

/**
 * Defines the elements returned by DynamoDB from a {@code GetItem} operation, such as
 * {@link DynamoDbTable#getItemWithResponse(GetItemEnhancedRequest)} and
 * {@link DynamoDbAsyncTable#getItemWithResponse(GetItemEnhancedRequest)}.
 *
 * @param <T> The type of the item.
 */
@SdkPublicApi
@ThreadSafe
public final class GetItemEnhancedResponse<T> {
    private final T attributes;
    private final ConsumedCapacity consumedCapacity;

    private GetItemEnhancedResponse(GetItemEnhancedResponse.Builder<T> builder) {
        this.attributes = builder.attributes;
        this.consumedCapacity = builder.consumedCapacity;
    }

    /**
     * The attribute values returned by {@code GetItem} operation.
     */
    public T attributes() {
        return attributes;
    }

    /**
     * The capacity units consumed by the {@code GetItem} operation.
     *
     * @see GetItemResponse#consumedCapacity() for more information.
     */
    public ConsumedCapacity consumedCapacity() {
        return consumedCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GetItemEnhancedResponse<?> that = (GetItemEnhancedResponse<?>) o;
        return Objects.equals(attributes, that.attributes) && Objects.equals(consumedCapacity, that.consumedCapacity);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(attributes);
        result = 31 * result + Objects.hashCode(consumedCapacity);
        return result;
    }

    public static <T> GetItemEnhancedResponse.Builder<T> builder() {
        return new GetItemEnhancedResponse.Builder<>();
    }

    @NotThreadSafe
    public static final class Builder<T> {
        private T attributes;
        private ConsumedCapacity consumedCapacity;

        public GetItemEnhancedResponse.Builder<T> attributes(T attributes) {
            this.attributes = attributes;
            return this;
        }

        public GetItemEnhancedResponse.Builder<T> consumedCapacity(ConsumedCapacity consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        public GetItemEnhancedResponse<T> build() {
            return new GetItemEnhancedResponse<>(this);
        }
    }
}
