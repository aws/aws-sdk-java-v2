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
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CompositeKeyFakeItem;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class QueryConditionalGsiCompositeKeysTest {

    private static final TableSchema<CompositeKeyFakeItem> COMPOSITE_SCHEMA = CompositeKeyFakeItem.SCHEMA;

    @Test
    public void equalToConditional_compositePartitionKeys_allProvided() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .build();

        QueryConditional conditional = new EqualToConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        assertThat(expression.expression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey1", "gsiKey1"));
        assertThat(expression.expressionNames(), hasEntry("#AMZN_MAPPED_gsiKey2", "gsiKey2"));
        assertThat(expression.expressionValues(),
                   hasEntry(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("key1").build()));
        assertThat(expression.expressionValues(),
                   hasEntry(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("key2").build()));
    }

    @Test
    public void equalToConditional_compositeKeys_partitionAndSort() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("sort2").build()))
                     .build();

        QueryConditional conditional = new EqualToConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void equalToConditional_partialSortKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().s("sort1").build()))
                     .build();

        QueryConditional conditional = new EqualToConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalToConditional_incompletePartitionKeys_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Collections.singletonList(
                         AttributeValue.builder().s("key1").build()))
                     .build();

        QueryConditional conditional = new EqualToConditional(key);
        conditional.expression(COMPOSITE_SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalToConditional_tooManySortKeys_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("sort2").build(),
                         AttributeValue.builder().s("sort3").build()))
                     .build();

        QueryConditional conditional = new EqualToConditional(key);
        conditional.expression(COMPOSITE_SCHEMA, "gsi1");
    }

    @Test
    public void singleKeyItemConditional_greaterThan_singleSortKey() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().s("sort1").build()))
                     .build();

        QueryConditional conditional = new SingleKeyItemConditional(key, ">");
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 > :AMZN_MAPPED_gsiSort1";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void singleKeyItemConditional_lessThanOrEqual_multipleSortKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("sort2").build()))
                     .build();

        QueryConditional conditional = new SingleKeyItemConditional(key, "<=");
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 <= :AMZN_MAPPED_gsiSort2";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test(expected = IllegalArgumentException.class)
    public void singleKeyItemConditional_noSortKeys_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .build();

        QueryConditional conditional = new SingleKeyItemConditional(key, ">");
        conditional.expression(COMPOSITE_SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void singleKeyItemConditional_nullSortValue_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().nul(true).build()))
                     .build();

        QueryConditional conditional = new SingleKeyItemConditional(key, ">");
        conditional.expression(COMPOSITE_SCHEMA, "gsi1");
    }

    @Test
    public void betweenConditional_singleSortKey() {
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

        QueryConditional conditional = new BetweenConditional(key1, key2);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 BETWEEN :AMZN_MAPPED_gsiSort1 AND :AMZN_MAPPED_gsiSort12";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void betweenConditional_multipleSortKeys() {
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

        QueryConditional conditional = new BetweenConditional(key1, key2);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 BETWEEN :AMZN_MAPPED_gsiSort2 AND :AMZN_MAPPED_gsiSort22";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test(expected = IllegalArgumentException.class)
    public void betweenConditional_noSortKeys_throwsException() {
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

        QueryConditional conditional = new BetweenConditional(key1, key2);
        conditional.expression(COMPOSITE_SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void betweenConditional_nullSortValue_throwsException() {
        Key key1 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("key1").build(),
                          AttributeValue.builder().s("key2").build()))
                      .sortValues(Collections.singletonList(
                          AttributeValue.builder().nul(true).build()))
                      .build();

        Key key2 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("key1").build(),
                          AttributeValue.builder().s("key2").build()))
                      .sortValues(Collections.singletonList(
                          AttributeValue.builder().s("sortZ").build()))
                      .build();

        QueryConditional conditional = new BetweenConditional(key1, key2);
        conditional.expression(COMPOSITE_SCHEMA, "gsi1");
    }

    @Test
    public void beginsWithConditional_singleSortKey() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().s("prefix").build()))
                     .build();

        QueryConditional conditional = new BeginsWithConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "begins_with(#AMZN_MAPPED_gsiSort1, :AMZN_MAPPED_gsiSort1)";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void beginsWithConditional_multipleSortKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("prefix").build()))
                     .build();

        QueryConditional conditional = new BeginsWithConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "begins_with(#AMZN_MAPPED_gsiSort2, :AMZN_MAPPED_gsiSort2)";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginsWithConditional_noSortKeys_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .build();

        QueryConditional conditional = new BeginsWithConditional(key);
        conditional.expression(COMPOSITE_SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginsWithConditional_nullSortValue_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().nul(true).build()))
                     .build();

        QueryConditional conditional = new BeginsWithConditional(key);
        conditional.expression(COMPOSITE_SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginsWithConditional_numericSortValue_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().n("123").build()))
                     .build();

        QueryConditional conditional = new BeginsWithConditional(key);
        conditional.expression(COMPOSITE_SCHEMA, "gsi1");
    }

    @Test
    public void equalToConditional_backwardCompatibility_singleKey() {
        Key singleKey = Key.builder()
                           .partitionValue("singlePartition")
                           .build();

        QueryConditional conditional = new EqualToConditional(singleKey);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "$PRIMARY_INDEX");

        assertThat(expression.expression(), is("#AMZN_MAPPED_id = :AMZN_MAPPED_id"));
    }

    @Test
    public void queryConditional_keyEqualTo_compositeKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().s("sort1").build()))
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void queryConditional_sortGreaterThan_compositeKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("sort2").build()))
                     .build();

        Expression expression = QueryConditional.sortGreaterThan(key)
                                                .expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 > :AMZN_MAPPED_gsiSort2";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void queryConditional_sortBetween_compositeKeys() {
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
                                                .expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 BETWEEN :AMZN_MAPPED_gsiSort1 AND :AMZN_MAPPED_gsiSort12";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void queryConditional_sortBeginsWith_compositeKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Collections.singletonList(
                         AttributeValue.builder().s("prefix").build()))
                     .build();

        Expression expression = QueryConditional.sortBeginsWith(key)
                                                .expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "begins_with(#AMZN_MAPPED_gsiSort1, :AMZN_MAPPED_gsiSort1)";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void equalToConditional_partitionValuesFromStrings() {
        Key key = Key.builder()
                     .addPartitionValue("key1")
                     .addPartitionValue("key2")
                     .build();

        QueryConditional conditional = new EqualToConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        assertThat(expression.expression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2"));
    }

    @Test
    public void singleKeyItemConditional_partitionValuesFromStrings() {
        Key key = Key.builder()
                     .addPartitionValue("key1")
                     .addPartitionValue("key2")
                     .addSortValue("sort1")
                     .addSortValue("sort2")
                     .build();

        QueryConditional conditional = new SingleKeyItemConditional(key, ">=");
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 >= :AMZN_MAPPED_gsiSort2";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void equalToConditional_partitionValuesFromNumbers() {
        Key key = Key.builder()
                     .addPartitionValue(123)
                     .addPartitionValue(456)
                     .build();

        QueryConditional conditional = new EqualToConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        assertThat(expression.expression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2"));
    }

    @Test
    public void betweenConditional_partitionValuesFromNumbers() {
        Key key1 = Key.builder()
                      .addPartitionValue(123)
                      .addPartitionValue(456)
                      .addSortValue(100)
                      .build();

        Key key2 = Key.builder()
                      .addPartitionValue(123)
                      .addPartitionValue(456)
                      .addSortValue(200)
                      .build();

        QueryConditional conditional = new BetweenConditional(key1, key2);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 BETWEEN :AMZN_MAPPED_gsiSort1 AND :AMZN_MAPPED_gsiSort12";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void equalToConditional_partitionValuesFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");

        Key key = Key.builder()
                     .addPartitionValue(bytes1)
                     .addPartitionValue(bytes2)
                     .build();

        QueryConditional conditional = new EqualToConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        assertThat(expression.expression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2"));
    }

    @Test
    public void beginsWithConditional_partitionValuesFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");
        SdkBytes sortBytes = SdkBytes.fromUtf8String("prefix");

        Key key = Key.builder()
                     .addPartitionValue(bytes1)
                     .addPartitionValue(bytes2)
                     .addSortValue(sortBytes)
                     .build();

        QueryConditional conditional = new BeginsWithConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "begins_with(#AMZN_MAPPED_gsiSort1, :AMZN_MAPPED_gsiSort1)";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void equalToConditional_mixedTypes() {
        Key key = Key.builder()
                     .addPartitionValue("key1")
                     .addPartitionValue("key2")
                     .addSortValue(123)
                     .addSortValue(456)
                     .build();

        QueryConditional conditional = new EqualToConditional(key);
        Expression expression = conditional.expression(COMPOSITE_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND " +
                                    "#AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND " +
                                    "#AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND " +
                                    "#AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2";
        assertThat(expression.expression(), is(expectedExpression));
    }
}