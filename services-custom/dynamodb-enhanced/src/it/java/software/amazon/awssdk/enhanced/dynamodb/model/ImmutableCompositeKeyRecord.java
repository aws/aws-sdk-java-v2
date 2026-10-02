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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbImmutable(builder = ImmutableCompositeKeyRecord.Builder.class)
public class ImmutableCompositeKeyRecord {
    private final String id;
    private final String sort;
    private final String pk1;
    private final Integer pk2;
    private final String pk3;
    private final Instant pk4;
    private final String sk1;
    private final String sk2;
    private final Instant sk3;
    private final Integer sk4;
    private final String data;
    private final ImmutableFlattenedRecord flattenedRecord;

    private ImmutableCompositeKeyRecord(Builder builder) {
        this.id = builder.id;
        this.sort = builder.sort;
        this.pk1 = builder.pk1;
        this.pk2 = builder.pk2;
        this.pk3 = builder.pk3;
        this.pk4 = builder.pk4;
        this.sk1 = builder.sk1;
        this.sk2 = builder.sk2;
        this.sk3 = builder.sk3;
        this.sk4 = builder.sk4;
        this.data = builder.data;
        this.flattenedRecord = builder.flattenedRecord;
    }

    public static Builder builder() {
        return new Builder();
    }

    @DynamoDbPartitionKey
    public String id() {
        return this.id;
    }

    @DynamoDbSortKey
    public String sort() {
        return this.sort;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"gsi1", "gsi2"}, order = Order.FIRST)
    public String pk1() {
        return this.pk1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.SECOND)
    @DynamoDbSecondaryPartitionKey(indexNames = "gsi3", order = Order.FIRST)
    public Integer pk2() {
        return this.pk2;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.THIRD)
    public String pk3() {
        return this.pk3;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi3", order = Order.SECOND)
    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FOURTH)
    public Instant pk4() {
        return this.pk4;
    }

    @DynamoDbSecondarySortKey(indexNames = {"gsi1", "gsi2"}, order = Order.FIRST)
    public String sk1() {
        return this.sk1;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.SECOND)
    public String sk2() {
        return this.sk2;
    }


    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.THIRD)
    @DynamoDbSecondarySortKey(indexNames = "gsi3", order = Order.SECOND)
    public Instant sk3() {
        return this.sk3;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.FOURTH)
    public Integer sk4() {
        return this.sk4;
    }

    public String data() {
        return this.data;
    }

    @DynamoDbFlatten
    public ImmutableFlattenedRecord flattenedRecord() {
        return this.flattenedRecord;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ImmutableCompositeKeyRecord that = (ImmutableCompositeKeyRecord) obj;
        return Objects.equals(id, that.id) &&
               Objects.equals(sort, that.sort) &&
               Objects.equals(pk1, that.pk1) &&
               Objects.equals(pk2, that.pk2) &&
               Objects.equals(pk3, that.pk3) &&
               Objects.equals(pk4, that.pk4) &&
               Objects.equals(sk1, that.sk1) &&
               Objects.equals(sk2, that.sk2) &&
               Objects.equals(sk3, that.sk3) &&
               Objects.equals(sk4, that.sk4) &&
               Objects.equals(data, that.data) &&
               Objects.equals(flattenedRecord, that.flattenedRecord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sort, pk1, pk2, pk3, pk4, sk1, sk2, sk3, sk4, data, flattenedRecord);
    }

    public static final class Builder {
        private String id;
        private String sort;
        private String pk1;
        private Integer pk2;
        private String pk3;
        private Instant pk4;
        private String sk1;
        private String sk2;
        private Instant sk3;
        private Integer sk4;
        private String data;
        private ImmutableFlattenedRecord flattenedRecord;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        public Builder pk1(String pk1) {
            this.pk1 = pk1;
            return this;
        }

        public Builder pk2(Integer pk2) {
            this.pk2 = pk2;
            return this;
        }

        public Builder pk3(String pk3) {
            this.pk3 = pk3;
            return this;
        }

        public Builder pk4(Instant pk4) {
            this.pk4 = pk4;
            return this;
        }

        public Builder sk1(String sk1) {
            this.sk1 = sk1;
            return this;
        }

        public Builder sk2(String sk2) {
            this.sk2 = sk2;
            return this;
        }

        public Builder sk3(Instant sk3) {
            this.sk3 = sk3;
            return this;
        }

        public Builder sk4(Integer sk4) {
            this.sk4 = sk4;
            return this;
        }

        public Builder data(String data) {
            this.data = data;
            return this;
        }

        public Builder flattenedRecord(ImmutableFlattenedRecord flattenedRecord) {
            this.flattenedRecord = flattenedRecord;
            return this;
        }

        public ImmutableCompositeKeyRecord build() {
            return new ImmutableCompositeKeyRecord(this);
        }
    }
}
