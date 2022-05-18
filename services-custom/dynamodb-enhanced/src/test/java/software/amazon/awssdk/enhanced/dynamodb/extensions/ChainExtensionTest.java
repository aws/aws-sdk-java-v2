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

package software.amazon.awssdk.enhanced.dynamodb.extensions;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;

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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.ChainExtension;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.OperationName;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class ChainExtensionTest {

    private static final String TABLE_NAME = "concrete-table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private static final Map<String, AttributeValue> ATTRIBUTE_VALUES_1 =
        Collections.unmodifiableMap(Collections.singletonMap("key1", AttributeValue.builder().s("1").build()));
    private static final Map<String, AttributeValue> ATTRIBUTE_VALUES_2 =
        Collections.unmodifiableMap(Collections.singletonMap("key2", AttributeValue.builder().s("2").build()));
    private static final Map<String, AttributeValue> ATTRIBUTE_VALUES_3 =
        Collections.unmodifiableMap(Collections.singletonMap("key3", AttributeValue.builder().s("3").build()));

    @Mock
    private DynamoDbEnhancedClientExtension mockExtension1;
    @Mock
    private DynamoDbEnhancedClientExtension mockExtension2;
    @Mock
    private DynamoDbEnhancedClientExtension mockExtension3;

    private final List<Map<String, AttributeValue>> fakeItems =
        IntStream.range(0, 4)
                 .mapToObj($ -> createUniqueFakeItem())
                 .map(fakeItem -> FakeItem.getTableSchema().itemToMap(fakeItem, true))
                 .collect(toList());

    @Test
    public void beforeWrite_multipleExtensions_multipleModifications() {
        Expression expression1 = Expression.builder().expression("one").expressionValues(ATTRIBUTE_VALUES_1).build();
        Expression expression2 = Expression.builder().expression("two").expressionValues(ATTRIBUTE_VALUES_2).build();
        Expression expression3 = Expression.builder().expression("three").expressionValues(ATTRIBUTE_VALUES_3).build();
        UpdateExpression updateExpression1 = updateExpression(removeAction("attr1"));
        UpdateExpression updateExpression2 = updateExpression(removeAction("attr2"));
        UpdateExpression updateExpression3 = updateExpression(removeAction("attr3"));

        ChainExtension extension = ChainExtension.create(mockExtension1, mockExtension2, mockExtension3);
        WriteModification writeModification1 = WriteModification.builder()
                                                                .additionalConditionalExpression(expression1)
                                                                .updateExpression(updateExpression1)
                                                                .transformedItem(fakeItems.get(1))
                                                                .build();
        WriteModification writeModification2 = WriteModification.builder()
                                                                .additionalConditionalExpression(expression2)
                                                                .updateExpression(updateExpression2)
                                                                .transformedItem(fakeItems.get(2))
                                                                .build();
        WriteModification writeModification3 = WriteModification.builder()
                                                                .additionalConditionalExpression(expression3)
                                                                .updateExpression(updateExpression3)
                                                                .transformedItem(fakeItems.get(3))
                                                                .build();
        when(mockExtension1.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(writeModification1);
        when(mockExtension2.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(writeModification2);
        when(mockExtension3.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(writeModification3);

        WriteModification result = extension.beforeWrite(getWriteExtensionContext(0));

        Map<String, AttributeValue> combinedMap = new HashMap<>(ATTRIBUTE_VALUES_1);
        combinedMap.putAll(ATTRIBUTE_VALUES_2);
        combinedMap.putAll(ATTRIBUTE_VALUES_3);
        Expression expectedConditionalExpression =
            Expression.builder().expression("((one) AND (two)) AND (three)").expressionValues(combinedMap).build();
        UpdateExpression expectedUpdateExpression = updateExpression(removeAction("attr1"),
                                                                     removeAction("attr2"),
                                                                     removeAction("attr3"));
        assertThat(result.transformedItem(), is(fakeItems.get(3)));
        assertThat(result.additionalConditionalExpression(), is(expectedConditionalExpression));
        assertThat(result.updateExpression(), is(expectedUpdateExpression));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension1).beforeWrite(getWriteExtensionContext(0));
        inOrder.verify(mockExtension2).beforeWrite(getWriteExtensionContext(1));
        inOrder.verify(mockExtension3).beforeWrite(getWriteExtensionContext(2));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void beforeWrite_multipleExtensions_doingNothing() {
        ChainExtension extension = ChainExtension.create(mockExtension1, mockExtension2, mockExtension3);
        when(mockExtension1.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(WriteModification.builder().build());
        when(mockExtension2.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(WriteModification.builder().build());
        when(mockExtension3.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(WriteModification.builder().build());

        WriteModification result = extension.beforeWrite(getWriteExtensionContext(0));

        assertThat(result.additionalConditionalExpression(), is(nullValue()));
        assertThat(result.updateExpression(), is(nullValue()));
        assertThat(result.transformedItem(), is(nullValue()));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension1).beforeWrite(getWriteExtensionContext(0));
        inOrder.verify(mockExtension2).beforeWrite(getWriteExtensionContext(0));
        inOrder.verify(mockExtension3).beforeWrite(getWriteExtensionContext(0));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void beforeWrite_multipleExtensions_singleCondition_noTransformations() {
        Expression conditionalExpression = Expression.builder().expression("one").expressionValues(ATTRIBUTE_VALUES_1).build();
        ChainExtension extension = ChainExtension.create(mockExtension1, mockExtension2, mockExtension3);
        WriteModification writeModification1 = WriteModification.builder().build();
        WriteModification writeModification2 = WriteModification.builder()
                                                                .additionalConditionalExpression(conditionalExpression)
                                                                .build();
        WriteModification writeModification3 = WriteModification.builder().build();
        when(mockExtension1.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(writeModification1);
        when(mockExtension2.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(writeModification2);
        when(mockExtension3.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(writeModification3);

        WriteModification result = extension.beforeWrite(getWriteExtensionContext(0));

        Expression expectedConditionalExpression = Expression.builder()
                                                  .expression("one")
                                                  .expressionValues(ATTRIBUTE_VALUES_1)
                                                  .build();
        assertThat(result.transformedItem(), is(nullValue()));
        assertThat(result.additionalConditionalExpression(), is(expectedConditionalExpression));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension1).beforeWrite(getWriteExtensionContext(0));
        inOrder.verify(mockExtension2).beforeWrite(getWriteExtensionContext(0));
        inOrder.verify(mockExtension3).beforeWrite(getWriteExtensionContext(0));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void beforeWrite_multipleExtensions_noConditions_singleTransformation() {
        ChainExtension extension = ChainExtension.create(mockExtension1, mockExtension2, mockExtension3);
        WriteModification writeModification1 = WriteModification.builder().build();
        WriteModification writeModification2 = WriteModification.builder()
                                                                .transformedItem(fakeItems.get(1))
                                                                .build();
        WriteModification writeModification3 = WriteModification.builder().build();
        when(mockExtension1.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(writeModification1);
        when(mockExtension2.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(writeModification2);
        when(mockExtension3.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(writeModification3);

        WriteModification result = extension.beforeWrite(getWriteExtensionContext(0));

        assertThat(result.transformedItem(), is(fakeItems.get(1)));
        assertThat(result.additionalConditionalExpression(), is(nullValue()));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension1).beforeWrite(getWriteExtensionContext(0));
        inOrder.verify(mockExtension2).beforeWrite(getWriteExtensionContext(0));
        inOrder.verify(mockExtension3).beforeWrite(getWriteExtensionContext(1));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void beforeWrite_noExtensions() {
        ChainExtension extension = ChainExtension.create();

        WriteModification result = extension.beforeWrite(getWriteExtensionContext(0));

        assertThat(result.transformedItem(), is(nullValue()));
        assertThat(result.additionalConditionalExpression(), is(nullValue()));
    }

    @Test
    public void afterRead_multipleExtensions_multipleTransformations() {
        ChainExtension extension = ChainExtension.create(mockExtension1, mockExtension2, mockExtension3);
        ReadModification readModification1 = ReadModification.builder().transformedItem(fakeItems.get(1)).build();
        ReadModification readModification2 = ReadModification.builder().transformedItem(fakeItems.get(2)).build();
        ReadModification readModification3 = ReadModification.builder().transformedItem(fakeItems.get(3)).build();
        when(mockExtension1.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(readModification1);
        when(mockExtension2.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(readModification2);
        when(mockExtension3.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(readModification3);

        ReadModification result = extension.afterRead(getReadExtensionContext(0));

        assertThat(result.transformedItem(), is(fakeItems.get(1)));

        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension3).afterRead(getReadExtensionContext(0));
        inOrder.verify(mockExtension2).afterRead(getReadExtensionContext(3));
        inOrder.verify(mockExtension1).afterRead(getReadExtensionContext(2));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void afterRead_multipleExtensions_singleTransformation() {
        ChainExtension extension = ChainExtension.create(mockExtension1, mockExtension2, mockExtension3);
        ReadModification readModification1 = ReadModification.builder().build();
        ReadModification readModification2 = ReadModification.builder().transformedItem(fakeItems.get(1)).build();
        ReadModification readModification3 = ReadModification.builder().build();
        when(mockExtension1.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(readModification1);
        when(mockExtension2.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(readModification2);
        when(mockExtension3.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(readModification3);

        ReadModification result = extension.afterRead(getReadExtensionContext(0));

        assertThat(result.transformedItem(), is(fakeItems.get(1)));
        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension3).afterRead(getReadExtensionContext(0));
        inOrder.verify(mockExtension2).afterRead(getReadExtensionContext(0));
        inOrder.verify(mockExtension1).afterRead(getReadExtensionContext(1));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void afterRead_multipleExtensions_noTransformations() {
        ChainExtension extension = ChainExtension.create(mockExtension1, mockExtension2, mockExtension3);
        ReadModification readModification1 = ReadModification.builder().build();
        ReadModification readModification2 = ReadModification.builder().build();
        ReadModification readModification3 = ReadModification.builder().build();
        when(mockExtension1.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(readModification1);
        when(mockExtension2.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(readModification2);
        when(mockExtension3.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(readModification3);

        ReadModification result = extension.afterRead(getReadExtensionContext(0));

        assertThat(result.transformedItem(), is(nullValue()));
        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2, mockExtension3);
        inOrder.verify(mockExtension3).afterRead(getReadExtensionContext(0));
        inOrder.verify(mockExtension2).afterRead(getReadExtensionContext(0));
        inOrder.verify(mockExtension1).afterRead(getReadExtensionContext(0));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void afterRead_noExtensions() {
        ChainExtension extension = ChainExtension.create();

        ReadModification result = extension.afterRead(getReadExtensionContext(0));

        assertThat(result.transformedItem(), is(nullValue()));
    }

    private DefaultDynamoDbExtensionContext getWriteExtensionContext(int i) {
        return getExtensionContext(i, OperationName.BATCH_WRITE_ITEM);
    }

    private DefaultDynamoDbExtensionContext getReadExtensionContext(int i) {
        return getExtensionContext(i, null);
    }

    private DefaultDynamoDbExtensionContext getExtensionContext(int i, OperationName operationName) {
        DefaultDynamoDbExtensionContext.Builder context =
            DefaultDynamoDbExtensionContext.builder()
                                           .tableMetadata(FakeItem.getTableMetadata())
                                           .tableSchema(FakeItem.getTableSchema())
                                           .operationContext(PRIMARY_CONTEXT)
                                           .items(fakeItems.get(i));
        if (operationName != null) {
            context.operationName(OperationName.BATCH_WRITE_ITEM);
        }
        return context.build();
    }

    private static RemoveAction removeAction(String attributeName) {
        return RemoveAction.builder()
                           .path(keyRef(attributeName))
                           .putExpressionName(keyRef(attributeName), attributeName)
                           .build();
    }

    private static SetAction setAction(String attributeName, AttributeValue value) {
        return SetAction.builder()
                        .value(valueRef(attributeName))
                        .putExpressionValue(valueRef(attributeName), value)
                        .path(keyRef(attributeName))
                        .putExpressionName(keyRef(attributeName), attributeName)
                        .build();
    }

    private UpdateExpression updateExpression(UpdateAction... actions) {
        return UpdateExpression.builder().actions(actions).build();
    }

    private AttributeValue string(String s) {
        return AttributeValue.builder().s(s).build();
    }

    private static String keyRef(String key) {
        return "#AMZN_" + key;
    }

    private static String valueRef(String value) {
        return ":AMZN_" + value;
    }
}
