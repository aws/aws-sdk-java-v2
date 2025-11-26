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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithoutSort;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CompositeKeyFakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithNumericSort;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class QueryOperationConditionalTest {
    private static final String ID_KEY = "#AMZN_MAPPED_id";
    private static final String ID_VALUE = ":AMZN_MAPPED_id";
    private static final String SORT_KEY = "#AMZN_MAPPED_sort";
    private static final String SORT_VALUE = ":AMZN_MAPPED_sort";
    private static final String SORT_OTHER_VALUE = ":AMZN_MAPPED_sort2";

    private final FakeItem fakeItem = createUniqueFakeItem();
    private final AttributeValue fakeItemHashValue = AttributeValue.builder().s(fakeItem.getId()).build();
    private final FakeItemWithSort fakeItemWithSort = createUniqueFakeItemWithSort();
    private final AttributeValue fakeItemWithSortHashValue =
        AttributeValue.builder().s(fakeItemWithSort.getId()).build();
    private final AttributeValue fakeItemWithSortSortValue =
        AttributeValue.builder().s(fakeItemWithSort.getSort()).build();
    private final FakeItemWithSort fakeItemWithoutSort = createUniqueFakeItemWithoutSort();
    private final AttributeValue fakeItemWithoutSortHashValue =
        AttributeValue.builder().s(fakeItemWithoutSort.getId()).build();

    @Test
    public void equalTo_hashOnly() {
        Expression expression = QueryConditional.keyEqualTo(getKey(fakeItem)).expression(FakeItem.getTableSchema(),
                                                                                         TableMetadata.primaryIndexName());

        assertThat(expression.expression(), is(ID_KEY + " = " + ID_VALUE));
        assertThat(expression.expressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.expressionValues(), hasEntry(ID_VALUE, fakeItemHashValue));
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_hashOnly_notSet_throwsIllegalArgumentException() {
        fakeItem.setId(null);
        QueryConditional.keyEqualTo(getKey(fakeItem))
                        .expression(FakeItem.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test
    public void equalTo_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.keyEqualTo(getKey(fakeItemWithSort))
                                                .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());

        verifyExpression(expression, "=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_hashAndRangeKey_hashNotSet_throwsIllegalArgumentException() {
        fakeItemWithSort.setId(null);
        QueryConditional.keyEqualTo(getKey(fakeItemWithSort))
                        .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test
    public void equalTo_hashAndRangeKey_hashOnlySet() {
        Expression expression = QueryConditional.keyEqualTo(getKey(fakeItemWithoutSort))
                                                .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());

        assertThat(expression.expression(), is(ID_KEY + " = " + ID_VALUE));
        assertThat(expression.expressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.expressionValues(), hasEntry(ID_VALUE, fakeItemWithoutSortHashValue));
    }

    @Test
    public void greaterThan_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.sortGreaterThan(getKey(fakeItemWithSort))
                                                .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());

        verifyExpression(expression, ">");
    }

    @Test(expected = IllegalArgumentException.class)
    public void greaterThan_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.sortGreaterThan(getKey(fakeItem))
                        .expression(FakeItem.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void greaterThan_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.sortGreaterThan(getKey(fakeItemWithoutSort))
                        .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test
    public void greaterThanOrEqualTo_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.sortGreaterThanOrEqualTo(getKey(fakeItemWithSort))
                                                .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());

        verifyExpression(expression, ">=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void greaterThanOrEqualTo_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.sortGreaterThanOrEqualTo(getKey(fakeItem))
                        .expression(FakeItem.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void greaterThanOrEqualTo_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.sortGreaterThanOrEqualTo(getKey(fakeItemWithoutSort))
                        .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test
    public void lessThan_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.sortLessThan(getKey(fakeItemWithSort))
                                                .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());

        verifyExpression(expression, "<");
    }

    @Test(expected = IllegalArgumentException.class)
    public void lessThan_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.sortLessThan(getKey(fakeItem))
                        .expression(FakeItem.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void lessThan_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.sortLessThan(getKey(fakeItemWithoutSort))
                        .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test
    public void lessThanOrEqualTo_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.sortLessThanOrEqualTo(getKey(fakeItemWithSort))
                                                .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());

        verifyExpression(expression, "<=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void lessThanOrEqualTo_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.sortLessThanOrEqualTo(getKey(fakeItem))
                        .expression(FakeItem.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void lessThanOrEqualTo_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.sortLessThanOrEqualTo(getKey(fakeItemWithoutSort))
                        .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test
    public void beginsWith_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.sortBeginsWith(getKey(fakeItemWithSort))
                                                .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());

        String expectedExpression = String.format("%s = %s AND begins_with(%s, %s)", ID_KEY, ID_VALUE, SORT_KEY,
                                                  SORT_VALUE);
        assertThat(expression.expression(), is(expectedExpression));
        assertThat(expression.expressionValues(), hasEntry(ID_VALUE, fakeItemWithSortHashValue));
        assertThat(expression.expressionValues(), hasEntry(SORT_VALUE, fakeItemWithSortSortValue));
        assertThat(expression.expressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.expressionNames(), hasEntry(SORT_KEY, "sort"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginsWith_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.sortBeginsWith(getKey(fakeItem))
                        .expression(FakeItem.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginsWith_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.sortBeginsWith(getKey(fakeItemWithoutSort))
                        .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginsWith_numericRange_throwsIllegalArgumentException() {
        FakeItemWithNumericSort fakeItemWithNumericSort = FakeItemWithNumericSort.createUniqueFakeItemWithSort();
        QueryConditional.sortBeginsWith(getKey(fakeItemWithNumericSort)).expression(FakeItemWithNumericSort.getTableSchema(),
                                                                                    TableMetadata.primaryIndexName());
    }

    @Test
    public void between_allKeysSet_stringSort() {
        FakeItemWithSort otherFakeItemWithSort =
            FakeItemWithSort.builder().id(fakeItemWithSort.getId()).sort(UUID.randomUUID().toString()).build();
        AttributeValue otherFakeItemWithSortSortValue =
            AttributeValue.builder().s(otherFakeItemWithSort.getSort()).build();

        Expression expression = QueryConditional.sortBetween(getKey(fakeItemWithSort), getKey(otherFakeItemWithSort))
                                                .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());

        String expectedExpression = String.format("%s = %s AND %s BETWEEN %s AND %s", ID_KEY, ID_VALUE, SORT_KEY,
                                                  SORT_VALUE, SORT_OTHER_VALUE);
        assertThat(expression.expression(), is(expectedExpression));
        assertThat(expression.expressionValues(), hasEntry(ID_VALUE, fakeItemWithSortHashValue));
        assertThat(expression.expressionValues(), hasEntry(SORT_VALUE, fakeItemWithSortSortValue));
        assertThat(expression.expressionValues(), hasEntry(SORT_OTHER_VALUE, otherFakeItemWithSortSortValue));
        assertThat(expression.expressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.expressionNames(), hasEntry(SORT_KEY, "sort"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void between_hashOnly_throwsIllegalArgumentException() {
        FakeItem otherFakeItem = createUniqueFakeItem();
        QueryConditional.sortBetween(getKey(fakeItem), getKey(otherFakeItem))
                        .expression(FakeItem.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void between_hashAndSort_onlyFirstSortSet_throwsIllegalArgumentException() {
        QueryConditional.sortBetween(getKey(fakeItemWithSort), getKey(fakeItemWithoutSort))
                        .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void between_hashAndSort_onlySecondSortSet_throwsIllegalArgumentException() {
        QueryConditional.sortBetween(getKey(fakeItemWithoutSort), getKey(fakeItemWithSort))
                        .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());
    }

    private void verifyExpression(Expression expression, String condition) {
        assertThat(expression.expression(), is(ID_KEY + " = " + ID_VALUE + " AND " + SORT_KEY + " " + condition +
                                               " " + SORT_VALUE));
        assertThat(expression.expressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.expressionNames(), hasEntry(SORT_KEY, "sort"));
        assertThat(expression.expressionValues(), hasEntry(ID_VALUE, fakeItemWithSortHashValue));
        assertThat(expression.expressionValues(), hasEntry(SORT_VALUE, fakeItemWithSortSortValue));
    }

    private Key getKey(FakeItem item) {
        return EnhancedClientUtils.createKeyFromItem(item, FakeItem.getTableSchema(), TableMetadata.primaryIndexName());
    }

    private Key getKey(FakeItemWithSort item) {
        return EnhancedClientUtils.createKeyFromItem(item, FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());
    }

    private Key getKey(FakeItemWithNumericSort item) {
        return EnhancedClientUtils.createKeyFromItem(item,
                                                     FakeItemWithNumericSort.getTableSchema(),
                                                     TableMetadata.primaryIndexName());
    }

    @Test
    public void equalTo_gsiCompositePartitionKeys_allProvided() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
    }

    @Test
    public void equalTo_gsiCompositeKeys_partitionAndSort() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("sort2").build()))
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND "
                                               + "#AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2"));
    }

    @Test
    public void equalTo_gsiCompositeKeys_partialSortKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().s("sort1").build()))
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_gsiCompositeKeys_incompletePartitionKeys() {
        Key key = Key.builder()
                     .partitionValues(Collections.singletonList(
                         AttributeValue.builder().s("key1").build()))
                     .build();

        QueryConditional.keyEqualTo(key).expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_gsiCompositeKeys_tooManySortKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("sort2").build(),
                         AttributeValue.builder().s("sort3").build()))
                     .build();

        QueryConditional.keyEqualTo(key).expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test
    public void sortGreaterThan_gsiCompositeKeys_singleSortKey() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().s("sort1").build()))
                     .build();

        Expression expression = QueryConditional.sortGreaterThan(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 > :AMZN_MAPPED_gsiSort1"));
    }

    @Test
    public void sortGreaterThan_gsiCompositeKeys_multipleSortKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("sort2").build()))
                     .build();

        Expression expression = QueryConditional.sortGreaterThan(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND "
                                               + "#AMZN_MAPPED_gsiSort2 > :AMZN_MAPPED_gsiSort2"));
    }

    @Test
    public void sortLessThanOrEqualTo_gsiCompositeKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("sort2").build()))
                     .build();

        Expression expression = QueryConditional.sortLessThanOrEqualTo(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND "
                                               + "#AMZN_MAPPED_gsiSort2 <= :AMZN_MAPPED_gsiSort2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sortGreaterThan_gsiCompositeKeys_noSortKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .build();

        QueryConditional.sortGreaterThan(key).expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test
    public void sortBetween_gsiCompositeKeys_singleSortKey() {
        Key key1 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("key1").build(),
                          AttributeValue.builder().s("key2").build()))
                      .sortValues(Collections.singletonList(
                          AttributeValue.builder().s("sortA").build()))
                      .build();

        Key key2 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("key1").build(),
                          AttributeValue.builder().s("key2").build()))
                      .sortValues(Collections.singletonList(
                          AttributeValue.builder().s("sortZ").build()))
                      .build();

        Expression expression = QueryConditional.sortBetween(key1, key2)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 BETWEEN :AMZN_MAPPED_gsiSort1 "
                                               + "AND :AMZN_MAPPED_gsiSort12"));
    }

    @Test
    public void sortBetween_gsiCompositeKeys_multipleSortKeys() {
        Key key1 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("key1").build(),
                          AttributeValue.builder().s("key2").build()))
                      .sortValues(Arrays.asList(
                          AttributeValue.builder().s("sort1").build(),
                          AttributeValue.builder().s("sortA").build()))
                      .build();

        Key key2 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("key1").build(),
                          AttributeValue.builder().s("key2").build()))
                      .sortValues(Arrays.asList(
                          AttributeValue.builder().s("sort1").build(),
                          AttributeValue.builder().s("sortZ").build()))
                      .build();

        Expression expression = QueryConditional.sortBetween(key1, key2)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND "
                                               + "#AMZN_MAPPED_gsiSort2 BETWEEN :AMZN_MAPPED_gsiSort2 AND "
                                               + ":AMZN_MAPPED_gsiSort22"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sortBetween_gsiCompositeKeys_noSortKeys() {
        Key key1 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("key1").build(),
                          AttributeValue.builder().s("key2").build()))
                      .build();

        Key key2 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("key1").build(),
                          AttributeValue.builder().s("key2").build()))
                      .build();

        QueryConditional.sortBetween(key1, key2).expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test
    public void sortBeginsWith_gsiCompositeKeys_singleSortKey() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().s("prefix").build()))
                     .build();

        Expression expression = QueryConditional.sortBeginsWith(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND begins_with(#AMZN_MAPPED_gsiSort1, "
                                               + ":AMZN_MAPPED_gsiSort1)"));
    }

    @Test
    public void sortBeginsWith_gsiCompositeKeys_multipleSortKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("prefix").build()))
                     .build();

        Expression expression = QueryConditional.sortBeginsWith(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND "
                                               + "begins_with(#AMZN_MAPPED_gsiSort2, :AMZN_MAPPED_gsiSort2)"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sortBeginsWith_gsiCompositeKeys_noSortKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .build();

        QueryConditional.sortBeginsWith(key).expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test
    public void equalTo_backwardCompatibility_singleKey() {
        Expression expression = QueryConditional.keyEqualTo(getKey(fakeItem))
                                                .expression(FakeItem.getTableSchema(), TableMetadata.primaryIndexName());

        assertThat(expression.expression(), is(ID_KEY + " = " + ID_VALUE));
        assertThat(expression.expressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.expressionValues(), hasEntry(ID_VALUE, fakeItemHashValue));
    }

    @Test
    public void sortGreaterThan_backwardCompatibility_singleKey() {
        Expression expression = QueryConditional.sortGreaterThan(getKey(fakeItemWithSort))
                                                .expression(FakeItemWithSort.getTableSchema(), TableMetadata.primaryIndexName());

        verifyExpression(expression, ">");
    }

    @Test
    public void keyEqualTo_consumerBuilder_gsiCompositeKeys() {
        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build()))
                                                    .sortValues(Collections.singletonList(
                                                        AttributeValue.builder().s("sort1").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1"));
    }

    @Test
    public void sortBetween_consumerBuilder_gsiCompositeKeys() {
        Expression expression = QueryConditional.sortBetween(
                                                    k1 -> k1.partitionValues(Arrays.asList(
                                                                AttributeValue.builder().s("key1").build(),
                                                                AttributeValue.builder().s("key2").build()))
                                                            .sortValues(Collections.singletonList(
                                                                AttributeValue.builder().s("sortA").build())),
                                                    k2 -> k2.partitionValues(Arrays.asList(
                                                                AttributeValue.builder().s("key1").build(),
                                                                AttributeValue.builder().s("key2").build()))
                                                            .sortValues(Collections.singletonList(
                                                                AttributeValue.builder().s("sortZ").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 BETWEEN :AMZN_MAPPED_gsiSort1 "
                                               + "AND :AMZN_MAPPED_gsiSort12"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_gsiCompositeKeys_nullPartitionValue() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().nul(true).build()))
                     .build();

        QueryConditional.keyEqualTo(key).expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sortBeginsWith_gsiCompositeKeys_nullSortValue() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().nul(true).build()))
                     .build();

        QueryConditional.sortBeginsWith(key).expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sortBeginsWith_gsiCompositeKeys_numericSortValue() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().n("123").build()))
                     .build();

        QueryConditional.sortBeginsWith(key).expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test
    public void equalTo_gsiCompositeKeys_maxPartitionKeys() {
        TableSchema<CompositeKeyFakeItem> maxKeysSchema =
            StaticTableSchema.builder(CompositeKeyFakeItem.class)
                             .newItemSupplier(CompositeKeyFakeItem::new)
                             .addAttribute(String.class, a -> a.name("id")
                                                               .getter(CompositeKeyFakeItem::getId)
                                                               .setter(CompositeKeyFakeItem::setId)
                                                               .tags(StaticAttributeTags.primaryPartitionKey()))
                             .addAttribute(String.class, a -> a.name("gsiKey1")
                                                               .getter(CompositeKeyFakeItem::getGsiKey1)
                                                               .setter(CompositeKeyFakeItem::setGsiKey1)
                                                               .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.FIRST)))
                             .addAttribute(String.class, a -> a.name("gsiKey2")
                                                               .getter(CompositeKeyFakeItem::getGsiKey2)
                                                               .setter(CompositeKeyFakeItem::setGsiKey2)
                                                               .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.SECOND)))
                             .addAttribute(String.class, a -> a.name("gsiSort1")
                                                               .getter(CompositeKeyFakeItem::getGsiSort1)
                                                               .setter(CompositeKeyFakeItem::setGsiSort1)
                                                               .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.THIRD)))
                             .addAttribute(String.class, a -> a.name("gsiSort2")
                                                               .getter(CompositeKeyFakeItem::getGsiSort2)
                                                               .setter(CompositeKeyFakeItem::setGsiSort2)
                                                               .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.FOURTH)))
                             .build();

        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build(),
                         AttributeValue.builder().s("key3").build(),
                         AttributeValue.builder().s("key4").build()))
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(maxKeysSchema, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND "
                                               + "#AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2"));
    }

    @Test
    public void equalTo_gsiCompositeKeys_addPartitionValuesFromStrings() {
        Key key = Key.builder()
                     .addPartitionValue("key1")
                     .addPartitionValue("key2")
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2"));
    }

    @Test
    public void sortGreaterThan_gsiCompositeKeys_addPartitionValuesFromStrings() {
        Key key = Key.builder()
                     .addPartitionValue("key1")
                     .addPartitionValue("key2")
                     .addSortValue("sort1")
                     .addSortValue("sort2")
                     .build();

        Expression expression = QueryConditional.sortGreaterThan(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND "
                                               + "#AMZN_MAPPED_gsiSort2 > :AMZN_MAPPED_gsiSort2"));
    }

    @Test
    public void sortBetween_gsiCompositeKeys_addPartitionValuesFromStrings() {
        Key key1 = Key.builder()
                      .addPartitionValue("key1")
                      .addPartitionValue("key2")
                      .addSortValue("sortA")
                      .build();

        Key key2 = Key.builder()
                      .addPartitionValue("key1")
                      .addPartitionValue("key2")
                      .addSortValue("sortZ")
                      .build();

        Expression expression = QueryConditional.sortBetween(key1, key2)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 BETWEEN :AMZN_MAPPED_gsiSort1 "
                                               + "AND :AMZN_MAPPED_gsiSort12"));
    }

    @Test
    public void equalTo_gsiCompositeKeys_addPartitionValuesFromNumbers() {
        Key key = Key.builder()
                     .addPartitionValue(123)
                     .addPartitionValue(456)
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2"));
    }

    @Test
    public void sortLessThan_gsiCompositeKeys_addPartitionValuesFromNumbers() {
        Key key = Key.builder()
                     .addPartitionValue(123)
                     .addPartitionValue(456)
                     .addSortValue(789)
                     .addSortValue(101112)
                     .build();

        Expression expression = QueryConditional.sortLessThan(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND "
                                               + "#AMZN_MAPPED_gsiSort2 < :AMZN_MAPPED_gsiSort2"));
    }

    @Test
    public void equalTo_gsiCompositeKeys_addPartitionValuesFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");

        Key key = Key.builder()
                     .addPartitionValue(bytes1)
                     .addPartitionValue(bytes2)
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2"));
    }

    @Test
    public void sortBeginsWith_gsiCompositeKeys_addPartitionValuesFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");
        SdkBytes sortBytes = SdkBytes.fromUtf8String("prefix");

        Key key = Key.builder()
                     .addPartitionValue(bytes1)
                     .addPartitionValue(bytes2)
                     .addSortValue(sortBytes)
                     .build();

        Expression expression = QueryConditional.sortBeginsWith(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND begins_with(#AMZN_MAPPED_gsiSort1, "
                                               + ":AMZN_MAPPED_gsiSort1)"));
    }

    @Test
    public void equalTo_gsiCompositeKeys_mixedTypes() {
        Key key = Key.builder()
                     .addPartitionValue("key1")
                     .addPartitionValue("key2")
                     .addSortValue(123)
                     .addSortValue(456)
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND "
                                               + "#AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2"));
    }

    @Test
    public void keyEqualTo_consumerBuilder_addPartitionValuesFromStrings() {
        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .addPartitionValue("key1")
                                                    .addPartitionValue("key2")
                                                    .addSortValue("sort1"))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1"));
    }

    @Test
    public void sortBetween_consumerBuilder_addPartitionValuesFromNumbers() {
        Expression expression = QueryConditional.sortBetween(
                                                    k1 -> k1.addPartitionValue(123)
                                                            .addPartitionValue(456)
                                                            .addSortValue(100),
                                                    k2 -> k2.addPartitionValue(123)
                                                            .addPartitionValue(456)
                                                            .addSortValue(200))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        assertThat(expression.expression(), is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = "
                                               + ":AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 BETWEEN :AMZN_MAPPED_gsiSort1 "
                                               + "AND :AMZN_MAPPED_gsiSort12"));
    }
}