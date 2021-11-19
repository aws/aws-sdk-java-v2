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
public class NestedPolyChildOne extends NestedPolyParent {
    SimplePolyParent simplePolyParent;

    public SimplePolyParent getSimplePolyParent() {
        return simplePolyParent;
    }

    public void setSimplePolyParent(SimplePolyParent simplePolyParent) {
        this.simplePolyParent = simplePolyParent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NestedPolyChildOne that = (NestedPolyChildOne) o;

        return simplePolyParent != null ? simplePolyParent.equals(that.simplePolyParent)
                : that.simplePolyParent == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (simplePolyParent != null ? simplePolyParent.hashCode() : 0);
        return result;
    }
}
