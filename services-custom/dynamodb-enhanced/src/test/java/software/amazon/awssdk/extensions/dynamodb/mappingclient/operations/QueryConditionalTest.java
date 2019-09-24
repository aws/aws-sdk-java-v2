/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithoutSort;

import java.util.UUID;

import org.junit.Test;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithNumericSort;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort;

public class QueryConditionalTest {
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
        Expression expression = QueryConditional.equalTo(getKey(fakeItem)).getExpression(FakeItem.getTableSchema(),
                                                                                         TableMetadata.getPrimaryIndexName());

        assertThat(expression.getExpression(), is(ID_KEY + " = " + ID_VALUE));
        assertThat(expression.getExpressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.getExpressionValues(), hasEntry(ID_VALUE, fakeItemHashValue));
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_hashOnly_notSet_throwsIllegalArgumentException() {
        fakeItem.setId(null);
        QueryConditional.equalTo(getKey(fakeItem))
                        .getExpression(FakeItem.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test
    public void equalTo_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.equalTo(getKey(fakeItemWithSort))
                                                .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());

        verifyExpression(expression, "=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_hashAndRangeKey_hashNotSet_throwsIllegalArgumentException() {
        fakeItemWithSort.setId(null);
        QueryConditional.equalTo(getKey(fakeItemWithSort))
                        .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test
    public void equalTo_hashAndRangeKey_hashOnlySet() {
        Expression expression = QueryConditional.equalTo(getKey(fakeItemWithoutSort))
                                                .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());

        assertThat(expression.getExpression(), is(ID_KEY + " = " + ID_VALUE));
        assertThat(expression.getExpressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.getExpressionValues(), hasEntry(ID_VALUE, fakeItemWithoutSortHashValue));
    }

    @Test
    public void greaterThan_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.greaterThan(getKey(fakeItemWithSort))
                                                .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());

