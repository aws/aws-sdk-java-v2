/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;

@SdkPublicApi
public final class GetItemEnhancedRequest {

    private final Key key;
    private final Boolean consistentRead;

    private GetItemEnhancedRequest(Builder builder) {
        this.key = builder.key;
        this.consistentRead = builder.consistentRead;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().key(key).consistentRead(consistentRead);
    }

    public Boolean consistentRead() {
        return this.consistentRead;
    }

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

    public static final class Builder {
        private Key key;
        private Boolean consistentRead;

        private Builder() {
        }

        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        public GetItemEnhancedRequest build() {
            return new GetItemEnhancedRequest(this);
        }
    }
}
