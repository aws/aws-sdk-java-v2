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

import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
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

        String expectedExpression = String.format("%s = %s AND begins_with ( %s, %s )", ID_KEY, ID_VALUE, SORT_KEY,
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
        QueryConditional.sortBeginsWith(getKey(fakeItemWithNumericSort)).expression(FakeItemWithNumericSort.getTableSchema(), TableMetadata.primaryIndexName());
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
}
