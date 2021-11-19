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
public class RecursivePolyChildOne extends RecursivePolyParent {
    RecursivePolyParent recursivePolyParentOne;
    String attributeOne;

    public RecursivePolyParent getRecursivePolyParentOne() {
        return recursivePolyParentOne;
    }

    public void setRecursivePolyParentOne(RecursivePolyParent recursivePolyParentOne) {
        this.recursivePolyParentOne = recursivePolyParentOne;
    }

    public String getAttributeOne() {
        return attributeOne;
    }

    public void setAttributeOne(String attributeOne) {
        this.attributeOne = attributeOne;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RecursivePolyChildOne that = (RecursivePolyChildOne) o;

        if (recursivePolyParentOne != null ? !recursivePolyParentOne.equals(that.recursivePolyParentOne) : that.recursivePolyParentOne != null)
            return false;
        return attributeOne != null ? attributeOne.equals(that.attributeOne) : that.attributeOne == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (recursivePolyParentOne != null ? recursivePolyParentOne.hashCode() : 0);
        result = 31 * result + (attributeOne != null ? attributeOne.hashCode() : 0);
        return result;
    }
}
