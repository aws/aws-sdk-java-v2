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
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

/**
 * Defines parameters used to retrieve an item from a DynamoDb table using the getItem() operation (such as
 * {@link DynamoDbTable#getItem(GetItemEnhancedRequest)} or {@link DynamoDbAsyncTable#getItem(GetItemEnhancedRequest)}).
 * <p>
 * A valid request object must contain a primary {@link Key} to reference the item to get.
 */
@SdkPublicApi
@ThreadSafe
public final class GetItemEnhancedRequest {

    private final Key key;
    private final Boolean consistentRead;
    private final String returnConsumedCapacity;

    private GetItemEnhancedRequest(Builder builder) {
        this.key = builder.key;
        this.consistentRead = builder.consistentRead;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
    }

    /**
     * All requests must be constructed using a Builder.
     * @return a builder of this type
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a builder with all existing values set
     */
    public Builder toBuilder() {
        return builder().key(key).consistentRead(consistentRead).returnConsumedCapacity(returnConsumedCapacity);
    }

    /**
     * @return whether or not this request will use consistent read
     */
    public Boolean consistentRead() {
        return this.consistentRead;
    }

    /**
     * Returns the primary {@link Key} for the item to get.
     */
    public Key key() {
        return this.key;
    }

    /**
     * Whether to return the capacity consumed by this operation.
     *
     * @see GetItemRequest#returnConsumedCapacity()
     */
    public ReturnConsumedCapacity returnConsumedCapacity() {
        return ReturnConsumedCapacity.fromValue(returnConsumedCapacity);
    }

    /**
     * Whether to return the capacity consumed by this operation.
     * <p>
     * Similar to {@link #returnConsumedCapacity()} but return the value as a string. This is useful in situations where the
     * value is not defined in {@link ReturnConsumedCapacity}.
     */
    public String returnConsumedCapacityAsString() {
        return returnConsumedCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GetItemEnhancedRequest that = (GetItemEnhancedRequest) o;

        return Objects.equals(key, that.key)
               && Objects.equals(consistentRead, that.consistentRead)
               && Objects.equals(returnConsumedCapacity, that.returnConsumedCapacity);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (consistentRead != null ? consistentRead.hashCode() : 0);
        result = 31 * result + (returnConsumedCapacity != null ? returnConsumedCapacity.hashCode() : 0);
        return result;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     * <p>
     * <b>Note</b>: A valid request builder must define a {@link Key}.
     */
    @NotThreadSafe
    public static final class Builder {
        private Key key;
        private Boolean consistentRead;
        private String returnConsumedCapacity;

        private Builder() {
        }

        /**
         * Determines the read consistency model: If set to true, the operation uses strongly consistent reads; otherwise,
         * the operation uses eventually consistent reads.
         * <p>
         * By default, the value of this property is set to <em>false</em>.
         *
         * @param consistentRead sets consistency model of the operation to use strong consistency
         * @return a builder of this type
         */
        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        /**
         * Sets the primary {@link Key} that will be used to match the item to retrieve.
         *
         * @param key the primary key to use in the request.
         * @return a builder of this type
         */
        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the primary {@link Key} that will be used to match the item to retrieve
         * by accepting a consumer of {@link Key.Builder}.
         *
         * @param keyConsumer a {@link Consumer} of {@link Key}
         * @return a builder of this type
         */
        public Builder key(Consumer<Key.Builder> keyConsumer) {
            Key.Builder builder = Key.builder();
            keyConsumer.accept(builder);
            return key(builder.build());
        }

        /**
         * Whether to return the capacity consumed by this operation.
         *
         * @see GetItemRequest.Builder#returnConsumedCapacity(ReturnConsumedCapacity)
         */
        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity == null ? null : returnConsumedCapacity.toString();
            return this;
        }

        /**
         * Whether to return the capacity consumed by this operation.
         *
         * @see GetItemRequest.Builder#returnConsumedCapacity(String)
         */
        public Builder returnConsumedCapacity(String returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        public GetItemEnhancedRequest build() {
            return new GetItemEnhancedRequest(this);
        }
    }
}
