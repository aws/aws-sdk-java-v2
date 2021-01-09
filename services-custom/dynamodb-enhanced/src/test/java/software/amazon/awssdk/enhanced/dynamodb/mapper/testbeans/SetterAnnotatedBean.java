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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class SetterAnnotatedBean {
    private String id;
    private Integer integerAttribute;

    public String getId() {
        return this.id;
    }

    @DynamoDbPartitionKey
    public void setId(String id) {
        this.id = id;
    }

    public Integer getIntegerAttribute() {
        return integerAttribute;
    }

    @DynamoDbIgnore
    public void setIntegerAttribute(Integer integerAttribute) {
        this.integerAttribute = integerAttribute;
    }
}
