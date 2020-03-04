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

abstract class FakeItemComposedAbstractSubclass {
    private static final StaticTableSchema<FakeItemComposedAbstractSubclass> FAKE_ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemComposedAbstractSubclass.class)
                         .attributes(attribute("composed_abstract_subclass",
                                               TypeToken.of(String.class),
                                               FakeItemComposedAbstractSubclass::getComposedSubclassAttribute,
                                               FakeItemComposedAbstractSubclass::setComposedSubclassAttribute))                         .build();

    private String composedSubclassAttribute;

    static StaticTableSchema<FakeItemComposedAbstractSubclass> getSubclassTableSchema() {
        return FAKE_ITEM_MAPPER;
    }

    public String getComposedSubclassAttribute() {
        return composedSubclassAttribute;
    }

    public void setComposedSubclassAttribute(String composedSubclassAttribute) {
        this.composedSubclassAttribute = composedSubclassAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FakeItemComposedAbstractSubclass that = (FakeItemComposedAbstractSubclass) o;
        return Objects.equals(composedSubclassAttribute, that.composedSubclassAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(composedSubclassAttribute);
    }
}
