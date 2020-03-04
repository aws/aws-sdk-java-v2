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

public class FakeItemComposedSubclass2 extends FakeItemComposedAbstractSubclass2 {
    private static final StaticTableSchema<FakeItemComposedSubclass2> ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemComposedSubclass2.class)
                         .newItemSupplier(FakeItemComposedSubclass2::new)
                         .extend(getSubclassTableSchema())
                         .attributes(attribute("composed_subclass_2",
                                               TypeToken.of(String.class),
                                               FakeItemComposedSubclass2::getComposedAttribute2,
                                               FakeItemComposedSubclass2::setComposedAttribute2))
                         .build();

    private String composedAttribute2;

    public static StaticTableSchema<FakeItemComposedSubclass2> getTableSchema() {
        return ITEM_MAPPER;
    }

    public String getComposedAttribute2() {
        return composedAttribute2;
    }

    public void setComposedAttribute2(String composedAttribute2) {
        this.composedAttribute2 = composedAttribute2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (! super.equals(o)) return false;
        FakeItemComposedSubclass2 that = (FakeItemComposedSubclass2) o;
        return Objects.equals(composedAttribute2, that.composedAttribute2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), composedAttribute2);
    }
}
