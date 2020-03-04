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

public class FakeItemComposedSubclass extends FakeItemComposedAbstractSubclass {
    private static final StaticTableSchema<FakeItemComposedSubclass> ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemComposedSubclass.class)
                         .newItemSupplier(FakeItemComposedSubclass::new)
                         .attributes(attribute("composed_subclass",
                                               TypeToken.of(String.class),
                                               FakeItemComposedSubclass::getComposedAttribute,
                                               FakeItemComposedSubclass::setComposedAttribute))
                         .extend(FakeItemComposedAbstractSubclass.getSubclassTableSchema())
                         .build();

    private String composedAttribute;

    public static StaticTableSchema<FakeItemComposedSubclass> getTableSchema() {
        return ITEM_MAPPER;
    }

    public String getComposedAttribute() {
        return composedAttribute;
    }

    public void setComposedAttribute(String composedAttribute) {
        this.composedAttribute = composedAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (! super.equals(o)) return false;
        FakeItemComposedSubclass that = (FakeItemComposedSubclass) o;
        return Objects.equals(composedAttribute, that.composedAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), composedAttribute);
    }
}
