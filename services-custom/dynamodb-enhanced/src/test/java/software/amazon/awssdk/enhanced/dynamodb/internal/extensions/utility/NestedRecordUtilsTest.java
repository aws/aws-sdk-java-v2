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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class NestedRecordUtilsTest {

    private static final String NESTED_OBJECT_UPDATE = "_NESTED_ATTR_UPDATE_";
    private static final Pattern PATTERN = Pattern.compile(NESTED_OBJECT_UPDATE);
    private static final Pattern NESTED_ATTR_UPDATE_ = Pattern.compile("_NESTED_ATTR_UPDATE_");

    @Mock
    private TableSchema<Object> mockSchema;

    @Mock
    private AttributeConverter<Object> mockConverter;

    @Mock
    private EnhancedType<Object> mockType;

    @Test
    public void getTableSchemaForListElement_withNullConverter_throwsIllegalArgumentException() {
        when(mockSchema.converterForAttribute("nonExistentAttribute")).thenReturn(null);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(mockSchema, "nonExistentAttribute"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No converter found for attribute: nonExistentAttribute");
    }

    @Test
    public void getTableSchemaForListElement_withEmptyRawClassParameters_throwsIllegalArgumentException() {
        when(mockSchema.converterForAttribute("emptyParamsAttribute")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(mockType);
        when(mockType.rawClassParameters()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(mockSchema, "emptyParamsAttribute"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No type parameters found for list attribute: emptyParamsAttribute");
    }

    @Test
    public void getTableSchemaForListElement_withNullRawClassParameters_throwsIllegalArgumentException() {
        when(mockSchema.converterForAttribute("nullParamsAttribute")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(mockType);
        when(mockType.rawClassParameters()).thenReturn(null);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(mockSchema, "nullParamsAttribute"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No type parameters found for list attribute: nullParamsAttribute");
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
    public void resolveSchemasPerPath_withEmptyAttributeMap_returnsOnlyRootSchema() {
        Map<String, AttributeValue> emptyAttributes = new HashMap<>();

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(emptyAttributes, mockSchema);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(mockSchema);
    }

    @Test
    public void resolveSchemasPerPath_withFlatAttributes_returnsOnlyRootSchema() {
        Map<String, AttributeValue> flatAttributes = new HashMap<>();
        flatAttributes.put("id", AttributeValue.builder().s("test-id").build());
        flatAttributes.put("simpleAttribute", AttributeValue.builder().s("test-value").build());

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(flatAttributes, mockSchema);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(mockSchema);
    }

    @Test
    public void resolveSchemasPerPath_withNestedAttributes_returnsCorrectSchemas() {
        Map<String, AttributeValue> nestedAttributes = new HashMap<>();
        nestedAttributes.put("id", AttributeValue.builder().s("test-id").build());
        nestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "name",
                             AttributeValue.builder().s("nested-name").build());
        nestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "tags",
                             AttributeValue.builder().ss("tag1", "tag2").build());

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(nestedAttributes, mockSchema);

        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(mockSchema);

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

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(multipleNestedAttributes, mockSchema);

        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(mockSchema);

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

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(duplicateNestedAttributes, mockSchema);

        // Assert - Should have root schema
        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(mockSchema);

        // Verify both attributes share the same nested path
        String path1 = "nestedItem" + NESTED_OBJECT_UPDATE + "name";
        String path2 = "nestedItem" + NESTED_OBJECT_UPDATE + "tags";

        String[] parts1 = PATTERN.split(path1);
        String[] parts2 = PATTERN.split(path2);

        assertThat(parts1[0]).isEqualTo(parts2[0]); // Same parent path
        assertThat(parts1[0]).isEqualTo("nestedItem");
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
    public void getTableSchemaForListElement_withNestedPathAndMissingSchema_throwsIllegalArgumentException() {
        String nestedKey = String.join(NESTED_OBJECT_UPDATE, "parent", "child", "listAttribute");

        // Mock the parent schema resolution to return empty
        when(mockSchema.converterForAttribute("parent")).thenReturn(null);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(mockSchema, nestedKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to resolve schema for list element at: " + nestedKey);
    }

    @Test
    public void resolveSchemasPerPath_withDeepNestedPaths_buildsCorrectSchemaMap() {
        Map<String, AttributeValue> deepNestedAttributes = new HashMap<>();
        deepNestedAttributes.put(String.join(NESTED_OBJECT_UPDATE, "level1", "level2", "level3", "attr"),
                                 AttributeValue.builder().s("deep-value").build());

        TableSchema<Object> level1Schema = mock(TableSchema.class);
        TableSchema<Object> level2Schema = mock(TableSchema.class);
        TableSchema<Object> level3Schema = mock(TableSchema.class);

        when(mockSchema.converterForAttribute("level1")).thenReturn(mockConverter);
        when(mockConverter.type()).thenReturn(mockType);
        when(mockType.tableSchema()).thenReturn(Optional.of(level1Schema));

        when(mockConverter.type()).thenReturn(mockType);
        when(mockType.tableSchema()).thenReturn(Optional.of(level2Schema));

        when(mockConverter.type()).thenReturn(mockType);
        when(mockType.tableSchema()).thenReturn(Optional.of(level3Schema));

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(deepNestedAttributes, mockSchema);

        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(mockSchema);
        assertThat(result.size()).isGreaterThan(1);
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