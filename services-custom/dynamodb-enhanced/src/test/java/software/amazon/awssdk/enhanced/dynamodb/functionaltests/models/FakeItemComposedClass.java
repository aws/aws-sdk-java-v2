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

public class FakeItemComposedClass {
    private static final StaticTableSchema<FakeItemComposedClass> ITEM_MAPPER =
        StaticTableSchema.builder(FakeItemComposedClass.class)
                         .attributes(attribute("composed_attribute",
                                               TypeToken.of(String.class),
                                               FakeItemComposedClass::getComposedAttribute,
                                               FakeItemComposedClass::setComposedAttribute))
                         .newItemSupplier(FakeItemComposedClass::new)
                         .build();

    private String composedAttribute;

    public FakeItemComposedClass() {
    }

    public FakeItemComposedClass(String composedAttribute) {
        this.composedAttribute = composedAttribute;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StaticTableSchema<FakeItemComposedClass> getTableSchema() {
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
        FakeItemComposedClass that = (FakeItemComposedClass) o;
        return Objects.equals(composedAttribute, that.composedAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(composedAttribute);
    }

    public static class Builder {
        private String composedAttribute;

        public Builder composedAttribute(String composedAttribute) {
            this.composedAttribute = composedAttribute;
            return this;
        }

        public FakeItemComposedClass build() {
            return new FakeItemComposedClass(composedAttribute);
        }
    }
}
