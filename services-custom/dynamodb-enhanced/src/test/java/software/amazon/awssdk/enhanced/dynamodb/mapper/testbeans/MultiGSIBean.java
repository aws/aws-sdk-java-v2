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

import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class MultiGSIBean {
    private String id;
    private String sort;
    private String gsi1Pk1;
    private String gsi1Pk2;
    private String gsi1Sk;
    private String gsi2Pk;
    private String gsi2Sk1;
    private String gsi2Sk2;
    private String gsi3Pk1;
    private String gsi3Pk2;
    private String gsi3Pk3;

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

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FIRST)
    public String getGsi1Pk1() {
        return gsi1Pk1;
    }

    public void setGsi1Pk1(String gsi1Pk1) {
        this.gsi1Pk1 = gsi1Pk1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.SECOND)
    public String getGsi1Pk2() {
        return gsi1Pk2;
    }

    public void setGsi1Pk2(String gsi1Pk2) {
        this.gsi1Pk2 = gsi1Pk2;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1")
    public String getGsi1Sk() {
        return gsi1Sk;
    }

    public void setGsi1Sk(String gsi1Sk) {
        this.gsi1Sk = gsi1Sk;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi2")
    public String getGsi2Pk() {
        return gsi2Pk;
    }

    public void setGsi2Pk(String gsi2Pk) {
        this.gsi2Pk = gsi2Pk;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi2", order = Order.FIRST)
    public String getGsi2Sk1() {
        return gsi2Sk1;
    }

    public void setGsi2Sk1(String gsi2Sk1) {
        this.gsi2Sk1 = gsi2Sk1;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi2", order = Order.SECOND)
    public String getGsi2Sk2() {
        return gsi2Sk2;
    }

    public void setGsi2Sk2(String gsi2Sk2) {
        this.gsi2Sk2 = gsi2Sk2;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi3", order = Order.FIRST)
    public String getGsi3Pk1() {
        return gsi3Pk1;
    }

    public void setGsi3Pk1(String gsi3Pk1) {
        this.gsi3Pk1 = gsi3Pk1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi3", order = Order.SECOND)
    public String getGsi3Pk2() {
        return gsi3Pk2;
    }

    public void setGsi3Pk2(String gsi3Pk2) {
        this.gsi3Pk2 = gsi3Pk2;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi3", order = Order.THIRD)
    public String getGsi3Pk3() {
        return gsi3Pk3;
    }

    public void setGsi3Pk3(String gsi3Pk3) {
        this.gsi3Pk3 = gsi3Pk3;
    }
}