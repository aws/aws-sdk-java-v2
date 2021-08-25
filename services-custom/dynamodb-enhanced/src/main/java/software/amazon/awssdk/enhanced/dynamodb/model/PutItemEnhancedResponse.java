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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

@SdkPublicApi
public final class PutItemEnhancedResponse<T> {
    private final T item;
    private final PutItemResponse response;

    private PutItemEnhancedResponse(Builder<T> builder) {
        this.item = builder.item;
        this.response = builder.response;
    }

    public T returnedItem() {
        return item;
    }

    /**
     * The response returned by the low level client.
     *
     * @return The low level response.
     */
    public PutItemResponse response() {
        return response;
    }

    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private T item;
        private PutItemResponse response;

        public Builder<T> item(T item) {
            this.item = item;
            return this;
        }

        public Builder<T> response(PutItemResponse response) {
            this.response = response;
            return this;
        }

        public PutItemEnhancedResponse<T> build() {
            return new PutItemEnhancedResponse<>(this);
        }
    }
}
