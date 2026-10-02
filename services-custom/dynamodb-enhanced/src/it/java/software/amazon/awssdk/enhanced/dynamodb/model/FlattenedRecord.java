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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@DynamoDbBean
public class FlattenedRecord {
    private Double flpk2;
    private String flpk3;
    private String flsk2;
    private Instant flsk3;
    private String fldata;

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi5", order = Order.SECOND)
    public Double getFlpk2() {
        return flpk2;
    }

    public void setFlpk2(Double flpk2) {
        this.flpk2 = flpk2;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi5", order = Order.THIRD)
    public String getFlpk3() {
        return flpk3;
    }

    public void setFlpk3(String flpk3) {
        this.flpk3 = flpk3;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi5", order = Order.SECOND)
    @DynamoDbSecondarySortKey(indexNames = "gsi6", order = Order.FIRST)
    public String getFlsk2() {
        return flsk2;
    }

    public void setFlsk2(String flsk2) {
        this.flsk2 = flsk2;
    }

    @DynamoDbSecondarySortKey(indexNames = "gsi5", order = Order.THIRD)
    public Instant getFlsk3() {
        return flsk3;
    }

    public void setFlsk3(Instant flsk3) {
        this.flsk3 = flsk3;
    }

    public String getFldata() {
        return fldata;
    }

    public void setFldata(String fldata) {
        this.fldata = fldata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlattenedRecord that = (FlattenedRecord) o;
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
}
