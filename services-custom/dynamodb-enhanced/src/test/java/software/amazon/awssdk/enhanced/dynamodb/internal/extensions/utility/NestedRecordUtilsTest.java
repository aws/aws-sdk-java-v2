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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class NestedRecordUtilsTest {

    private static final String NESTED_OBJECT_UPDATE = "_NESTED_ATTR_UPDATE_";
    private static final Pattern PATTERN = Pattern.compile(NESTED_OBJECT_UPDATE);
    private static final Pattern NESTED_ATTR_UPDATE_ = Pattern.compile("_NESTED_ATTR_UPDATE_");

    @Mock
    private TableSchema<Record> mockSchema;

    @Mock
    private EnhancedType<Record> mockEnhancedType;

    @Mock
    private AttributeConverter<Record> mockAttributeConverter;

    @Test
    public void getListElementSchemaCached_cacheNotPopulated_populatesCacheAndReturnsSchema() {
        TableSchema<Record> rootSchema = TableSchema.fromBean(Record.class);
        Map<NestedRecordUtils.SchemaLookupKey, TableSchema<?>> cache = new HashMap<>();
        TableSchema<?> result = NestedRecordUtils.getListElementSchemaCached(cache, rootSchema, "children");

        assertNotNull(result);
        assertThat(result.itemType().rawClass()).isEqualTo(ChildRecord.class);

        NestedRecordUtils.SchemaLookupKey expectedKey =
            new NestedRecordUtils.SchemaLookupKey(rootSchema, "children");

        assertThat(cache).containsKey(expectedKey);
        assertThat(cache.get(expectedKey)).isSameAs(result);
    }

    @Test
    public void getListElementSchemaCached_whenElementFoundInCache_returnsCachedSchema() {
        TableSchema<Record> rootSchema = TableSchema.fromBean(Record.class);
        TableSchema<?> cachedSchema = TableSchema.fromBean(ChildRecord.class);

        Map<NestedRecordUtils.SchemaLookupKey, TableSchema<?>> cache = new HashMap<>();
        NestedRecordUtils.SchemaLookupKey key = new NestedRecordUtils.SchemaLookupKey(rootSchema, "children");
        cache.put(key, cachedSchema);

        TableSchema<?> result = NestedRecordUtils.getListElementSchemaCached(cache, rootSchema, "children");
        assertThat(result).isSameAs(cachedSchema);
        assertThat(cache.get(key)).isSameAs(cachedSchema);
    }

    @Test
    public void getTableSchemaForListElement_simpleHierarchy_resolvesCorrectSchema() {
        TableSchema<Record> rootSchema = TableSchema.fromBean(Record.class);

        TableSchema<?> result = NestedRecordUtils.getTableSchemaForListElement(rootSchema, "children");

        assertNotNull(result);
        assertThat(result.itemType().rawClass()).isEqualTo(ChildRecord.class);
    }

    @Test
    public void getTableSchemaForListElement_nestedHierarchy_resolvesCorrectSchema() {
        TableSchema<Record> rootSchema = TableSchema.fromBean(Record.class);
        String key = "nestedItem" + NESTED_OBJECT_UPDATE + "children";

        TableSchema<?> result = NestedRecordUtils.getTableSchemaForListElement(rootSchema, key);

        assertNotNull(result);
        assertThat(result.itemType().rawClass()).isEqualTo(ChildRecord.class);
    }

    @Test
    public void getTableSchemaForListElement_withRawClassParameters_returnsCorrectSchema() {
        List<EnhancedType<?>> rawClassParameters = Collections.singletonList(EnhancedType.of(ChildRecord.class));
        when(mockSchema.converterForAttribute("child")).thenReturn(mockAttributeConverter);
        when(mockAttributeConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.rawClassParameters()).thenReturn(rawClassParameters);

        TableSchema<?> result = NestedRecordUtils.getTableSchemaForListElement(mockSchema, "child");

        assertNotNull(result);
        assertThat(result.itemType().rawClass()).isEqualTo(ChildRecord.class);
    }

    @Test
    public void getTableSchemaForListElement_withNullConverter_throwsIllegalArgumentException() {
        when(mockSchema.converterForAttribute("nonExistentAttribute")).thenReturn(null);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(mockSchema, "nonExistentAttribute"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No converter found for attribute: nonExistentAttribute");
    }

    @Test
    public void getTableSchemaForListElement_withEmptyRawClassParameters_throwsIllegalArgumentException() {
        when(mockSchema.converterForAttribute("emptyParamsAttribute")).thenReturn(mockAttributeConverter);
        when(mockAttributeConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.rawClassParameters()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(mockSchema, "emptyParamsAttribute"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No type parameters found for list attribute: emptyParamsAttribute");
    }

    @Test
    public void getTableSchemaForListElement_withNullRawClassParameters_throwsIllegalArgumentException() {
        when(mockSchema.converterForAttribute("nullParamsAttribute")).thenReturn(mockAttributeConverter);
        when(mockAttributeConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.rawClassParameters()).thenReturn(null);

        assertThatThrownBy(() -> NestedRecordUtils.getTableSchemaForListElement(mockSchema, "nullParamsAttribute"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No type parameters found for list attribute: nullParamsAttribute");
    }

    @Test
    public void getTableSchemaForListElement_withDeepNestedPath_returnsCorrectParts() {
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
        assertThat(parts1[0]).isEqualTo(parts2[0]);
    }

    @Test
    public void resolveSchemasPerPath_withDuplicateNestedPaths_doesNotDuplicateSchemas() {
        Map<String, AttributeValue> duplicateNestedAttributes = new HashMap<>();
        duplicateNestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "name",
                                      AttributeValue.builder().s("nested-name-1").build());
        duplicateNestedAttributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "tags",
                                      AttributeValue.builder().ss("tag1").build());

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(duplicateNestedAttributes, mockSchema);

        assertThat(result).containsKey("");
        assertThat(result.get("")).isEqualTo(mockSchema);

        String path1 = "nestedItem" + NESTED_OBJECT_UPDATE + "name";
        String path2 = "nestedItem" + NESTED_OBJECT_UPDATE + "tags";

        String[] parts1 = PATTERN.split(path1);
        String[] parts2 = PATTERN.split(path2);

        assertThat(parts1[0]).isEqualTo(parts2[0]);
        assertThat(parts1[0]).isEqualTo("nestedItem");
    }

    @Test
    public void resolveSchemasPerPath_withRepeatedNestedPrefix_getsSchemaFromCache() {
        TableSchema<Record> rootSchema = TableSchema.fromBean(Record.class);

        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "children",
                       AttributeValue.builder().l(Collections.emptyList()).build());
        attributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "children",
                       AttributeValue.builder().l(Collections.emptyList()).build());

        attributes.clear();
        attributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "children",
                       AttributeValue.builder().l(Collections.emptyList()).build());
        attributes.put("nestedItem" + NESTED_OBJECT_UPDATE + "children2",
                       AttributeValue.builder().l(Collections.emptyList()).build());

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(attributes, rootSchema);

        assertThat(result).containsEntry("", rootSchema);
        assertThat(result).containsKey("nestedItem");
        assertThat(result.get("nestedItem").itemType().rawClass()).isEqualTo(Record.class);
    }

    @Test
    public void resolveSchemasPerPath_withRepeatedDeepNestedPrefix_resolvesCorrectSchema() {
        TableSchema<Record> rootSchema = TableSchema.fromBean(Record.class);

        String prefix = "nestedItem" + NESTED_OBJECT_UPDATE + "nestedItem" + NESTED_OBJECT_UPDATE;

        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(prefix + "children", AttributeValue.builder().l(Collections.emptyList()).build());
        attributes.put(prefix + "children2", AttributeValue.builder().l(Collections.emptyList()).build());

        Map<String, TableSchema<?>> result = NestedRecordUtils.resolveSchemasPerPath(attributes, rootSchema);

        assertThat(result).containsEntry("", rootSchema);
        assertThat(result).containsKey("nestedItem");
        assertThat(result).containsKey("nestedItem.nestedItem");
        assertThat(result.get("nestedItem").itemType().rawClass()).isEqualTo(Record.class);
        assertThat(result.get("nestedItem.nestedItem").itemType().rawClass()).isEqualTo(Record.class);
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

        TableSchema<Record> level1Schema = mock(TableSchema.class);
        TableSchema<Record> level2Schema = mock(TableSchema.class);
        TableSchema<Record> level3Schema = mock(TableSchema.class);

        when(mockSchema.converterForAttribute("level1")).thenReturn(mockAttributeConverter);
        when(mockAttributeConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.tableSchema()).thenReturn(Optional.of(level1Schema));

        when(mockAttributeConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.tableSchema()).thenReturn(Optional.of(level2Schema));

        when(mockAttributeConverter.type()).thenReturn(mockEnhancedType);
        when(mockEnhancedType.tableSchema()).thenReturn(Optional.of(level3Schema));

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

    @DynamoDbBean
    public static class Record {
        private Record nestedItem;
        private List<ChildRecord> children;

        public Record getNestedItem() {
            return nestedItem;
        }

        public void setNestedItem(Record nestedItem) {
            this.nestedItem = nestedItem;
        }

        public List<ChildRecord> getChildren() {
            return children;
        }

        public Record setChildren(List<ChildRecord> children) {
            this.children = children;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Record record = (Record) o;
            return Objects.equals(nestedItem, record.nestedItem) &&
                   Objects.equals(children, record.children);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nestedItem, children);
        }
    }

    @DynamoDbBean
    public static class ChildRecord {

        private List<String> attribute;

        public List<String> getAttribute() {
            return attribute;
        }

        public ChildRecord setAttribute(List<String> attribute) {
            this.attribute = attribute;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ChildRecord that = (ChildRecord) o;
            return Objects.equals(attribute, that.attribute);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(attribute);
        }
    }
}