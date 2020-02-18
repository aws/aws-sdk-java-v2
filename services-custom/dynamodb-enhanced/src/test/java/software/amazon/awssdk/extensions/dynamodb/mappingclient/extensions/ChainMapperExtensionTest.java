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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem.createUniqueFakeItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;

@RunWith(MockitoJUnitRunner.class)
public class ChainMapperExtensionTest {
    private static final String TABLE_NAME = "concrete-table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        OperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private static final Map<String, AttributeValue> ATTRIBUTE_VALUES_1 =
        Collections.unmodifiableMap(Collections.singletonMap("key1", AttributeValue.builder().s("1").build()));
    private static final Map<String, AttributeValue> ATTRIBUTE_VALUES_2 =
        Collections.unmodifiableMap(Collections.singletonMap("key2", AttributeValue.builder().s("2").build()));
    private static final Map<String, AttributeValue> ATTRIBUTE_VALUES_3 =
        Collections.unmodifiableMap(Collections.singletonMap("key3", AttributeValue.builder().s("3").build()));

    @Mock
    private MapperExtension mockExtension1;
    @Mock
    private MapperExtension mockExtension2;
    @Mock
    private MapperExtension mockExtension3;

    private final List<Map<String, AttributeValue>> fakeItems =
        IntStream.range(0, 4)
                 .mapToObj($ -> createUniqueFakeItem())
                 .map(fakeItem -> FakeItem.getTableSchema().itemToMap(fakeItem, true))
                 .collect(toList());

