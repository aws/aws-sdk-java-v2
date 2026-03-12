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
public class CompositeKeyMaxBean {
    private String id;
    private String gsiPk1;
    private String gsiPk2;
    private String gsiPk3;
    private String gsiPk4;
    private String gsiSk1;
    private String gsiSk2;
    private String gsiSk3;
    private String gsiSk4;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FIRST)
    public String getGsiPk1() {
        return gsiPk1;
    }

    public void setGsiPk1(String gsiPk1) {
        this.gsiPk1 = gsiPk1;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.SECOND)
    public String getGsiPk2() {
        return gsiPk2;
    }

    public void setGsiPk2(String gsiPk2) {
        this.gsiPk2 = gsiPk2;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.THIRD)
    public String getGsiPk3() {
        return gsiPk3;
    }

    public void setGsiPk3(String gsiPk3) {
        this.gsiPk3 = gsiPk3;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi1", order = Order.FOURTH)
    public String getGsiPk4() {
        return gsiPk4;
    }

    public void setGsiPk4(String gsiPk4) {
        this.gsiPk4 = gsiPk4;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.FIRST)
    public String getGsiSk1() {
        return gsiSk1;
    }

    public void setGsiSk1(String gsiSk1) {
        this.gsiSk1 = gsiSk1;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.SECOND)
    public String getGsiSk2() {
        return gsiSk2;
    }

    public void setGsiSk2(String gsiSk2) {
        this.gsiSk2 = gsiSk2;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.THIRD)
    public String getGsiSk3() {
        return gsiSk3;
    }

    public void setGsiSk3(String gsiSk3) {
        this.gsiSk3 = gsiSk3;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi1", order = Order.FOURTH)
    public String getGsiSk4() {
        return gsiSk4;
    }

    public void setGsiSk4(String gsiSk4) {
        this.gsiSk4 = gsiSk4;
    }
}