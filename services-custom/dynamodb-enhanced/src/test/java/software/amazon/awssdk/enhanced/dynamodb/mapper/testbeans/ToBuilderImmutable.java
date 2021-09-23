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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = ToBuilderImmutable.Builder.class)
public class ToBuilderImmutable {
    private final String id;
    private final String attribute1;

    private ToBuilderImmutable(Builder b) {
        this.id = b.id;
        this.attribute1 = b.attribute1;
    }

    @DynamoDbPartitionKey
    public String id() {
        return this.id;
    }

    public String attribute1() {
        return attribute1;
    }

    public Builder toBuilder() {
        return builder().id(this.id).attribute1(this.attribute1);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String attribute1;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder attribute1(String attribute1) {
            this.attribute1 = attribute1;
            return this;
        }

        public ToBuilderImmutable build() {
            return new ToBuilderImmutable(this);
        }
    }
}
