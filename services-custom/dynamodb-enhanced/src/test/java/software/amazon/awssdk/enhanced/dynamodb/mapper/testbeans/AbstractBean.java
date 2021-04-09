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

@DynamoDbBean
public class AbstractBean {
    private String attribute2;

    public String getAttribute2() {
        return attribute2;
    }
    public void setAttribute2(String attribute2) {
        this.attribute2 = attribute2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractBean that = (AbstractBean) o;

        return attribute2 != null ? attribute2.equals(that.attribute2) : that.attribute2 == null;
    }

    @Override
    public int hashCode() {
        return attribute2 != null ? attribute2.hashCode() : 0;
    }
}
