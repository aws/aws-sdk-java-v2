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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;

public class EnhancedClientUtilsTest {
    private static final AttributeValue PARTITION_VALUE = AttributeValue.builder().s("id123").build();
    private static final AttributeValue SORT_VALUE = AttributeValue.builder().s("sort123").build();

    @Test
    public void createKeyFromMap_partitionOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", PARTITION_VALUE);

        Key key = EnhancedClientUtils.createKeyFromMap(itemMap,
                                                       FakeItem.getTableSchema(),
                                                       TableMetadata.primaryIndexName());

        assertThat(key.partitionKeyValue()).isEqualTo(PARTITION_VALUE);
        assertThat(key.sortKeyValue()).isEmpty();
    }

    @Test
    public void createKeyFromMap_partitionAndSort() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", PARTITION_VALUE);
        itemMap.put("sort", SORT_VALUE);

        Key key = EnhancedClientUtils.createKeyFromMap(itemMap,
                                                       FakeItemWithSort.getTableSchema(),
                                                       TableMetadata.primaryIndexName());

        assertThat(key.partitionKeyValue()).isEqualTo(PARTITION_VALUE);
        assertThat(key.sortKeyValue()).isEqualTo(Optional.of(SORT_VALUE));
    }

    @Test
    public void cleanAttributeName_cleansSpecialCharacters() {
        String result = EnhancedClientUtils.cleanAttributeName("a*b.c-d:e#f+g:h/i(j)k&l<m>n?o=p!q@r%s$t|u");
        
        assertThat(result).isEqualTo("a_b_c_d_e_f_g_h_i_j_k_l_m_n_o_p_q_r_s_t_u");
    }

    @Test
    public void keyRef_simpleAttributeName_returnsCorrectReference() {
        assertThat(EnhancedClientUtils.keyRef("simpleName"))
            .isEqualTo("#AMZN_MAPPED_simpleName");
    }

    @Test
    public void keyRef_attributeNameWithSpecialCharacters_returnsCleanedReference() {
        assertThat(EnhancedClientUtils.keyRef("a*b.c"))
            .isEqualTo("#AMZN_MAPPED_a_b_c");
    }

    @Test
    public void keyRef_nestedAttributeName_returnsNestedReference() {
        String result = EnhancedClientUtils.keyRef("foo.nested.bar");
        assertThat(result).contains("#AMZN_MAPPED_");
    }

    @Test
    public void valueRef_simpleAttributeName_returnsCorrectReference() {
        assertThat(EnhancedClientUtils.valueRef("simpleName"))
            .isEqualTo(":AMZN_MAPPED_simpleName");
    }

    @Test
    public void valueRef_attributeNameWithSpecialCharacters_returnsCleanedReference() {
        assertThat(EnhancedClientUtils.valueRef("a*b.c"))
            .isEqualTo(":AMZN_MAPPED_a_b_c");
    }

    @Test
    public void valueRef_nestedAttributeName_returnsNestedReference() {
        String result = EnhancedClientUtils.valueRef("foo.nested.bar");
        assertThat(result).contains(":AMZN_MAPPED_");
    }

    @Test
    public void readAndTransformSingleItem_nullItemMap_returnsNull() {
        assertThat(
            EnhancedClientUtils.readAndTransformSingleItem(
                null,
                FakeItem.getTableSchema(),
                null,
                null))
            .isNull();
    }

    @Test
    public void readAndTransformSingleItem_emptyItemMap_returnsNull() {
        assertThat(
            EnhancedClientUtils.readAndTransformSingleItem(
                Collections.emptyMap(),
                FakeItem.getTableSchema(),
                null,
                null))
            .isNull();
    }

    @Test
    public void readAndTransformSingleItem_withExtensionAndTransformedItem_returnsTransformedItem() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", PARTITION_VALUE);
        DynamoDbEnhancedClientExtension extension = new DynamoDbEnhancedClientExtension() {
            @Override
            public ReadModification afterRead(DynamoDbExtensionContext.AfterRead context) {
                return ReadModification.builder().transformedItem(itemMap).build();
            }
        };

        assertThat(
            EnhancedClientUtils.readAndTransformSingleItem(
                itemMap,
                FakeItem.getTableSchema(),
                null,
                extension))
            .isNotNull();
    }

    @Test
    public void readAndTransformSingleItem_withExtensionNoTransformedItem_returnsOriginalItem() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", PARTITION_VALUE);
        DynamoDbEnhancedClientExtension extension = new DynamoDbEnhancedClientExtension() {
            @Override
            public ReadModification afterRead(DynamoDbExtensionContext.AfterRead context) {
                return ReadModification.builder().build();
            }
        };

        assertThat(
            EnhancedClientUtils.readAndTransformSingleItem(
                itemMap,
                FakeItem.getTableSchema(),
                null, extension))
            .isNotNull();
    }

    @Test
    public void readAndTransformPaginatedItems_withAllFields_returnsCompletePage() {
        class TestResponse {
            List<Map<String, AttributeValue>> items;
            Map<String, AttributeValue> lastKey;
            int count;
            int scannedCount;
            ConsumedCapacity consumedCapacity;
        }
        TestResponse response = new TestResponse();
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", PARTITION_VALUE);
        response.items = Collections.singletonList(itemMap);
        response.lastKey = new HashMap<>();
        response.lastKey.put("id", PARTITION_VALUE);
        response.count = 1;
        response.scannedCount = 1;
        response.consumedCapacity = null;

        Page<FakeItem> page = EnhancedClientUtils.readAndTransformPaginatedItems(
            response,
            FakeItem.getTableSchema(),
            null,
            null,
            r -> r.items,
            r -> r.lastKey,
            r -> r.count,
            r -> r.scannedCount,
            r -> r.consumedCapacity
        );

        assertThat(page.items()).hasSize(1);
        assertThat(page.count()).isEqualTo(1);
        assertThat(page.scannedCount()).isEqualTo(1);
        assertThat(page.lastEvaluatedKey()).isEqualTo(response.lastKey);
    }

    @Test
    public void createKeyFromItem_partitionKeyOnly_returnsKeyWithPartitionOnly() {
        FakeItem item = new FakeItem();
        item.setId("id123");

        Key key = EnhancedClientUtils.createKeyFromItem(
            item,
            FakeItem.getTableSchema(),
            TableMetadata.primaryIndexName());

        assertThat(Objects.requireNonNull(key.partitionKeyValue()).s()).isEqualTo("id123");
        assertThat(key.sortKeyValue()).isEmpty();
    }

    @Test
    public void createKeyFromItem_partitionAndSortKey_returnsKeyWithBoth() {
        FakeItemWithSort item = new FakeItemWithSort();
        item.setId("id123");
        item.setSort("sort123");

        Key key = EnhancedClientUtils.createKeyFromItem(
            item,
            FakeItemWithSort.getTableSchema(),
            TableMetadata.primaryIndexName());

        assertThat(Objects.requireNonNull(key.partitionKeyValue()).s()).isEqualTo("id123");
        assertThat(key.sortKeyValue().get().s()).isEqualTo("sort123");
    }

    @Test
    public void createKeyFromMap_missingPartitionKey_throwsException() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("sort", SORT_VALUE);

        try {
            EnhancedClientUtils.createKeyFromMap(itemMap,
                                                 FakeItemWithSort.getTableSchema(),
                                                 TableMetadata.primaryIndexName());
            assertThat(false).as("Expected IllegalArgumentException").isTrue();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("partitionValue should not be null");
        }
    }

    @Test
    public void getItemsFromSupplier_nullSupplierList_returnsNull() {
        assertThat(EnhancedClientUtils.getItemsFromSupplier(null)).isNull();
    }

    @Test
    public void getItemsFromSupplier_emptySupplierList_returnsNull() {
        assertThat(EnhancedClientUtils.getItemsFromSupplier(Collections.emptyList())).isNull();
    }

    @Test
    public void getItemsFromSupplier_nonEmptySupplierList_returnsItems() {
        assertThat(
            EnhancedClientUtils.getItemsFromSupplier(Collections.singletonList(() -> "item")))
            .containsExactly("item");
    }

    @Test
    public void isNullAttributeValue_nullAttribute_returnsTrue() {
        AttributeValue nullAttr = AttributeValue.builder().nul(true).build();
        assertThat(EnhancedClientUtils.isNullAttributeValue(nullAttr)).isTrue();
    }

    @Test
    public void isNullAttributeValue_nonNullAttribute_returnsFalse() {
        AttributeValue notNullAttr = AttributeValue.builder().s("value").build();
        assertThat(EnhancedClientUtils.isNullAttributeValue(notNullAttr)).isFalse();
    }

    @Test
    public void isNullAttributeValue_nullAttributeValueObject_throwsNullPointerException() {
        try {
            EnhancedClientUtils.isNullAttributeValue(null);
            assertThat(false).as("Expected NullPointerException").isTrue();
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void isNullAttributeValue_attributeWithNulFalse_returnsFalse() {
        AttributeValue attr = AttributeValue.builder().nul(false).build();
        assertThat(EnhancedClientUtils.isNullAttributeValue(attr)).isFalse();
    }

    @Test
    public void readAndTransformSingleItem_withExtensionReturningNull_returnsOriginalItem() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", PARTITION_VALUE);
        DynamoDbEnhancedClientExtension extension = new DynamoDbEnhancedClientExtension() {
            @Override
            public ReadModification afterRead(DynamoDbExtensionContext.AfterRead context) {
                return null;
            }
        };

        assertThat(
            EnhancedClientUtils.readAndTransformSingleItem(
                itemMap,
                FakeItem.getTableSchema(),
                null, extension))
            .isNotNull();
    }

    @Test
    public void keyRef_nestedAttributeWithSpecialChars_returnsNestedReference() {
        String result = EnhancedClientUtils.keyRef("foo[0].bar");
        assertThat(result).contains("#AMZN_MAPPED_");
    }

    @Test
    public void valueRef_nestedAttributeWithSpecialChars_returnsNestedReference() {
        String result = EnhancedClientUtils.valueRef("foo[0].bar");
        assertThat(result).contains(":AMZN_MAPPED_");
    }

    @Test
    public void createKeyFromItem_tableWithoutSortKey_returnsKeyWithPartitionOnly() {
        FakeItem item = new FakeItem();
        item.setId("id123");

        Key key = EnhancedClientUtils.createKeyFromItem(item, FakeItem.getTableSchema(), TableMetadata.primaryIndexName());

        assertThat(Objects.requireNonNull(key.partitionKeyValue()).s()).isEqualTo("id123");
        assertThat(key.sortKeyValue()).isEmpty();
    }

    @Test
    public void createKeyFromMap_tableWithoutSortKey_returnsKeyWithPartitionOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", PARTITION_VALUE);

        Key key = EnhancedClientUtils.createKeyFromMap(itemMap, FakeItem.getTableSchema(), TableMetadata.primaryIndexName());

        assertThat(key.partitionKeyValue()).isEqualTo(PARTITION_VALUE);
        assertThat(key.sortKeyValue()).isEmpty();
    }
}