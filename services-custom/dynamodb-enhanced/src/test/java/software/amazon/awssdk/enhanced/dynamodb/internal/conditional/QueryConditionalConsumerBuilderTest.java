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

package software.amazon.awssdk.enhanced.dynamodb.internal.conditional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CompositeKeyFakeItem;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class QueryConditionalConsumerBuilderTest {

    @Test
    public void keyEqualTo_consumerBuilder_compositePartitionKeys() {
        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(2));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));

        assertThat(expression.expressionValues().size(), is(2));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
    }

    @Test
    public void keyEqualTo_consumerBuilder_compositeKeysWithSort() {
        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build()))
                                                    .sortValues(Arrays.asList(
                                                        AttributeValue.builder().s("sort1").build(),
                                                        AttributeValue.builder().s("sort2").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(4));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort2", "gsiSort2"));

        assertThat(expression.expressionValues().size(), is(4));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().s("sort1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort2", AttributeValue.builder().s("sort2").build()));
    }

    @Test
    public void sortGreaterThan_consumerBuilder_compositeKeys() {
        Expression expression = QueryConditional.sortGreaterThan(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build()))
                                                    .sortValues(Arrays.asList(
                                                        AttributeValue.builder().s("sort1").build(),
                                                        AttributeValue.builder().s("sort2").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 > :AMZN_MAPPED_gsiSort2";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(4));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort2", "gsiSort2"));

        assertThat(expression.expressionValues().size(), is(4));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().s("sort1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort2", AttributeValue.builder().s("sort2").build()));
    }

    @Test
    public void sortLessThan_consumerBuilder_singleSortKey() {
        Expression expression = QueryConditional.sortLessThan(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build()))
                                                    .sortValues(Collections.singletonList(
                                                        AttributeValue.builder().s("sort1").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 < :AMZN_MAPPED_gsiSort1";
        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(3));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));

        assertThat(expression.expressionValues().size(), is(3));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().s("sort1").build()));
    }

    @Test
    public void sortGreaterThanOrEqualTo_consumerBuilder() {
        Expression expression = QueryConditional.sortGreaterThanOrEqualTo(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build()))
                                                    .sortValues(Collections.singletonList(
                                                        AttributeValue.builder().s("sort1").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 >= :AMZN_MAPPED_gsiSort1";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(3));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));

        assertThat(expression.expressionValues().size(), is(3));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().s("sort1").build()));
    }

    @Test
    public void sortLessThanOrEqualTo_consumerBuilder() {
        Expression expression = QueryConditional.sortLessThanOrEqualTo(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build()))
                                                    .sortValues(Collections.singletonList(
                                                        AttributeValue.builder().s("sort1").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 <= :AMZN_MAPPED_gsiSort1";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(3));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));

        assertThat(expression.expressionValues().size(), is(3));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().s("sort1").build()));
    }

    @Test
    public void sortBetween_consumerBuilder_compositeKeys() {
        Expression expression = QueryConditional.sortBetween(
                                                    k1 -> k1.partitionValues(Arrays.asList(
                                                                AttributeValue.builder().s("key1").build(),
                                                                AttributeValue.builder().s("key2").build()))
                                                            .sortValues(Arrays.asList(
                                                                AttributeValue.builder().s("sort1").build(),
                                                                AttributeValue.builder().s("sortA").build())),
                                                    k2 -> k2.partitionValues(Arrays.asList(
                                                                AttributeValue.builder().s("key1").build(),
                                                                AttributeValue.builder().s("key2").build()))
                                                            .sortValues(Arrays.asList(
                                                                AttributeValue.builder().s("sort1").build(),
                                                                AttributeValue.builder().s("sortZ").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 BETWEEN :AMZN_MAPPED_gsiSort2 AND :AMZN_MAPPED_gsiSort22";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(4));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort2", "gsiSort2"));

        assertThat(expression.expressionValues().size(), is(5));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().s("sort1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort2", AttributeValue.builder().s("sortA").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort22",
                                                           AttributeValue.builder().s("sortZ").build()));
    }

    @Test
    public void sortBeginsWith_consumerBuilder_compositeKeys() {
        Expression expression = QueryConditional.sortBeginsWith(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build()))
                                                    .sortValues(Arrays.asList(
                                                        AttributeValue.builder().s("sort1").build(),
                                                        AttributeValue.builder().s("prefix").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "begins_with(#AMZN_MAPPED_gsiSort2, :AMZN_MAPPED_gsiSort2)";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(4));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort2", "gsiSort2"));

        assertThat(expression.expressionValues().size(), is(4));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().s("sort1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort2",
                                                           AttributeValue.builder().s("prefix").build()));
    }

    @Test
    public void sortBeginsWith_consumerBuilder_singleSortKey() {
        Expression expression = QueryConditional.sortBeginsWith(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build()))
                                                    .sortValues(Collections.singletonList(
                                                        AttributeValue.builder().s("prefix").build())))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "begins_with(#AMZN_MAPPED_gsiSort1, :AMZN_MAPPED_gsiSort1)";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(3));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));

        assertThat(expression.expressionValues().size(), is(3));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1",
                                                           AttributeValue.builder().s("prefix").build()));
    }

    @Test
    public void keyEqualTo_consumerBuilder_backwardCompatibility() {
        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .partitionValue("singleKey"))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "$PRIMARY_INDEX");

        assertThat(expression.expression(), is("#AMZN_MAPPED_id = :AMZN_MAPPED_id"));
        assertThat(expression.expressionNames().size(), is(1));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_id", "id"));
        assertThat(expression.expressionValues().size(), is(1));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_id", AttributeValue.builder().s("singleKey").build()));
    }

    @Test
    public void keyEqualTo_consumerBuilder_mixedMethods() {
        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .partitionValues(Arrays.asList(
                                                        AttributeValue.builder().s("key1").build(),
                                                        AttributeValue.builder().s("key2").build()))
                                                    .sortValue("singleSort"))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(3));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));

        assertThat(expression.expressionValues().size(), is(3));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1",
                                                           AttributeValue.builder().s("singleSort").build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyEqualTo_consumerBuilder_incompletePartitionKeys() {
        QueryConditional.keyEqualTo(k -> k
                            .partitionValues(Collections.singletonList(
                                AttributeValue.builder().s("key1").build())))
                        .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sortGreaterThan_consumerBuilder_noSortKeys() {
        QueryConditional.sortGreaterThan(k -> k
                            .partitionValues(Arrays.asList(
                                AttributeValue.builder().s("key1").build(),
                                AttributeValue.builder().s("key2").build())))
                        .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sortBeginsWith_consumerBuilder_nullSortValue() {
        QueryConditional.sortBeginsWith(k -> k
                            .partitionValues(Arrays.asList(
                                AttributeValue.builder().s("key1").build(),
                                AttributeValue.builder().s("key2").build()))
                            .sortValues(Collections.singletonList(
                                AttributeValue.builder().nul(true).build())))
                        .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sortBeginsWith_consumerBuilder_numericSortValue() {
        QueryConditional.sortBeginsWith(k -> k
                            .partitionValues(Arrays.asList(
                                AttributeValue.builder().s("key1").build(),
                                AttributeValue.builder().s("key2").build()))
                            .sortValues(Collections.singletonList(
                                AttributeValue.builder().n("123").build())))
                        .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");
    }

    @Test
    public void keyEqualTo_consumerBuilder_partitionValuesFromStrings() {
        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .addPartitionValue("key1")
                                                    .addPartitionValue("key2"))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2";
        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(2));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));

        assertThat(expression.expressionValues().size(), is(2));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
    }

    @Test
    public void sortGreaterThan_consumerBuilder_partitionValuesFromStrings() {
        Expression expression = QueryConditional.sortGreaterThan(k -> k
                                                    .addPartitionValue("key1")
                                                    .addPartitionValue("key2")
                                                    .addSortValue("sort1")
                                                    .addSortValue("sort2"))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 > :AMZN_MAPPED_gsiSort2";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(4));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort2", "gsiSort2"));

        assertThat(expression.expressionValues().size(), is(4));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().s("sort1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort2", AttributeValue.builder().s("sort2").build()));
    }

    @Test
    public void keyEqualTo_consumerBuilder_partitionValuesFromNumbers() {
        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .addPartitionValue(123)
                                                    .addPartitionValue(456))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(2));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));

        assertThat(expression.expressionValues().size(), is(2));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().n("123").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().n("456").build()));
    }

    @Test
    public void sortLessThan_consumerBuilder_partitionValuesFromNumbers() {
        Expression expression = QueryConditional.sortLessThan(k -> k
                                                    .addPartitionValue(123)
                                                    .addPartitionValue(456)
                                                    .addSortValue(789))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 < :AMZN_MAPPED_gsiSort1";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(3));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));

        assertThat(expression.expressionValues().size(), is(3));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().n("123").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().n("456").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().n("789").build()));
    }

    @Test
    public void keyEqualTo_consumerBuilder_partitionValuesFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");

        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .addPartitionValue(bytes1)
                                                    .addPartitionValue(bytes2))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(2));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));

        assertThat(expression.expressionValues().size(), is(2));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().b(bytes1).build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().b(bytes2).build()));
    }

    @Test
    public void sortBetween_consumerBuilder_partitionValuesFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");
        SdkBytes sortBytes1 = SdkBytes.fromUtf8String("sortA");
        SdkBytes sortBytes2 = SdkBytes.fromUtf8String("sortZ");

        Expression expression = QueryConditional.sortBetween(
                                                    k1 -> k1.addPartitionValue(bytes1)
                                                            .addPartitionValue(bytes2)
                                                            .addSortValue(sortBytes1),
                                                    k2 -> k2.addPartitionValue(bytes1)
                                                            .addPartitionValue(bytes2)
                                                            .addSortValue(sortBytes2))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 BETWEEN :AMZN_MAPPED_gsiSort1 AND :AMZN_MAPPED_gsiSort12";

        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(3));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));

        assertThat(expression.expressionValues().size(), is(4));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().b(bytes1).build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().b(bytes2).build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1",
                                                           AttributeValue.builder().b(sortBytes1).build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort12",
                                                           AttributeValue.builder().b(sortBytes2).build()));
    }

    @Test
    public void keyEqualTo_consumerBuilder_mixedTypes() {
        Expression expression = QueryConditional.keyEqualTo(k -> k
                                                    .addPartitionValue("key1")
                                                    .addPartitionValue("key2")
                                                    .addSortValue(123)
                                                    .addSortValue(456))
                                                .expression(CompositeKeyFakeItem.SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2";
        assertThat(expression.expression(), is(expectedExpression));

        assertThat(expression.expressionNames().size(), is(4));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort1", "gsiSort1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiSort2", "gsiSort2"));

        assertThat(expression.expressionValues().size(), is(4));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort1", AttributeValue.builder().n("123").build()));
        assertThat(expression.expressionValues(), hasEntry(":AMZN_MAPPED_gsiSort2", AttributeValue.builder().n("456").build()));
    }
}