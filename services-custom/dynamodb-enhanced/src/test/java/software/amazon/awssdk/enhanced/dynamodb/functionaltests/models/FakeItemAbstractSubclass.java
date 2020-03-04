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

abstract class FakeItemAbstractSubclass extends FakeItemAbstractSubclass2 {
    private static final StaticTableSchema<FakeItemAbstractSubclass> FAKE_ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemAbstractSubclass.class)
                         .attributes(attribute("subclass_attribute",
                                               TypeToken.of(String.class),
                                               FakeItemAbstractSubclass::getSubclassAttribute,
                                               FakeItemAbstractSubclass::setSubclassAttribute))
                         .flatten(FakeItemComposedSubclass.getTableSchema(),
                                  FakeItemAbstractSubclass::getComposedAttribute,
                                  FakeItemAbstractSubclass::setComposedAttribute)
                         .extend(FakeItemAbstractSubclass2.getSubclass2TableSchema())
                         .build();

    private String subclassAttribute;

    private FakeItemComposedSubclass composedAttribute;

    static StaticTableSchema<FakeItemAbstractSubclass> getSubclassTableSchema() {
        return FAKE_ITEM_MAPPER;
    }

    FakeItemAbstractSubclass() {
        composedAttribute = new FakeItemComposedSubclass();
    }

    public String getSubclassAttribute() {
        return subclassAttribute;
    }

    public void setSubclassAttribute(String subclassAttribute) {
        this.subclassAttribute = subclassAttribute;
    }

    public FakeItemComposedSubclass getComposedAttribute() {
        return composedAttribute;
    }

    public void setComposedAttribute(FakeItemComposedSubclass composedAttribute) {
        this.composedAttribute = composedAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (! super.equals(o)) return false;
        FakeItemAbstractSubclass that = (FakeItemAbstractSubclass) o;
        return Objects.equals(subclassAttribute, that.subclassAttribute) &&
               Objects.equals(composedAttribute, that.composedAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subclassAttribute, composedAttribute);
    }
}
