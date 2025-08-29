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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypeDiscriminator;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSupertype;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSupertype.Subtype;

@DynamoDbBean
@DynamoDbSupertype(@Subtype(discriminatorValue = "recursive_one", subtypeClass = RecursivePolymorphicChild.class))
public abstract class RecursivePolymorphicParent {
    String type;
    RecursivePolymorphicParent recursivePolymorphicParent;

    @DynamoDbSubtypeDiscriminator
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RecursivePolymorphicParent getRecursivePolyParent() {
        return recursivePolymorphicParent;
    }

    public void setRecursivePolyParent(RecursivePolymorphicParent recursivePolymorphicParent) {
        this.recursivePolymorphicParent = recursivePolymorphicParent;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecursivePolymorphicParent that = (RecursivePolymorphicParent) o;
        return Objects.equals(type, that.type) && Objects.equals(recursivePolymorphicParent, that.recursivePolymorphicParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, recursivePolymorphicParent);
    }
}

