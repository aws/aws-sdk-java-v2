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

package software.amazon.awssdk.enhanced.dynamodb.internal.immutable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class MetaTableSchemaTest {
    private final MetaTableSchema<FakeItem> metaTableSchema = MetaTableSchema.create(FakeItem.class);
    private final FakeItem fakeItem = FakeItem.createUniqueFakeItem();
    private final Map<String, AttributeValue> fakeMap =
        Collections.singletonMap("test", AttributeValue.builder().s("test").build());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private TableSchema<FakeItem> mockTableSchema;

    @Mock
    private EnhancedType<FakeItem> mockEnhancedType;

    @Test
    public void mapToItem() {
        metaTableSchema.initialize(mockTableSchema);
        when(mockTableSchema.mapToItem(any())).thenReturn(fakeItem);

        assertThat(metaTableSchema.mapToItem(fakeMap)).isSameAs(fakeItem);
        verify(mockTableSchema).mapToItem(fakeMap);
    }

    @Test
    public void mapToItem_notInitialized() {
        assertUninitialized(t -> t.mapToItem(fakeMap));
    }

    @Test
    public void itemToMap_ignoreNulls() {
        metaTableSchema.initialize(mockTableSchema);
        when(mockTableSchema.itemToMap(any(FakeItem.class), any(boolean.class))).thenReturn(fakeMap);

        assertThat(metaTableSchema.itemToMap(fakeItem, true)).isSameAs(fakeMap);
        verify(mockTableSchema).itemToMap(fakeItem, true);
        assertThat(metaTableSchema.itemToMap(fakeItem, false)).isSameAs(fakeMap);
        verify(mockTableSchema).itemToMap(fakeItem, false);
    }

    @Test
    public void itemToMap_ignoreNulls_notInitialized() {
        assertUninitialized(t -> t.itemToMap(fakeItem, true));
    }

    @Test
    public void itemToMap_attributes() {
        Collection<String> attributes = Collections.singletonList("test-attribute");

        metaTableSchema.initialize(mockTableSchema);
        when(mockTableSchema.itemToMap(any(FakeItem.class), any())).thenReturn(fakeMap);

        assertThat(metaTableSchema.itemToMap(fakeItem, attributes)).isSameAs(fakeMap);
        verify(mockTableSchema).itemToMap(fakeItem, attributes);
    }

    @Test
    public void itemToMap_attributes_notInitialized() {
        assertUninitialized(t -> t.itemToMap(fakeItem, null));
    }

    @Test
    public void attributeValue() {
        AttributeValue attributeValue = AttributeValue.builder().s("test-attribute").build();

        metaTableSchema.initialize(mockTableSchema);
        when(mockTableSchema.attributeValue(any(), any())).thenReturn(attributeValue);

        assertThat(metaTableSchema.attributeValue(fakeItem, "test-name")).isSameAs(attributeValue);
        verify(mockTableSchema).attributeValue(fakeItem, "test-name");
    }

    @Test
    public void attributeValue_notInitialized() {
        assertUninitialized(t -> t.attributeValue(fakeItem, "test"));
    }

    @Test
    public void tableMetadata() {
        TableMetadata mockTableMetadata = Mockito.mock(TableMetadata.class);

        metaTableSchema.initialize(mockTableSchema);
        when(mockTableSchema.tableMetadata()).thenReturn(mockTableMetadata);

        assertThat(metaTableSchema.tableMetadata()).isSameAs(mockTableMetadata);
        verify(mockTableSchema).tableMetadata();
    }

    @Test
    public void tableMetadata_notInitialized() {
        assertUninitialized(TableSchema::tableMetadata);
    }

    @Test
    public void itemType() {
        metaTableSchema.initialize(mockTableSchema);
        when(mockTableSchema.itemType()).thenReturn(mockEnhancedType);

        assertThat(metaTableSchema.itemType()).isSameAs(mockEnhancedType);
        verify(mockTableSchema).itemType();
    }

    @Test
    public void itemType_notInitialized() {
        assertUninitialized(TableSchema::itemType);
    }

    @Test
    public void attributeNames() {
        List<String> attributeNames = Collections.singletonList("attribute-names");

        metaTableSchema.initialize(mockTableSchema);
        when(mockTableSchema.attributeNames()).thenReturn(attributeNames);

        assertThat(metaTableSchema.attributeNames()).isSameAs(attributeNames);
        verify(mockTableSchema).attributeNames();
    }

    @Test
    public void attributeNames_notInitialized() {
        assertUninitialized(TableSchema::attributeNames);
    }

    @Test
    public void isAbstract() {
        metaTableSchema.initialize(mockTableSchema);

        when(mockTableSchema.isAbstract()).thenReturn(true);
        assertThat(metaTableSchema.isAbstract()).isTrue();
        verify(mockTableSchema).isAbstract();

        when(mockTableSchema.isAbstract()).thenReturn(false);
        assertThat(metaTableSchema.isAbstract()).isFalse();
        verify(mockTableSchema, times(2)).isAbstract();
    }

    @Test
    public void isAbstract_notInitialized() {
        assertUninitialized(TableSchema::isAbstract);
    }

    @Test
    public void doubleInitialize_throwsIllegalStateException() {
        metaTableSchema.initialize(mockTableSchema);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("initialized");
        metaTableSchema.initialize(mockTableSchema);
    }

    @Test
    public void isInitialized_uninitialized() {
        assertThat(metaTableSchema.isInitialized()).isFalse();
    }

    @Test
    public void isInitialized_initialized() {
        metaTableSchema.initialize(mockTableSchema);
        assertThat(metaTableSchema.isInitialized()).isTrue();
    }

    @Test
    public void concreteTableSchema_uninitialized() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("must be initialized");
        metaTableSchema.concreteTableSchema();
    }

    @Test
    public void concreteTableSchema_initialized() {
        metaTableSchema.initialize(mockTableSchema);
        assertThat(metaTableSchema.concreteTableSchema()).isSameAs(mockTableSchema);
    }

    private void assertUninitialized(Consumer<TableSchema<FakeItem>> methodToTest) {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("must be initialized");
        methodToTest.accept(metaTableSchema);
    }
}