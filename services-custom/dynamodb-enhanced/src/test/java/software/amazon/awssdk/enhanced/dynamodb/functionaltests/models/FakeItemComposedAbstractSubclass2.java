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

abstract class FakeItemComposedAbstractSubclass2 {
    private static final StaticTableSchema<FakeItemComposedAbstractSubclass2> FAKE_ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemComposedAbstractSubclass2.class)
                         .attributes(attribute("composed_abstract_subclass_2",
                                               TypeToken.of(String.class),
                                               FakeItemComposedAbstractSubclass2::getComposedSubclassAttribute2,
                                               FakeItemComposedAbstractSubclass2::setComposedSubclassAttribute2))
                         .build();

    private String composedSubclassAttribute2;

    static StaticTableSchema<FakeItemComposedAbstractSubclass2> getSubclassTableSchema() {
        return FAKE_ITEM_MAPPER;
    }

    public String getComposedSubclassAttribute2() {
        return composedSubclassAttribute2;
    }

    public void setComposedSubclassAttribute2(String composedSubclassAttribute2) {
        this.composedSubclassAttribute2 = composedSubclassAttribute2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FakeItemComposedAbstractSubclass2 that = (FakeItemComposedAbstractSubclass2) o;
        return Objects.equals(composedSubclassAttribute2, that.composedSubclassAttribute2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(composedSubclassAttribute2);
    }
}
