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

@DynamoDbBean
public class MixedCompositeBean {
    private String id;
    private String pk1;
    private String pk2;
    private String sk1;
    private String sk2;
    private String sk3;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FIRST)
    public String getPk1() {
        return pk1;
    }

    public void setPk1(String pk1) {
        this.pk1 = pk1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.SECOND)
    public String getPk2() {
        return pk2;
    }

    public void setPk2(String pk2) {
        this.pk2 = pk2;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.SECOND)
    public String getSk1() {
        return sk1;
    }

    public void setSk1(String sk1) {
        this.sk1 = sk1;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.FIRST)
    public String getSk2() {
        return sk2;
    }

    public void setSk2(String sk2) {
        this.sk2 = sk2;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.THIRD)
    public String getSk3() {
        return sk3;
    }

    public void setSk3(String sk3) {
        this.sk3 = sk3;
    }
}