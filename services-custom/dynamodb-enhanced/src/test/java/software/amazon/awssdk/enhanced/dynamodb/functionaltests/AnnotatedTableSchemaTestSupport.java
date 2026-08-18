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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.AbstractBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.AbstractImmutable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.SimpleBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.SimpleImmutable;

/**
 * Shared parameterized test dimensions and test item builders for
 * {@code AnnotatedTableSchemaTest} and {@code AsyncAnnotatedTableSchemaTest}.
 */
final class AnnotatedTableSchemaTestSupport {

    private AnnotatedTableSchemaTestSupport() {
    }

    /**
     * Widen {@link TableSchema}{@code <?>} to {@code TableSchema<Object>} for parameterized tests (unchecked).
     */
    @SuppressWarnings("unchecked")
    private static TableSchema<Object> castTableSchema(Class<?> itemClass) {
        return (TableSchema<Object>) TableSchema.fromClass(itemClass);
    }

    static Collection<Object[]> parameters() {
        List<ParameterizedRow> rows = Arrays.asList(beanRow(), immutableRow());
        return rows.stream().map(ParameterizedRow::toParameterArray).collect(Collectors.toList());
    }

    private static ParameterizedRow beanRow() {
        return new ParameterizedRow(
            "@DynamoDbBean",
            SimpleBean.class,
            castTableSchema(SimpleBean.class),
            AbstractBean.class,
            TestItemFactory::bean,
            TestItemFactory::beanPartial,
            TestItemFactory::beanItem1,
            TestItemFactory::beanItem2,
            TestItemFactory::beanUpdated,
            TestItemFactory::beanUpdatedNullString
        );
    }

    private static ParameterizedRow immutableRow() {
        return new ParameterizedRow(
            "@DynamoDbImmutable",
            SimpleImmutable.class,
            castTableSchema(SimpleImmutable.class),
            AbstractImmutable.class,
            TestItemFactory::immutable,
            TestItemFactory::immutablePartial,
            TestItemFactory::immutableItem1,
            TestItemFactory::immutableItem2,
            TestItemFactory::immutableUpdated,
            TestItemFactory::immutableUpdatedNullString
        );
    }

    /**
     * One {@link org.junit.runners.Parameterized} data row; named fields replace positional {@code Object[]} indexing here only.
     */
    private static final class ParameterizedRow {
        private final String schemaType;
        private final Class<?> itemClass;
        private final TableSchema<Object> tableSchema;
        private final Class<?> abstractItemClass;
        private final Function<TestItemFactory, Object> fullItem;
        private final Function<TestItemFactory, Object> partialItem;
        private final Function<TestItemFactory, Object> firstItem;
        private final Function<TestItemFactory, Object> secondItem;
        private final Function<TestItemFactory, Object> updatedItem;
        private final Function<TestItemFactory, Object> updatedItemWithNullString;

        private ParameterizedRow(
            String schemaType,
            Class<?> itemClass,
            TableSchema<Object> tableSchema,
            Class<?> abstractItemClass,
            Function<TestItemFactory, Object> fullItem,
            Function<TestItemFactory, Object> partialItem,
            Function<TestItemFactory, Object> firstItem,
            Function<TestItemFactory, Object> secondItem,
            Function<TestItemFactory, Object> updatedItem,
            Function<TestItemFactory, Object> updatedItemWithNullString) {

            this.schemaType = schemaType;
            this.itemClass = itemClass;
            this.tableSchema = tableSchema;
            this.abstractItemClass = abstractItemClass;
            this.fullItem = fullItem;
            this.partialItem = partialItem;
            this.firstItem = firstItem;
            this.secondItem = secondItem;
            this.updatedItem = updatedItem;
            this.updatedItemWithNullString = updatedItemWithNullString;
        }

        @SuppressWarnings("unchecked")
        private Object[] toParameterArray() {
            return new Object[] {
                schemaType,
                (Class<Object>) itemClass,
                tableSchema,
                fullItem,
                partialItem,
                firstItem,
                secondItem,
                updatedItem,
                updatedItemWithNullString,
                abstractItemClass
            };
        }
    }

    static final class TestItemFactory {

        private static final String DEFAULT_ID = "id-value";
        private static final String DEFAULT_SORT = "sort-value";

        private static final String ATTR_FULL = "stringAttribute-value";
        private static final String ATTR_ITEM1 = "stringAttribute-value-item1";
        private static final String ATTR_ITEM2 = "stringAttribute-value-item2";
        private static final String ATTR_UPDATED = "stringAttribute-value-updated";

        private final String id = DEFAULT_ID;
        private final String sort = DEFAULT_SORT;

        Object bean() {
            return beanWithStringAttribute(ATTR_FULL);
        }

        Object beanPartial() {
            SimpleBean item = new SimpleBean();
            item.setId(id);
            item.setSort(sort);
            return item;
        }

        Object beanItem1() {
            return beanWithStringAttribute(ATTR_ITEM1);
        }

        Object beanItem2() {
            return beanWithStringAttribute(ATTR_ITEM2);
        }

        Object beanUpdated() {
            return beanWithStringAttribute(ATTR_UPDATED);
        }

        Object beanUpdatedNullString() {
            return beanWithStringAttribute(null);
        }

        Object immutable() {
            return immutableWithStringAttribute(ATTR_FULL);
        }

        Object immutablePartial() {
            return SimpleImmutable.builder()
                                  .id(id)
                                  .sort(sort)
                                  .build();
        }

        Object immutableItem1() {
            return immutableWithStringAttribute(ATTR_ITEM1);
        }

        Object immutableItem2() {
            return immutableWithStringAttribute(ATTR_ITEM2);
        }

        Object immutableUpdated() {
            return immutableWithStringAttribute(ATTR_UPDATED);
        }

        Object immutableUpdatedNullString() {
            return immutableWithStringAttribute(null);
        }

        private SimpleBean beanWithStringAttribute(String stringAttribute) {
            SimpleBean item = new SimpleBean();
            item.setId(id);
            item.setSort(sort);
            item.setStringAttribute(stringAttribute);
            return item;
        }

        private SimpleImmutable immutableWithStringAttribute(String stringAttribute) {
            return SimpleImmutable.builder()
                                .id(id)
                                .sort(sort)
                                .stringAttribute(stringAttribute)
                                .build();
        }
    }
}
