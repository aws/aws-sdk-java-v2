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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;

@RunWith(MockitoJUnitRunner.class)
public class EnhancedClientUtilsTest {
    private static final AttributeValue PARTITION_VALUE = AttributeValue.builder().s("id123").build();
    private static final AttributeValue SORT_VALUE = AttributeValue.builder().s("sort123").build();

    @Mock
    private TableSchema<Object> mockSchema;

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
    public void keyRef_withSimpleKey_returnsFormattedKey() {
        String result = EnhancedClientUtils.keyRef("simpleKey");

        assertThat(result).isEqualTo("#AMZN_MAPPED_simpleKey");
    }

    @Test
    public void keyRef_withSpecialCharacters_cleansAndFormatsKey() {
        String result = EnhancedClientUtils.keyRef("key*with.special-chars");

        assertThat(result).isEqualTo("#AMZN_MAPPED_key_with_special_chars");
    }

    @Test
    public void keyRef_withNestedKey_handlesNestedDelimiter() {
        String nestedKey = "parent_NESTED_ATTR_UPDATE_child";
        String result = EnhancedClientUtils.keyRef(nestedKey);

        assertThat(result).contains("#AMZN_MAPPED_");
        assertThat(result).contains("parent");
        assertThat(result).contains("child");
    }

    @Test
    public void valueRef_withSimpleValue_returnsFormattedValue() {
        String result = EnhancedClientUtils.valueRef("simpleValue");

        assertThat(result).isEqualTo(":AMZN_MAPPED_simpleValue");
    }

    @Test
    public void valueRef_withSpecialCharacters_cleansAndFormatsValue() {
        String result = EnhancedClientUtils.valueRef("value*with.special-chars");

        assertThat(result).isEqualTo(":AMZN_MAPPED_value_with_special_chars");
    }

    @Test
    public void valueRef_withNestedValue_handlesNestedDelimiter() {
        String nestedValue = "parent_NESTED_ATTR_UPDATE_child";
        String result = EnhancedClientUtils.valueRef(nestedValue);

        assertThat(result).startsWith(":AMZN_MAPPED_");
        assertThat(result).contains("parent");
        assertThat(result).contains("child");
    }

    @Test
    public void cleanAttributeName_withNoSpecialCharacters_returnsOriginal() {
        String original = "normalAttributeName123";
        String result = EnhancedClientUtils.cleanAttributeName(original);

        assertThat(result).isSameAs(original); // Should return same instance when no changes needed
    }

    @Test
    public void isNullAttributeValue_withNullAttributeValue_returnsTrue() {
        AttributeValue nullValue = AttributeValue.builder().nul(true).build();

        boolean result = EnhancedClientUtils.isNullAttributeValue(nullValue);

        assertThat(result).isTrue();
    }

    @Test
    public void isNullAttributeValue_withNonNullAttributeValue_returnsFalse() {
        AttributeValue stringValue = AttributeValue.builder().s("test").build();

        boolean result = EnhancedClientUtils.isNullAttributeValue(stringValue);

        assertThat(result).isFalse();
    }

    @Test
    public void isNullAttributeValue_withFalseNullValue_returnsFalse() {
        AttributeValue falseNullValue = AttributeValue.builder().nul(false).build();

        boolean result = EnhancedClientUtils.isNullAttributeValue(falseNullValue);

        assertThat(result).isFalse();
    }

    @Test
    public void createKeyFromItem_withPartitionKeyOnly_createsCorrectKey() {
        FakeItem item = new FakeItem();
        item.setId("test-id");

        Key result = EnhancedClientUtils.createKeyFromItem(item, FakeItem.getTableSchema(),
                                                           TableMetadata.primaryIndexName());

        assertThat(result.partitionKeyValue()).isEqualTo(AttributeValue.builder().s("test-id").build());
        assertThat(result.sortKeyValue()).isEmpty();
    }

    @Test
    public void createKeyFromItem_withPartitionAndSortKey_createsCorrectKey() {
        FakeItemWithSort item = new FakeItemWithSort();
        item.setId("test-id");
        item.setSort("test-sort");

        Key result = EnhancedClientUtils.createKeyFromItem(item, FakeItemWithSort.getTableSchema(),
                                                           TableMetadata.primaryIndexName());

        assertThat(result.partitionKeyValue()).isEqualTo(AttributeValue.builder().s("test-id").build());
        assertThat(result.sortKeyValue()).isPresent();
        assertThat(result.sortKeyValue().get()).isEqualTo(AttributeValue.builder().s("test-sort").build());
    }

    @Test
    public void readAndTransformSingleItem_withNullItemMap_returnsNull() {
        Object result = EnhancedClientUtils.readAndTransformSingleItem(null, mockSchema, null, null);

        assertThat(result).isNull();
    }

    @Test
    public void readAndTransformSingleItem_withEmptyItemMap_returnsNull() {
        Map<String, AttributeValue> emptyMap = Collections.emptyMap();

        Object result = EnhancedClientUtils.readAndTransformSingleItem(emptyMap, mockSchema, null, null);

        assertThat(result).isNull();
    }

    @Test
    public void getItemsFromSupplier_withNullList_returnsNull() {
        List<Object> result = EnhancedClientUtils.getItemsFromSupplier(null);

        assertThat(result).isNull();
    }

    @Test
    public void getItemsFromSupplier_withEmptyList_returnsNull() {
        List<Object> result = EnhancedClientUtils.getItemsFromSupplier(Collections.emptyList());

        assertThat(result).isNull();
    }
}