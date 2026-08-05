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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbTimeToLiveAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = RecordWithTTLImmutable.Builder.class)
public class RecordWithTTLImmutable {
    private final String id;
    private final Long expirationDate;

    private RecordWithTTLImmutable(Builder b) {
        this.id = b.id;
        this.expirationDate = b.expirationDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    @DynamoDbTimeToLiveAttribute
    public Long expirationDate() {
        return expirationDate;
    }

    public static final class Builder {
        private String id;
        private Long expirationDate;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder expirationDate(Long expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public RecordWithTTLImmutable build() {
            return new RecordWithTTLImmutable(this);
        }
    }
}
