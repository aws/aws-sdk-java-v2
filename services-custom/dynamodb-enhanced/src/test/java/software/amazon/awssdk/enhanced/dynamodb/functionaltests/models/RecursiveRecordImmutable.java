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

import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

@DynamoDbImmutable(builder = RecursiveRecordImmutable.Builder.class)
public final class RecursiveRecordImmutable {
    private final int attribute;
    private final RecursiveRecordImmutable recursiveRecordImmutable;
    private final RecursiveRecordBean recursiveRecordBean;
    private final List<RecursiveRecordImmutable> recursiveRecordImmutableList;

    private RecursiveRecordImmutable(Builder b) {
        this.attribute = b.attribute;
        this.recursiveRecordImmutable = b.recursiveRecordImmutable;
        this.recursiveRecordBean = b.recursiveRecordBean;
        this.recursiveRecordImmutableList = b.recursiveRecordImmutableList;
    }

    public int getAttribute() {
        return attribute;
    }

    public RecursiveRecordImmutable getRecursiveRecordImmutable() {
        return recursiveRecordImmutable;
    }

    public RecursiveRecordBean getRecursiveRecordBean() {
        return recursiveRecordBean;
    }

    public List<RecursiveRecordImmutable> getRecursiveRecordList() {
        return recursiveRecordImmutableList;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int attribute;
        private RecursiveRecordImmutable recursiveRecordImmutable;
        private RecursiveRecordBean recursiveRecordBean;
        private List<RecursiveRecordImmutable> recursiveRecordImmutableList;

        private Builder() {
        }

        public Builder setAttribute(int attribute) {
            this.attribute = attribute;
            return this;
        }

        public Builder setRecursiveRecordImmutable(RecursiveRecordImmutable recursiveRecordImmutable) {
            this.recursiveRecordImmutable = recursiveRecordImmutable;
            return this;
        }

        public Builder setRecursiveRecordBean(RecursiveRecordBean recursiveRecordBean) {
            this.recursiveRecordBean = recursiveRecordBean;
            return this;
        }

        public Builder setRecursiveRecordList(List<RecursiveRecordImmutable> recursiveRecordImmutableList) {
            this.recursiveRecordImmutableList = recursiveRecordImmutableList;
            return this;
        }

        public RecursiveRecordImmutable build() {
            return new RecursiveRecordImmutable(this);
        }
    }
}
