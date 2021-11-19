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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypeName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes.Subtype;

@DynamoDbBean
@DynamoDbSubtypes({
        @Subtype(name = "recursive_one", subtypeClass = RecursivePolyChildOne.class)
})
public abstract class RecursivePolyParent {
    String type;
    RecursivePolyParent recursivePolyParent;

    @DynamoDbSubtypeName
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RecursivePolyParent getRecursivePolyParent() {
        return recursivePolyParent;
    }

    public void setRecursivePolyParent(RecursivePolyParent recursivePolyParent) {
        this.recursivePolyParent = recursivePolyParent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecursivePolyParent that = (RecursivePolyParent) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return recursivePolyParent != null ? recursivePolyParent.equals(that.recursivePolyParent)
                : that.recursivePolyParent == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (recursivePolyParent != null ? recursivePolyParent.hashCode() : 0);
        return result;
    }
}
