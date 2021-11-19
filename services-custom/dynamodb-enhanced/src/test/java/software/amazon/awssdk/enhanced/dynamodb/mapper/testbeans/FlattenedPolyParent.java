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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypeName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes.Subtype;

@DynamoDbBean
@DynamoDbSubtypes(@Subtype(name = "one", subtypeClass = FlattenedPolyChildOne.class))
public abstract class FlattenedPolyParent {
    FlattenedPolyParentComposite flattenedPolyParentComposite;

    @DynamoDbFlatten
    public FlattenedPolyParentComposite getFlattenedPolyParentComposite() {
        return flattenedPolyParentComposite;
    }

    public void setFlattenedPolyParentComposite(FlattenedPolyParentComposite flattenedPolyParentComposite) {
        this.flattenedPolyParentComposite = flattenedPolyParentComposite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlattenedPolyParent that = (FlattenedPolyParent) o;

        return flattenedPolyParentComposite != null ?
                flattenedPolyParentComposite.equals(that.flattenedPolyParentComposite) :
                that.flattenedPolyParentComposite == null;
    }

    @Override
    public int hashCode() {
        return flattenedPolyParentComposite != null ? flattenedPolyParentComposite.hashCode() : 0;
    }
}
