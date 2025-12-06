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

import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@DynamoDbImmutable(builder = ImmutableFlattenedRecord.Builder.class)
public class ImmutableFlattenedRecord {
    private final Double flpk2;
    private final String flpk3;
    private final String flsk2;
    private final Instant flsk3;
    private final String fldata;

    private ImmutableFlattenedRecord(Builder builder) {
        this.flpk2 = builder.flpk2;
        this.flpk3 = builder.flpk3;
        this.flsk2 = builder.flsk2;
        this.flsk3 = builder.flsk3;
        this.fldata = builder.fldata;
    }

    public static Builder builder() {
        return new Builder();
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi2", order = Order.SECOND)
    public Double flpk2() {
        return this.flpk2;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi2", order = Order.THIRD)
    public String flpk3() {
        return this.flpk3;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi2", order = Order.SECOND)
    @DynamoDbSecondarySortKey(indexNames = "gsi3", order = Order.FIRST)
    public String flsk2() {
        return this.flsk2;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi2", order = Order.THIRD)
    public Instant flsk3() {
        return this.flsk3;
    }

    public String fldata() {
        return this.fldata;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ImmutableFlattenedRecord that = (ImmutableFlattenedRecord) obj;
        return Objects.equals(flpk2, that.flpk2) &&
               Objects.equals(flpk3, that.flpk3) &&
               Objects.equals(flsk2, that.flsk2) &&
               Objects.equals(flsk3, that.flsk3) &&
               Objects.equals(fldata, that.fldata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flpk2, flpk3, flsk2, flsk3, fldata);
    }

    public static final class Builder {
        private Double flpk2;
        private String flpk3;
        private String flsk2;
        private Instant flsk3;
        private String fldata;

        public Builder flpk2(Double flpk2) {
            this.flpk2 = flpk2;
            return this;
        }

        public Builder flpk3(String flpk3) {
            this.flpk3 = flpk3;
            return this;
        }

        public Builder flsk2(String flsk2) {
            this.flsk2 = flsk2;
            return this;
        }

        public Builder flsk3(Instant flsk3) {
            this.flsk3 = flsk3;
            return this;
        }

        public Builder fldata(String fldata) {
            this.fldata = fldata;
            return this;
        }

        public ImmutableFlattenedRecord build() {
            return new ImmutableFlattenedRecord(this);
        }
    }
}
