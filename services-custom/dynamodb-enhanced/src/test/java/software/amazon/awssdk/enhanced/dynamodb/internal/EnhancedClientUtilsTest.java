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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class EnhancedClientUtilsTest {
    private static final AttributeValue PARTITION_VALUE = AttributeValue.builder().s("id123").build();
    private static final AttributeValue SORT_VALUE = AttributeValue.builder().s("sort123").build();

    @Mock
    private TableSchema<Object> mockSchema;

    @Mock
    private AttributeConverter<Object> mockConverter;

    @Mock
    private EnhancedType<Object> mockEnhancedType;

    @Mock
    private EnhancedType<Object> mockParameterType;

    @Mock
    private TableSchema<Object> mockNestedSchema;

    @Test
    public void hasMap_returnsTrue() {
        AttributeValue nullValue = AttributeValue.builder().nul(false).m(new HashMap<>()).build();

        boolean result = EnhancedClientUtils.hasMap(nullValue);

        assertThat(result).isTrue();
    }

    @Test
    public void hasMap_forNullAttributeValue_returnsFalse() {
        AttributeValue nullValue = AttributeValue.builder().nul(true).build();

        boolean result = EnhancedClientUtils.hasMap(nullValue);

        assertThat(result).isFalse();
    }

    @Test
    public void hasMap_forNotNullAttributeValueWithoutMap_returnsFalse() {
        AttributeValue nullValue = AttributeValue.builder().nul(false).build();

        boolean result = EnhancedClientUtils.hasMap(nullValue);

        assertThat(result).isFalse();
    }

    @Test
    public void hasMap_forAttributeValueNull_returnsFalse() {

        boolean result = EnhancedClientUtils.hasMap(null);

        assertThat(result).isFalse();
    }

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
    public void getNestedSchema_withNullConverter_returnsEmpty() {
        when(mockSchema.converterForAttribute("nonExistentAttribute")).thenReturn(null);

        Optional<? extends TableSchema<?>> result =
            EnhancedClientUtils.getNestedSchema(mockSchema, "nonExistentAttribute");

        assertThat(result).isEmpty();
    }

    @Test
    public void getNestedSchema_withNullParentSchema_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> EnhancedClientUtils.getNestedSchema(null, "attributeName"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Parent schema cannot be null");
    }

    @Test
    public void getNestedSchema_withNullAttributeName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> EnhancedClientUtils.getNestedSchema(mockSchema, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attribute name cannot be null or empty");
    }

    @Test
    public void getNestedSchema_withEmptyAttributeName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> EnhancedClientUtils.getNestedSchema(mockSchema, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attribute name cannot be null or empty");
    }

    @Test
    public void getNestedSchema_withWhitespaceAttributeName_doesNotThrow() {
        when(mockSchema.converterForAttribute("   ")).thenReturn(null);

        Optional<? extends TableSchema<?>> result = EnhancedClientUtils.getNestedSchema(mockSchema, "   ");

        assertThat(result).isEmpty();
    }

    @Test
    public void getNestedSchema_withNullEnhancedType_returnsEmpty() {
        when(mockSchema.converterForAttribute("attributeWithNullType")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(null);

        Optional<? extends TableSchema<?>> result =
            EnhancedClientUtils.getNestedSchema(mockSchema, "attributeWithNullType");

        assertThat(result).isEmpty();
    }

    @Test
    public void getNestedSchema_withParameterizedType_extractsFirstParameter() {
        List<EnhancedType<?>> parameters = Collections.singletonList(mockParameterType);
        when(mockSchema.converterForAttribute("listAttribute")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.rawClassParameters()).thenReturn(parameters);
        when(mockParameterType.tableSchema()).thenReturn(Optional.of(mockNestedSchema));

        Optional<? extends TableSchema<?>> result = EnhancedClientUtils.getNestedSchema(mockSchema, "listAttribute");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockNestedSchema);
    }

    @Test
    public void getNestedSchema_withEmptyParameters_usesOriginalType() {
        when(mockSchema.converterForAttribute("simpleAttribute")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.rawClassParameters()).thenReturn(Collections.emptyList());
        when(mockEnhancedType.tableSchema()).thenReturn(Optional.of(mockNestedSchema));

        Optional<? extends TableSchema<?>> result =
            EnhancedClientUtils.getNestedSchema(mockSchema, "simpleAttribute");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockNestedSchema);
    }

    @Test
    public void getNestedSchema_withNullParameters_usesOriginalType() {
        when(mockSchema.converterForAttribute("simpleAttribute")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.rawClassParameters()).thenReturn(null);
        when(mockEnhancedType.tableSchema()).thenReturn(Optional.of(mockNestedSchema));

        Optional<? extends TableSchema<?>> result =
            EnhancedClientUtils.getNestedSchema(mockSchema, "simpleAttribute");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockNestedSchema);
    }

    @Test
    public void getNestedSchema_withNoTableSchema_returnsEmpty() {
        when(mockSchema.converterForAttribute("attributeWithoutSchema")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.rawClassParameters()).thenReturn(Collections.emptyList());
        when(mockEnhancedType.tableSchema()).thenReturn(Optional.empty());

        Optional<? extends TableSchema<?>> result = EnhancedClientUtils.getNestedSchema(mockSchema, "attributeWithoutSchema");

        assertThat(result).isEmpty();
    }

    @Test
    public void getNestedSchema_withParameterizedTypeNoTableSchema_returnsEmpty() {
        List<EnhancedType<?>> parameters = Collections.singletonList(mockParameterType);
        when(mockSchema.converterForAttribute("listAttributeNoSchema")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.rawClassParameters()).thenReturn(parameters);
        when(mockParameterType.tableSchema()).thenReturn(Optional.empty());

        Optional<? extends TableSchema<?>> result =
            EnhancedClientUtils.getNestedSchema(mockSchema, "listAttributeNoSchema");

        assertThat(result).isEmpty();
    }

    @Test
    public void getNestedSchema_withValidInputs_returnsNestedSchema() {
        when(mockSchema.converterForAttribute("validAttribute")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.rawClassParameters()).thenReturn(Collections.emptyList());
        when(mockEnhancedType.tableSchema()).thenReturn(Optional.of(mockNestedSchema));

        Optional<? extends TableSchema<?>> result = EnhancedClientUtils.getNestedSchema(mockSchema, "validAttribute");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockNestedSchema);
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