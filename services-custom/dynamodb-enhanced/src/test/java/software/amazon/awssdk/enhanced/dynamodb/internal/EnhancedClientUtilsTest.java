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
}