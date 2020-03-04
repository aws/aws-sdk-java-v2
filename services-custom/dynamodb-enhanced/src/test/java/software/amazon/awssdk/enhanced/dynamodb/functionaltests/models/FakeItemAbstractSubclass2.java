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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.Attributes.attribute;

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

abstract class FakeItemAbstractSubclass2 {
    private static final StaticTableSchema<FakeItemAbstractSubclass2> FAKE_ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemAbstractSubclass2.class)
                         .attributes(attribute("abstract_subclass_2",
                                               TypeToken.of(String.class),
                                               FakeItemAbstractSubclass2::getSubclassAttribute2,
                                               FakeItemAbstractSubclass2::setSubclassAttribute2))
                         .flatten(FakeItemComposedSubclass2.getTableSchema(),
                                  FakeItemAbstractSubclass2::getComposedAttribute2,
                                  FakeItemAbstractSubclass2::setComposedAttribute2)
                         .build();


    private String subclassAttribute2;

    private FakeItemComposedSubclass2 composedAttribute2;

    static StaticTableSchema<FakeItemAbstractSubclass2> getSubclass2TableSchema() {
        return FAKE_ITEM_MAPPER;
    }

    FakeItemAbstractSubclass2() {
        composedAttribute2 = new FakeItemComposedSubclass2();
    }

    public String getSubclassAttribute2() {
        return subclassAttribute2;
    }

    public void setSubclassAttribute2(String subclassAttribute2) {
        this.subclassAttribute2 = subclassAttribute2;
    }

    public FakeItemComposedSubclass2 getComposedAttribute2() {
        return composedAttribute2;
    }

    public void setComposedAttribute2(FakeItemComposedSubclass2 composedAttribute2) {
        this.composedAttribute2 = composedAttribute2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FakeItemAbstractSubclass2 that = (FakeItemAbstractSubclass2) o;
        return Objects.equals(subclassAttribute2, that.subclassAttribute2) &&
               Objects.equals(composedAttribute2, that.composedAttribute2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subclassAttribute2, composedAttribute2);
    }
}
