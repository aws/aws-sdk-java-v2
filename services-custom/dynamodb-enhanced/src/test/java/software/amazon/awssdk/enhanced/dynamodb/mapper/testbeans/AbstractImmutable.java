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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

@DynamoDbImmutable(builder = AbstractImmutable.Builder.class)
public class AbstractImmutable {
    private final String attribute2;

    private AbstractImmutable(Builder b) {
        this.attribute2 = b.attribute2;
    }

    public String attribute2() {
        return attribute2;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String attribute2;

        public Builder attribute2(String attribute2) {
            this.attribute2 = attribute2;
            return this;
        }

        public AbstractImmutable build() {
            return new AbstractImmutable(this);
        }
    }
}
