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

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class FluentSetterBean {

    private Integer attribute1;
    private String attribute2;
    private String attribute3;

    @DynamoDbPartitionKey
    public Integer getAttribute1() {
        return attribute1;
    }

    public FluentSetterBean setAttribute1(Integer attribute1) {
        this.attribute1 = attribute1;
        return this;
    }

    @DynamoDbSortKey
    public String getAttribute2() {
        return attribute2;
    }

    public FluentSetterBean setAttribute2(String attribute2) {
        this.attribute2 = attribute2;
        return this;
    }

    public String getAttribute3() {
        return attribute3;
    }

    public FluentSetterBean setAttribute3(String attribute3) {
        this.attribute3 = attribute3;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FluentSetterBean fluentSetterBean = (FluentSetterBean) o;
        return Objects.equals(attribute1, fluentSetterBean.attribute1) &&
               Objects.equals(attribute2, fluentSetterBean.attribute2) &&
               Objects.equals(attribute3, fluentSetterBean.attribute3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute1, attribute2, attribute3);
    }
}
