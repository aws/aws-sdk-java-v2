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

package software.amazon.awssdk.enhanced.dynamodb.internal.extensions.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.NestedBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class NestedRecordUtilsTest {

    private static final String NESTED_OBJECT_UPDATE = "_NESTED_ATTR_UPDATE_";
    private static final Pattern PATTERN = Pattern.compile(NESTED_OBJECT_UPDATE);
    private static final Pattern NESTED_ATTR_UPDATE_ = Pattern.compile("_NESTED_ATTR_UPDATE_");

    @Mock
    private TableSchema<NestedBean> objectSchema;

    @Mock
    private TableSchema<List<NestedBean>> listSchema;

    @Mock
    private AttributeConverter<NestedBean> objectConverter;

    @Mock
    private AttributeConverter<List<NestedBean>> listConverter;

    @Mock
    private EnhancedType<NestedBean> objectType;

    @Mock
    private EnhancedType<List<NestedBean>> listType;

    @Test
    public void getTableSchemaForListElement_withNullConverter_throwsIllegalArgumentException() {
        when(objectSchema.converterForAttribute("nonExistentAttribute")).thenReturn(null);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(objectSchema, "nonExistentAttribute"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No converter found for attribute: nonExistentAttribute");
    }

    @Test
    public void getTableSchemaForListElement_withEmptyRawClassParameters_throwsIllegalArgumentException() {
        when(objectSchema.converterForAttribute("emptyParamsAttribute")).thenReturn(objectConverter);
        when(objectConverter.type()).thenReturn(objectType);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(objectSchema, "emptyParamsAttribute"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No type parameters found for list attribute: emptyParamsAttribute");
    }

    @Test
    public void getTableSchemaForListElement_withNullRawClassParameters_throwsIllegalArgumentException() {
        when(objectSchema.converterForAttribute("nullParamsAttribute")).thenReturn(objectConverter);
        when(objectConverter.type()).thenReturn(objectType);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(objectSchema, "nullParamsAttribute"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No type parameters found for list attribute: nullParamsAttribute");
    }

    @Test
    public void getTableSchemaForListElement_withListConverter_returnsCorrectTableSchema() {
        List<EnhancedType<?>> parameters = Collections.singletonList(listType);
        when(listSchema.converterForAttribute("listAttribute")).thenReturn(listConverter);
        when(listConverter.type()).thenReturn(listType);
        when(listType.rawClass()).thenReturn((Class) NestedBean.class);
        when(listType.rawClassParameters()).thenReturn(parameters);

        TableSchema<?> result = NestedRecordUtils.getTableSchemaForListElement(listSchema, "listAttribute");
        ;
        assertThat(result).isInstanceOf(BeanTableSchema.class);
    }

    @Test
    public void getTableSchemaForListElement_withDeepNestedPath_returnsCorrectSchema() {
        String nestedKey = "nestedItem" + NESTED_OBJECT_UPDATE + "tags";
        String[] parts = PATTERN.split(nestedKey);

        assertThat(parts).hasSize(2);
        assertThat(parts[0]).isEqualTo("nestedItem");
        assertThat(parts[1]).isEqualTo("tags");

        String deepNestedKey = String.join(NESTED_OBJECT_UPDATE, "parent", "child", "grandchild");
        String[] deepParts = PATTERN.split(deepNestedKey);

        assertThat(deepParts).hasSize(3);
        assertThat(deepParts[0]).isEqualTo("parent");
        assertThat(deepParts[1]).isEqualTo("child");
        assertThat(deepParts[2]).isEqualTo("grandchild");
    }

    @Test
    public void getTableSchemaForListElement_withNestedPathAndMissingSchema_throwsIllegalArgumentException() {
        String nestedKey = String.join(NESTED_OBJECT_UPDATE, "parent", "child", "listAttribute");

        // Mock the parent schema resolution to return empty
        when(objectSchema.converterForAttribute("parent")).thenReturn(null);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(objectSchema, nestedKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to resolve schema for list element at: " + nestedKey);
    }

    @Test
    public void getTableSchemaForListElement_withStaticNestedSchema_returnsCorrectSchema() {
        TableSchema<NestedBean> childSchema = mock(TableSchema.class);
        AttributeConverter<NestedBean> childConverter = mock(AttributeConverter.class);
        EnhancedType<NestedBean> childType = mock(EnhancedType.class);

        when(objectSchema.converterForAttribute("child")).thenReturn(childConverter);
        when(childConverter.type()).thenReturn(childType);
        when(childType.tableSchema()).thenReturn(Optional.of(childSchema));

        TableSchema<?> result = NestedRecordUtils.getTableSchemaForListElement(objectSchema, "child");
        assertThat(result).isEqualTo(childSchema);
    }

    @Test
    public void getTableSchemaForListElement_withNestedPathAndPresentSchemas_returnsCorrectSchema() {
        String nestedKey = String.join(NESTED_OBJECT_UPDATE, "parent", "child", "listAttribute");

        TableSchema<NestedBean> level1Schema = mock(TableSchema.class);
        TableSchema<NestedBean> level2Schema = mock(TableSchema.class);
        TableSchema<NestedBean> expectedListElementSchema = mock(TableSchema.class);

        AttributeConverter<NestedBean> converter1 = mock(AttributeConverter.class);
        AttributeConverter<NestedBean> converter2 = mock(AttributeConverter.class);
        AttributeConverter<NestedBean> converter3 = mock(AttributeConverter.class);

        EnhancedType<NestedBean> type1 = mock(EnhancedType.class);
        EnhancedType<NestedBean> type2 = mock(EnhancedType.class);
        EnhancedType<NestedBean> type3 = mock(EnhancedType.class);

        // parent -> level1
        when(objectSchema.converterForAttribute("parent")).thenReturn(converter1);
        when(converter1.type()).thenReturn(type1);
        when(type1.tableSchema()).thenReturn(Optional.of(level1Schema));

        // level1 -> level2
        when(level1Schema.converterForAttribute("child")).thenReturn(converter2);
        when(converter2.type()).thenReturn(type2);
        when(type2.tableSchema()).thenReturn(Optional.of(level2Schema));

        // level2 -> list element schema
        when(level2Schema.converterForAttribute("listAttribute")).thenReturn(converter3);
        when(converter3.type()).thenReturn(type3);
        when(type3.tableSchema()).thenReturn(Optional.of(expectedListElementSchema));

        TableSchema<?> result = NestedRecordUtils.getTableSchemaForListElement(objectSchema, nestedKey);
        assertThat(result).isEqualTo(expectedListElementSchema);
    }

    @Test
    public void getTableSchemaForListElement_withClassNotFound_throwsIllegalArgumentException() {
        String badAttr = "badAttr";
        when(objectSchema.converterForAttribute(badAttr)).thenReturn(objectConverter);

        EnhancedType<NestedBean> enhancedType = mock(EnhancedType.class);
        EnhancedType<NestedBean> paramType = mock(EnhancedType.class);

        when(objectConverter.type()).thenReturn(enhancedType);
        when(enhancedType.rawClassParameters()).thenReturn(Collections.singletonList(paramType));

        when(paramType.tableSchema()).thenReturn(Optional.empty());
        when(paramType.rawClass()).thenReturn((Class) int.class);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(objectSchema, badAttr))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Class not found for field name: " + badAttr);
    }

    @Test
    public void resolveSchemasPerPath_withEmptyAttributeMap_returnsOnlyRootSchema() {
        Map<String, AttributeValue> emptyAttributes = new HashMap<>();

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(emptyAttributes, objectSchema);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(objectSchema);
    }

    @Test
    public void resolveSchemasPerPath_withFlatAttributes_returnsOnlyRootSchema() {
        Map<String, AttributeValue> flatAttributes = new HashMap<>();
        flatAttributes.put("id", AttributeValue.builder().s("test-id").build());
        flatAttributes.put("simpleAttribute", AttributeValue.builder().s("test-value").build());

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(flatAttributes, objectSchema);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(objectSchema);
    }

    @Test
    public void resolveSchemasPerPath_withNestedAttributes_returnsCorrectSchemas() {
        Map<String, AttributeValue> nestedAttributes = new HashMap<>();
        nestedAttributes.put("id", AttributeValue.builder().s("test-id").build());
        nestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "name",
                             AttributeValue.builder().s("nested-name").build());
        nestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "tags",
                             AttributeValue.builder().ss("tag1", "tag2").build());

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(nestedAttributes, objectSchema);

        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(objectSchema);

        boolean hasNestedPath = nestedAttributes.keySet().stream()
                                                .anyMatch(key -> key.contains(NESTED_OBJECT_UPDATE));
        assertThat(hasNestedPath).isTrue();
    }

    @Test
    public void resolveSchemasPerPath_withMultipleNestedPaths_returnsAllSchemas() {
        Map<String, AttributeValue> multipleNestedAttributes = new HashMap<>();
        multipleNestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "name",
                                     AttributeValue.builder().s("nested-name").build());
        multipleNestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "tags",
                                     AttributeValue.builder().ss("tag1", "tag2").build());

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(multipleNestedAttributes, objectSchema);

        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(objectSchema);

        String[] parts1 = NESTED_ATTR_UPDATE_.split("nestedItem_NESTED_ATTR_UPDATE_name");
        String[] parts2 = NESTED_ATTR_UPDATE_.split("nestedItem_NESTED_ATTR_UPDATE_tags");

        assertThat(parts1[0]).isEqualTo("nestedItem");
        assertThat(parts2[0]).isEqualTo("nestedItem");
        assertThat(parts1[0]).isEqualTo(parts2[0]); // Same nested path
    }

    @Test
    public void resolveSchemasPerPath_withDuplicateNestedPaths_doesNotDuplicateSchemas() {
        Map<String, AttributeValue> duplicateNestedAttributes = new HashMap<>();
        duplicateNestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "name",
                                      AttributeValue.builder().s("nested-name-1").build());
        duplicateNestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "tags",
                                      AttributeValue.builder().ss("tag1").build());

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(duplicateNestedAttributes, objectSchema);

        // Assert - Should have root schema
        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(objectSchema);

        // Verify both attributes share the same nested path
        String path1 = "nestedItem" + NESTED_OBJECT_UPDATE + "name";
        String path2 = "nestedItem" + NESTED_OBJECT_UPDATE + "tags";

        String[] parts1 = PATTERN.split(path1);
        String[] parts2 = PATTERN.split(path2);

        assertThat(parts1[0]).isEqualTo(parts2[0]); // Same parent path
        assertThat(parts1[0]).isEqualTo("nestedItem");
    }

    @Test
    public void resolveSchemasPerPath_withDeepNestedPaths_buildsCorrectSchemaMap() {
        Map<String, AttributeValue> deepNestedAttributes = new HashMap<>();
        deepNestedAttributes.put(String.join(NESTED_OBJECT_UPDATE, "level1", "level2", "level3", "attr"),
                                 AttributeValue.builder().s("deep-value").build());

        TableSchema<NestedBean> level1Schema = mock(TableSchema.class);
        TableSchema<NestedBean> level2Schema = mock(TableSchema.class);
        TableSchema<NestedBean> level3Schema = mock(TableSchema.class);

        when(objectSchema.converterForAttribute("level1")).thenReturn(objectConverter);
        when(objectConverter.type()).thenReturn(objectType);
        when(objectType.tableSchema()).thenReturn(Optional.of(level1Schema));

        when(objectConverter.type()).thenReturn(objectType);
        when(objectType.tableSchema()).thenReturn(Optional.of(level2Schema));

        when(objectConverter.type()).thenReturn(objectType);
        when(objectType.tableSchema()).thenReturn(Optional.of(level3Schema));

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(deepNestedAttributes, objectSchema);

        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(objectSchema);
        assertThat(result.size()).isGreaterThan(1);
    }

    @Test
    public void reconstructCompositeKey_withNullPath_returnsAttributeName() {
        String result = NestedRecordUtils.reconstructCompositeKey(null, "attributeName");

        assertThat(result).isEqualTo("attributeName");
    }

    @Test
    public void reconstructCompositeKey_withEmptyPath_returnsAttributeName() {
        String result = NestedRecordUtils.reconstructCompositeKey("", "attributeName");

        assertThat(result).isEqualTo("attributeName");
    }

    @Test
    public void reconstructCompositeKey_withNullAttributeName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> NestedRecordUtils.reconstructCompositeKey("parent", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attribute name cannot be null");
    }

    @Test
    public void reconstructCompositeKey_withSimplePath_returnsCompositeKey() {
        String result = NestedRecordUtils.reconstructCompositeKey("parent", "attributeName");

        assertThat(result).isEqualTo("parent" + NESTED_OBJECT_UPDATE + "attributeName");
    }

    @Test
    public void reconstructCompositeKey_withDottedPath_returnsCompositeKey() {
        String result = NestedRecordUtils.reconstructCompositeKey("parent.child", "attributeName");

        assertThat(result).isEqualTo(String.join(NESTED_OBJECT_UPDATE,
                                                 "parent", "child", "attributeName"));
    }

    @Test
    public void reconstructCompositeKey_withDeepDottedPath_returnsCompositeKey() {
        String result = NestedRecordUtils.reconstructCompositeKey("parent.child.grandchild", "attributeName");

        assertThat(result).isEqualTo(String.join(NESTED_OBJECT_UPDATE,
                                                 "parent", "child", "grandchild", "attributeName"));
    }

    @Test
    public void reconstructCompositeKey_withMultipleDots_handlesCorrectly() {
        String result = NestedRecordUtils.reconstructCompositeKey("a.b.c.d.e", "finalAttribute");

        String expected = String.join(NESTED_OBJECT_UPDATE, "a", "b", "c", "d", "e", "finalAttribute");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void reconstructCompositeKey_withWhitespaceInPath_preservesWhitespace() {
        String result = NestedRecordUtils.reconstructCompositeKey("parent with spaces.child with spaces", "attr");

        String expected = String.join(NESTED_OBJECT_UPDATE, "parent with spaces", "child with spaces", "attr");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void reconstructCompositeKey_withSpecialCharactersInPath_preservesCharacters() {
        String result = NestedRecordUtils.reconstructCompositeKey(
            "parent-with-dashes.child_with_underscores", "attr");

        String expected = String.join(NESTED_OBJECT_UPDATE, "parent-with-dashes", "child_with_underscores", "attr");
        assertThat(result).isEqualTo(expected);
    }
}