    @Test
    public void beforeWrite_multipleExtensions_multipleConditions_multipleTransformations() {
        Expression expression1 = Expression.builder().expression("one").expressionValues(ATTRIBUTE_VALUES_1).build();
        Expression expression2 = Expression.builder().expression("two").expressionValues(ATTRIBUTE_VALUES_2).build();
        Expression expression3 = Expression.builder().expression("three").expressionValues(ATTRIBUTE_VALUES_3).build();
        ChainMapperExtension extension = ChainMapperExtension.create(mockExtension1, mockExtension2, mockExtension3);
        WriteModification writeModification1 = WriteModification.builder()
                                                                .additionalConditionalExpression(expression1)
                                                                .transformedItem(fakeItems.get(1))
                                                                .build();
        WriteModification writeModification2 = WriteModification.builder()
                                                                .additionalConditionalExpression(expression2)
                                                                .transformedItem(fakeItems.get(2))
                                                                .build();
        WriteModification writeModification3 = WriteModification.builder()
                                                                .additionalConditionalExpression(expression3)
                                                                .transformedItem(fakeItems.get(3))
                                                                .build();
        when(mockExtension1.beforeWrite(anyMap(), any(), any())).thenReturn(writeModification1);
        when(mockExtension2.beforeWrite(anyMap(), any(), any())).thenReturn(writeModification2);
        when(mockExtension3.beforeWrite(anyMap(), any(), any())).thenReturn(writeModification3);

        WriteModification result = extension.beforeWrite(fakeItems.get(0), 
                                                         PRIMARY_CONTEXT,
                                                         FakeItem.getTableMetadata());

        Map<String, AttributeValue> combinedMap = new HashMap<>(ATTRIBUTE_VALUES_1);
        combinedMap.putAll(ATTRIBUTE_VALUES_2);
        combinedMap.putAll(ATTRIBUTE_VALUES_3);
        Expression expectedExpression =
            Expression.builder().expression("((one) AND (two)) AND (three)").expressionValues(combinedMap).build();
        assertThat(result.transformedItem(), is(fakeItems.get(3)));
        assertThat(result.additionalConditionalExpression(), is(expectedExpression));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension1).beforeWrite(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension2).beforeWrite(fakeItems.get(1), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension3).beforeWrite(fakeItems.get(2), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void beforeWrite_multipleExtensions_doingNothing() {
        ChainMapperExtension extension = ChainMapperExtension.create(mockExtension1, mockExtension2, mockExtension3);
        when(mockExtension1.beforeWrite(anyMap(), any(), any())).thenReturn(WriteModification.builder().build());
        when(mockExtension2.beforeWrite(anyMap(), any(), any())).thenReturn(WriteModification.builder().build());
        when(mockExtension3.beforeWrite(anyMap(), any(), any())).thenReturn(WriteModification.builder().build());

        WriteModification result = extension.beforeWrite(fakeItems.get(0), 
                                                         PRIMARY_CONTEXT,
                                                         FakeItem.getTableMetadata());

        assertThat(result.additionalConditionalExpression(), is(nullValue()));
        assertThat(result.transformedItem(), is(nullValue()));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension1).beforeWrite(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension2).beforeWrite(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension3).beforeWrite(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void beforeWrite_multipleExtensions_singleCondition_noTransformations() {
        Expression expression = Expression.builder().expression("one").expressionValues(ATTRIBUTE_VALUES_1).build();
        ChainMapperExtension extension = ChainMapperExtension.create(mockExtension1, mockExtension2, mockExtension3);
        WriteModification writeModification1 = WriteModification.builder().build();
        WriteModification writeModification2 = WriteModification.builder()
                                                                .additionalConditionalExpression(expression)
                                                                .build();
        WriteModification writeModification3 = WriteModification.builder().build();
        when(mockExtension1.beforeWrite(anyMap(), any(), any())).thenReturn(writeModification1);
        when(mockExtension2.beforeWrite(anyMap(), any(), any())).thenReturn(writeModification2);
        when(mockExtension3.beforeWrite(anyMap(), any(), any())).thenReturn(writeModification3);

        WriteModification result = extension.beforeWrite(fakeItems.get(0), 
                                                         PRIMARY_CONTEXT,
                                                         FakeItem.getTableMetadata());

        Expression expectedExpression = Expression.builder()
                                                  .expression("one")
                                                  .expressionValues(ATTRIBUTE_VALUES_1)
                                                  .build();
        assertThat(result.transformedItem(), is(nullValue()));
        assertThat(result.additionalConditionalExpression(), is(expectedExpression));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension1).beforeWrite(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension2).beforeWrite(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension3).beforeWrite(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void beforeWrite_multipleExtensions_noConditions_singleTransformation() {
        ChainMapperExtension extension = ChainMapperExtension.create(mockExtension1, mockExtension2, mockExtension3);
        WriteModification writeModification1 = WriteModification.builder().build();
        WriteModification writeModification2 = WriteModification.builder()
                                                                .transformedItem(fakeItems.get(1))
                                                                .build();
        WriteModification writeModification3 = WriteModification.builder().build();
        when(mockExtension1.beforeWrite(anyMap(), any(), any())).thenReturn(writeModification1);
        when(mockExtension2.beforeWrite(anyMap(), any(), any())).thenReturn(writeModification2);
        when(mockExtension3.beforeWrite(anyMap(), any(), any())).thenReturn(writeModification3);

        WriteModification result = extension.beforeWrite(fakeItems.get(0), 
                                                         PRIMARY_CONTEXT,
                                                         FakeItem.getTableMetadata());

        assertThat(result.transformedItem(), is(fakeItems.get(1)));
        assertThat(result.additionalConditionalExpression(), is(nullValue()));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension1).beforeWrite(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension2).beforeWrite(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension3).beforeWrite(fakeItems.get(1), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void beforeWrite_noExtensions() {
        ChainMapperExtension extension = ChainMapperExtension.create();

        WriteModification result = extension.beforeWrite(fakeItems.get(0),
                                                         PRIMARY_CONTEXT,
                                                         FakeItem.getTableMetadata());

        assertThat(result.transformedItem(), is(nullValue()));
        assertThat(result.additionalConditionalExpression(), is(nullValue()));
    }

    @Test
    public void afterRead_multipleExtensions_multipleTransformations() {
        ChainMapperExtension extension = ChainMapperExtension.create(mockExtension1, mockExtension2, mockExtension3);
        ReadModification readModification1 = ReadModification.builder().transformedItem(fakeItems.get(1)).build();
        ReadModification readModification2 = ReadModification.builder().transformedItem(fakeItems.get(2)).build();
        ReadModification readModification3 = ReadModification.builder().transformedItem(fakeItems.get(3)).build();
        when(mockExtension1.afterRead(anyMap(), any(), any())).thenReturn(readModification1);
        when(mockExtension2.afterRead(anyMap(), any(), any())).thenReturn(readModification2);
        when(mockExtension3.afterRead(anyMap(), any(), any())).thenReturn(readModification3);

        ReadModification result = extension.afterRead(fakeItems.get(0), PRIMARY_CONTEXT,  FakeItem.getTableMetadata());

        assertThat(result.transformedItem(), is(fakeItems.get(1)));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension3).afterRead(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension2).afterRead(fakeItems.get(3), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension1).afterRead(fakeItems.get(2), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void afterRead_multipleExtensions_singleTransformation() {
        ChainMapperExtension extension = ChainMapperExtension.create(mockExtension1, mockExtension2, mockExtension3);
        ReadModification readModification1 = ReadModification.builder().build();
        ReadModification readModification2 = ReadModification.builder().transformedItem(fakeItems.get(1)).build();
        ReadModification readModification3 = ReadModification.builder().build();
        when(mockExtension1.afterRead(anyMap(), any(), any())).thenReturn(readModification1);
        when(mockExtension2.afterRead(anyMap(), any(), any())).thenReturn(readModification2);
        when(mockExtension3.afterRead(anyMap(), any(), any())).thenReturn(readModification3);

        ReadModification result = extension.afterRead(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());

        assertThat(result.transformedItem(), is(fakeItems.get(1)));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension3).afterRead(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension2).afterRead(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension1).afterRead(fakeItems.get(1), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void afterRead_multipleExtensions_noTransformations() {
        ChainMapperExtension extension = ChainMapperExtension.create(mockExtension1, mockExtension2, mockExtension3);
        ReadModification readModification1 = ReadModification.builder().build();
        ReadModification readModification2 = ReadModification.builder().build();
        ReadModification readModification3 = ReadModification.builder().build();
        when(mockExtension1.afterRead(anyMap(), any(), any())).thenReturn(readModification1);
        when(mockExtension2.afterRead(anyMap(), any(), any())).thenReturn(readModification2);
        when(mockExtension3.afterRead(anyMap(), any(), any())).thenReturn(readModification3);

        ReadModification result = extension.afterRead(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());

        assertThat(result.transformedItem(), is(nullValue()));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension3).afterRead(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension2).afterRead(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verify(mockExtension1).afterRead(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void afterRead_noExtensions() {
        ChainMapperExtension extension = ChainMapperExtension.create();

        ReadModification result = extension.afterRead(fakeItems.get(0), PRIMARY_CONTEXT, FakeItem.getTableMetadata());

        assertThat(result.transformedItem(), is(nullValue()));
    }
}
