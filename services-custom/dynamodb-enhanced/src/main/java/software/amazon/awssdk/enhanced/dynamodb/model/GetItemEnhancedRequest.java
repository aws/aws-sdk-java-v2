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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

/**
 * Defines parameters used to retrieve an item from a DynamoDb table using the getItem() operation (such as
 * {@link DynamoDbTable#getItem(GetItemEnhancedRequest)} or {@link DynamoDbAsyncTable#getItem(GetItemEnhancedRequest)}).
 * <p>
 * A valid request object must contain a primary {@link Key} to reference the item to get.
 */
@SdkPublicApi
public final class GetItemEnhancedRequest {

    private final Key key;
    private final Boolean consistentRead;

    private GetItemEnhancedRequest(Builder builder) {
        this.key = builder.key;
        this.consistentRead = builder.consistentRead;
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
        return builder().key(key).consistentRead(consistentRead);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GetItemEnhancedRequest getItem = (GetItemEnhancedRequest) o;

        if (key != null ? ! key.equals(getItem.key) : getItem.key != null) {
            return false;
        }
        return consistentRead != null ? consistentRead.equals(getItem.consistentRead) : getItem.consistentRead == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (consistentRead != null ? consistentRead.hashCode() : 0);
        return result;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     * <p>
     * <b>Note</b>: A valid request builder must define a {@link Key}.
     */
    public static final class Builder {
        private Key key;
        private Boolean consistentRead;

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

        public GetItemEnhancedRequest build() {
            return new GetItemEnhancedRequest(this);
        }
    }
}