        verifyExpression(expression, ">");
    }

    @Test(expected = IllegalArgumentException.class)
    public void greaterThan_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.greaterThan(getKey(fakeItem))
                        .getExpression(FakeItem.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void greaterThan_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.greaterThan(getKey(fakeItemWithoutSort))
                        .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test
    public void greaterThanOrEqualTo_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.greaterThanOrEqualTo(getKey(fakeItemWithSort))
                                                .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());

        verifyExpression(expression, ">=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void greaterThanOrEqualTo_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.greaterThanOrEqualTo(getKey(fakeItem))
                        .getExpression(FakeItem.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void greaterThanOrEqualTo_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.greaterThanOrEqualTo(getKey(fakeItemWithoutSort))
                        .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test
    public void lessThan_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.lessThan(getKey(fakeItemWithSort))
                                                .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());

        verifyExpression(expression, "<");
    }

    @Test(expected = IllegalArgumentException.class)
    public void lessThan_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.lessThan(getKey(fakeItem))
                        .getExpression(FakeItem.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void lessThan_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.lessThan(getKey(fakeItemWithoutSort))
                        .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test
    public void lessThanOrEqualTo_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.lessThanOrEqualTo(getKey(fakeItemWithSort))
                                                .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());

        verifyExpression(expression, "<=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void lessThanOrEqualTo_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.lessThanOrEqualTo(getKey(fakeItem))
                        .getExpression(FakeItem.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void lessThanOrEqualTo_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.lessThanOrEqualTo(getKey(fakeItemWithoutSort))
                        .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test
    public void beginsWith_hashAndRangeKey_bothSet() {
        Expression expression = QueryConditional.beginsWith(getKey(fakeItemWithSort))
                                                .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());

        String expectedExpression = String.format("%s = %s AND begins_with ( %s, %s )", ID_KEY, ID_VALUE, SORT_KEY,
                                                  SORT_VALUE);
        assertThat(expression.getExpression(), is(expectedExpression));
        assertThat(expression.getExpressionValues(), hasEntry(ID_VALUE, fakeItemWithSortHashValue));
        assertThat(expression.getExpressionValues(), hasEntry(SORT_VALUE, fakeItemWithSortSortValue));
        assertThat(expression.getExpressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.getExpressionNames(), hasEntry(SORT_KEY, "sort"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginsWith_hashOnly_throwsIllegalArgumentException() {
        QueryConditional.beginsWith(getKey(fakeItem))
                        .getExpression(FakeItem.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginsWith_hashAndSort_onlyHashSet_throwsIllegalArgumentException() {
        QueryConditional.beginsWith(getKey(fakeItemWithoutSort))
                        .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginsWith_numericRange_throwsIllegalArgumentException() {
        FakeItemWithNumericSort fakeItemWithNumericSort = FakeItemWithNumericSort.createUniqueFakeItemWithSort();
        QueryConditional.beginsWith(getKey(fakeItemWithNumericSort)).getExpression(FakeItemWithNumericSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test
    public void between_allKeysSet_stringSort() {
        FakeItemWithSort otherFakeItemWithSort =
            FakeItemWithSort.builder().id(fakeItemWithSort.getId()).sort(UUID.randomUUID().toString()).build();
        AttributeValue otherFakeItemWithSortSortValue =
            AttributeValue.builder().s(otherFakeItemWithSort.getSort()).build();

        Expression expression = QueryConditional.between(getKey(fakeItemWithSort), getKey(otherFakeItemWithSort))
            .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());

        String expectedExpression = String.format("%s = %s AND %s BETWEEN %s AND %s", ID_KEY, ID_VALUE, SORT_KEY,
                                                  SORT_VALUE, SORT_OTHER_VALUE);
        assertThat(expression.getExpression(), is(expectedExpression));
        assertThat(expression.getExpressionValues(), hasEntry(ID_VALUE, fakeItemWithSortHashValue));
        assertThat(expression.getExpressionValues(), hasEntry(SORT_VALUE, fakeItemWithSortSortValue));
        assertThat(expression.getExpressionValues(), hasEntry(SORT_OTHER_VALUE, otherFakeItemWithSortSortValue));
        assertThat(expression.getExpressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.getExpressionNames(), hasEntry(SORT_KEY, "sort"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void between_hashOnly_throwsIllegalArgumentException() {
        FakeItem otherFakeItem = createUniqueFakeItem();
        QueryConditional.between(getKey(fakeItem), getKey(otherFakeItem))
                        .getExpression(FakeItem.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void between_hashAndSort_onlyFirstSortSet_throwsIllegalArgumentException() {
        QueryConditional.between(getKey(fakeItemWithSort), getKey(fakeItemWithoutSort))
                        .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void between_hashAndSort_onlySecondSortSet_throwsIllegalArgumentException() {
        QueryConditional.between(getKey(fakeItemWithoutSort), getKey(fakeItemWithSort))
                        .getExpression(FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    private void verifyExpression(Expression expression, String condition) {
        assertThat(expression.getExpression(), is(ID_KEY + " = " + ID_VALUE + " AND " + SORT_KEY + " " + condition +
                                                  " " + SORT_VALUE));
        assertThat(expression.getExpressionNames(), hasEntry(ID_KEY, "id"));
        assertThat(expression.getExpressionNames(), hasEntry(SORT_KEY, "sort"));
        assertThat(expression.getExpressionValues(), hasEntry(ID_VALUE, fakeItemWithSortHashValue));
        assertThat(expression.getExpressionValues(), hasEntry(SORT_VALUE, fakeItemWithSortSortValue));
    }
    
    private Key getKey(FakeItem item) {
        return Utils.createKeyFromItem(item, FakeItem.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    private Key getKey(FakeItemWithSort item) {
        return Utils.createKeyFromItem(item, FakeItemWithSort.getTableSchema(), TableMetadata.getPrimaryIndexName());
    }

    private Key getKey(FakeItemWithNumericSort item) {
        return Utils.createKeyFromItem(item,
                                       FakeItemWithNumericSort.getTableSchema(),
                                       TableMetadata.getPrimaryIndexName());
    }
}