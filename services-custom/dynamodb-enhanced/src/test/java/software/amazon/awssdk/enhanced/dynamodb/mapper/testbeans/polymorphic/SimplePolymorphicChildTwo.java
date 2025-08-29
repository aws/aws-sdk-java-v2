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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic;

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class SimplePolymorphicChildTwo extends SimplePolymorphicParent {
    String attributeTwo;

    public String getAttributeTwo() {
        return attributeTwo;
    }

    public void setAttributeTwo(String attributeTwo) {
        this.attributeTwo = attributeTwo;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        SimplePolymorphicChildTwo that = (SimplePolymorphicChildTwo) o;
        return Objects.equals(attributeTwo, that.attributeTwo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attributeTwo);
    }
}
