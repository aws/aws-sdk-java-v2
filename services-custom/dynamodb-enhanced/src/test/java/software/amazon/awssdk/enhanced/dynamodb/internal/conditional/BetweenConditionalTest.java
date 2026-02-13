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

package software.amazon.awssdk.enhanced.dynamodb.internal.conditional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

class BetweenConditionalTest {

    private static final StaticTableSchema<TestItem> TABLE_SCHEMA =
        StaticTableSchema.builder(TestItem.class)
                         .newItemSupplier(TestItem::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(TestItem::getId)
                                                           .setter(TestItem::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("sort")
                                                           .getter(TestItem::getSort)
                                                           .setter(TestItem::setSort)
                                                           .tags(primarySortKey()))
                         .build();

    @Test
    void expression_whenBothKeysHaveValidSortValues_generatesExpression() {
        Key key1 = Key.builder()
                      .partitionValue("test")
                      .sortValue("sortA")
                      .build();
        Key key2 = Key.builder()
                      .partitionValue("test")
                      .sortValue("sortZ")
                      .build();

        QueryConditional conditional = new BetweenConditional(key1, key2);
        Expression expression = conditional.expression(TABLE_SCHEMA, TableMetadata.primaryIndexName());

        assertThat(expression).isNotNull();
        assertThat(expression.expression()).contains("BETWEEN");
    }

    @Test
    void expression_whenSecondKeySortValuesDoNotContainNull_generatesExpression() {
        Key key1 = Key.builder().partitionValue("test").sortValue("sortA").build();
        Key key2 = Key.builder().partitionValue("test").sortValue("sortZ").build();

        QueryConditional conditional = new BetweenConditional(key1, key2);
        Expression expression = conditional.expression(TABLE_SCHEMA, TableMetadata.primaryIndexName());

        assertThat(expression.expression()).contains("BETWEEN");
    }

    @Test
    void expression_whenSecondKeySortValuesContainNull_throwsException() {
        Key key1 = Key.builder().partitionValue("test").sortValue("sortA").build();
        Key key2 = Key.builder()
                      .partitionValue("test")
                      .sortValues(java.util.Collections.singletonList(
                          software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().nul(true).build()))
                      .build();

        BetweenConditional conditional = new BetweenConditional(key1, key2);

        assertThatThrownBy(() -> conditional.expression(TABLE_SCHEMA, TableMetadata.primaryIndexName()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expression_whenSecondKeyHasNullSortValue_throwsException() {
        Key key1 = Key.builder().partitionValue("test").sortValue("sortA").build();
        Key key2 = Key.builder().partitionValue("test").build();

        BetweenConditional conditional = new BetweenConditional(key1, key2);

        assertThatThrownBy(() -> conditional.expression(TABLE_SCHEMA, TableMetadata.primaryIndexName()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expression_whenFirstKeyHasNullSortValue_throwsException() {
        Key key1 = Key.builder().partitionValue("test").build();
        Key key2 = Key.builder().partitionValue("test").sortValue("sortZ").build();

        BetweenConditional conditional = new BetweenConditional(key1, key2);

        assertThatThrownBy(() -> conditional.expression(TABLE_SCHEMA, TableMetadata.primaryIndexName()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expression_whenNumericSortValues_generatesExpression() {
        Key key1 = Key.builder().partitionValue("test").sortValue(100).build();
        Key key2 = Key.builder().partitionValue("test").sortValue(200).build();

        QueryConditional conditional = new BetweenConditional(key1, key2);
        Expression expression = conditional.expression(TABLE_SCHEMA, TableMetadata.primaryIndexName());

        assertThat(expression.expression()).contains("BETWEEN");
    }

    @Test
    void equals_whenKeysAreIdentical_returnsTrue() {
        Key key1 = Key.builder().partitionValue("test").sortValue("sortA").build();
        Key key2 = Key.builder().partitionValue("test").sortValue("sortZ").build();

        BetweenConditional conditional1 = new BetweenConditional(key1, key2);
        BetweenConditional conditional2 = new BetweenConditional(key1, key2);

        assertThat(conditional1).isEqualTo(conditional2);
    }

    @Test
    void equals_whenKeysAreDifferent_returnsFalse() {
        Key key1 = Key.builder().partitionValue("test").sortValue("sortA").build();
        Key key2 = Key.builder().partitionValue("test").sortValue("sortZ").build();
        Key key3 = Key.builder().partitionValue("test").sortValue("sortX").build();

        BetweenConditional conditional1 = new BetweenConditional(key1, key2);
        BetweenConditional conditional2 = new BetweenConditional(key1, key3);

        assertThat(conditional1).isNotEqualTo(conditional2);
    }

    @Test
    void hashCode_whenKeysAreIdentical_returnsSameHashCode() {
        Key key1 = Key.builder().partitionValue("test").sortValue("sortA").build();
        Key key2 = Key.builder().partitionValue("test").sortValue("sortZ").build();

        BetweenConditional conditional1 = new BetweenConditional(key1, key2);
        BetweenConditional conditional2 = new BetweenConditional(key1, key2);

        assertThat(conditional1.hashCode()).isEqualTo(conditional2.hashCode());
    }

    private static class TestItem {
        private String id;
        private String sort;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSort() {
            return sort;
        }

        public void setSort(String sort) {
            this.sort = sort;
        }
    }
}