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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class CompositeKeyRecord {
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
    private FlattenedRecord flattenedRecord;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSortKey
    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"gsi1", "gsi2", "gsi3", "gsi4", "gsi5"}, order = Order.FIRST)
    public String getPk1() {
        return pk1;
    }

    public void setPk1(String pk1) {
        this.pk1 = pk1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"gsi2", "gsi3", "gsi4"}, order = Order.SECOND)
    @DynamoDbSecondaryPartitionKey(indexNames = "gsi6", order = Order.FIRST)
    public Integer getPk2() {
        return pk2;
    }

    public void setPk2(Integer pk2) {
        this.pk2 = pk2;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"gsi3", "gsi4"}, order = Order.THIRD)
    @DynamoDbSecondaryPartitionKey(indexNames = "gsi6", order = Order.SECOND)
    public String getPk3() {
        return pk3;
    }

    public void setPk3(String pk3) {
        this.pk3 = pk3;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi4", order = Order.FOURTH)
    public Instant getPk4() {
        return pk4;
    }

    public void setPk4(Instant pk4) {
        this.pk4 = pk4;
    }

    @DynamoDbSecondarySortKey(indexNames = {"gsi1", "gsi2", "gsi3", "gsi4", "gsi5"}, order = Order.FIRST)
    public String getSk1() {
        return sk1;
    }

    public void setSk1(String sk1) {
        this.sk1 = sk1;
    }

    @DynamoDbSecondarySortKey(indexNames = {"gsi2", "gsi3", "gsi4"}, order = Order.SECOND)
    public String getSk2() {
        return sk2;
    }

    public void setSk2(String sk2) {
        this.sk2 = sk2;
    }

    @DynamoDbSecondarySortKey(indexNames = {"gsi3", "gsi4"}, order = Order.THIRD)
    @DynamoDbSecondarySortKey(indexNames = "gsi6", order = Order.SECOND)
    public Instant getSk3() {
        return sk3;
    }

    public void setSk3(Instant sk3) {
        this.sk3 = sk3;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi4", order = Order.FOURTH)
    public Integer getSk4() {
        return sk4;
    }

    public void setSk4(Integer sk4) {
        this.sk4 = sk4;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @DynamoDbFlatten
    public FlattenedRecord getFlattenedRecord() {
        return flattenedRecord;
    }

    public void setFlattenedRecord(FlattenedRecord flattenedRecord) {
        this.flattenedRecord = flattenedRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompositeKeyRecord that = (CompositeKeyRecord) o;
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
}