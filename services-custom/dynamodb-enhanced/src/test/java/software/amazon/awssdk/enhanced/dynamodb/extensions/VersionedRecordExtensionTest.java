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

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.OperationContext;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class VersionedRecordExtensionTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        OperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private final VersionedRecordExtension versionedRecordExtension = VersionedRecordExtension.builder().build();

    @Test
    public void beforeRead_doesNotTransformObject() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);

        ReadModification result =
            versionedRecordExtension.afterRead(fakeItemMap, PRIMARY_CONTEXT, FakeItem.getTableMetadata());

        assertThat(result, is(ReadModification.builder().build()));
    }

    @Test
    public void beforeWrite_initialVersion_expressionIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();

        WriteModification result =
            versionedRecordExtension.beforeWrite(FakeItem.getTableSchema().itemToMap(fakeItem, true),
                                                 PRIMARY_CONTEXT,
                                                 FakeItem.getTableMetadata());

        assertThat(result.additionalConditionalExpression(),
                   is(Expression.builder().expression("attribute_not_exists(version)").build()));
    }

    @Test
    public void beforeWrite_initialVersion_transformedItemIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemWithInitialVersion =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        fakeItemWithInitialVersion.put("version", AttributeValue.builder().n("1").build());

        WriteModification result =
            versionedRecordExtension.beforeWrite(FakeItem.getTableSchema().itemToMap(fakeItem, true),
                                                 PRIMARY_CONTEXT,
                                                 FakeItem.getTableMetadata());


        assertThat(result.transformedItem(), is(fakeItemWithInitialVersion));
    }

    @Test
    public void beforeWrite_initialVersionDueToExplicitNull_transformedItemIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> inputMap =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        inputMap.put("version", AttributeValue.builder().nul(true).build());
        Map<String, AttributeValue> fakeItemWithInitialVersion =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        fakeItemWithInitialVersion.put("version", AttributeValue.builder().n("1").build());

        WriteModification result =
            versionedRecordExtension.beforeWrite(inputMap, PRIMARY_CONTEXT, FakeItem.getTableMetadata());

        assertThat(result.transformedItem(), is(fakeItemWithInitialVersion));
    }

    @Test
    public void beforeWrite_existingVersion_expressionIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();
        fakeItem.setVersion(13);

        WriteModification result =
            versionedRecordExtension.beforeWrite(FakeItem.getTableSchema().itemToMap(fakeItem, true),
                                                 PRIMARY_CONTEXT,
                                                 FakeItem.getTableMetadata());

        assertThat(result.additionalConditionalExpression(),
                   is(Expression.builder()
                                .expression("version = :old_version_value")
                                .expressionValues(singletonMap(":old_version_value",
                                                               AttributeValue.builder().n("13").build()))
                                .build()));
    }

    @Test
    public void beforeWrite_existingVersion_transformedItemIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();
        fakeItem.setVersion(13);
        Map<String, AttributeValue> fakeItemWithInitialVersion =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        fakeItemWithInitialVersion.put("version", AttributeValue.builder().n("14").build());

        WriteModification result =
            versionedRecordExtension.beforeWrite(FakeItem.getTableSchema().itemToMap(fakeItem, true),
                                                 PRIMARY_CONTEXT,
                                                 FakeItem.getTableMetadata());


        assertThat(result.transformedItem(), is(fakeItemWithInitialVersion));
    }

    @Test
    public void beforeWrite_returnsNoOpModification_ifVersionAttributeNotDefined() {
        FakeItemWithSort fakeItemWithSort = createUniqueFakeItemWithSort();
        Map<String, AttributeValue> itemMap =
            new HashMap<>(FakeItemWithSort.getTableSchema().itemToMap(fakeItemWithSort, true));

        WriteModification writeModification = versionedRecordExtension.beforeWrite(itemMap,
                                                                                   PRIMARY_CONTEXT,
                                                                                   FakeItemWithSort.getTableSchema().tableMetadata());

        assertThat(writeModification, is(WriteModification.builder().build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void beforeWrite_throwsIllegalArgumentException_ifVersionAttributeIsWrongType() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemWIthBadVersion =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        fakeItemWIthBadVersion.put("version", AttributeValue.builder().s("14").build());

        versionedRecordExtension.beforeWrite(fakeItemWIthBadVersion, PRIMARY_CONTEXT, FakeItem.getTableMetadata());
    }
}
