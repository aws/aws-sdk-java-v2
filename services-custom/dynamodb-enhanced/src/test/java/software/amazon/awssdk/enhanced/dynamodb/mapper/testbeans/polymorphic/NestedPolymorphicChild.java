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
public class NestedPolymorphicChild extends NestedPolymorphicParent {
    SimplePolymorphicParent simplePolymorphicParent;

    public SimplePolymorphicParent getSimplePolyParent() {
        return simplePolymorphicParent;
    }

    public void setSimplePolyParent(SimplePolymorphicParent simplePolymorphicParent) {
        this.simplePolymorphicParent = simplePolymorphicParent;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NestedPolymorphicChild that = (NestedPolymorphicChild) o;
        return Objects.equals(simplePolymorphicParent, that.simplePolymorphicParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), simplePolymorphicParent);
    }
}
