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
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class QueryConditionalMaxKeysTest {

    private static class MaxKeysItem {
        private String id;
        private String pk1;
        private String pk2;
        private String pk3;
        private String pk4;
        private String sk1;
        private String sk2;
        private String sk3;
        private String sk4;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPk1() {
            return pk1;
        }

        public void setPk1(String pk1) {
            this.pk1 = pk1;
        }

        public String getPk2() {
            return pk2;
        }

        public void setPk2(String pk2) {
            this.pk2 = pk2;
        }

        public String getPk3() {
            return pk3;
        }

        public void setPk3(String pk3) {
            this.pk3 = pk3;
        }

        public String getPk4() {
            return pk4;
        }

        public void setPk4(String pk4) {
            this.pk4 = pk4;
        }

        public String getSk1() {
            return sk1;
        }

        public void setSk1(String sk1) {
            this.sk1 = sk1;
        }

        public String getSk2() {
            return sk2;
        }

        public void setSk2(String sk2) {
            this.sk2 = sk2;
        }

        public String getSk3() {
            return sk3;
        }

        public void setSk3(String sk3) {
            this.sk3 = sk3;
        }

        public String getSk4() {
            return sk4;
        }

        public void setSk4(String sk4) {
            this.sk4 = sk4;
        }
    }

    private static final TableSchema<MaxKeysItem> MAX_KEYS_SCHEMA =
        StaticTableSchema.builder(MaxKeysItem.class)
                         .newItemSupplier(MaxKeysItem::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(MaxKeysItem::getId)
                                                           .setter(MaxKeysItem::setId)
                                                           .tags(StaticAttributeTags.primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("pk1")
                                                           .getter(MaxKeysItem::getPk1)
                                                           .setter(MaxKeysItem::setPk1)
                                                           .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.FIRST)))
                         .addAttribute(String.class, a -> a.name("pk2")
                                                           .getter(MaxKeysItem::getPk2)
                                                           .setter(MaxKeysItem::setPk2)
                                                           .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.SECOND)))
                         .addAttribute(String.class, a -> a.name("pk3")
                                                           .getter(MaxKeysItem::getPk3)
                                                           .setter(MaxKeysItem::setPk3)
                                                           .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.THIRD)))
                         .addAttribute(String.class, a -> a.name("pk4")
                                                           .getter(MaxKeysItem::getPk4)
                                                           .setter(MaxKeysItem::setPk4)
                                                           .tags(StaticAttributeTags.secondaryPartitionKey("gsi1", Order.FOURTH)))
                         .addAttribute(String.class, a -> a.name("sk1")
                                                           .getter(MaxKeysItem::getSk1)
                                                           .setter(MaxKeysItem::setSk1)
                                                           .tags(StaticAttributeTags.secondarySortKey("gsi1", Order.FIRST)))
                         .addAttribute(String.class, a -> a.name("sk2")
                                                           .getter(MaxKeysItem::getSk2)
                                                           .setter(MaxKeysItem::setSk2)
                                                           .tags(StaticAttributeTags.secondarySortKey("gsi1", Order.SECOND)))
                         .addAttribute(String.class, a -> a.name("sk3")
                                                           .getter(MaxKeysItem::getSk3)
                                                           .setter(MaxKeysItem::setSk3)
                                                           .tags(StaticAttributeTags.secondarySortKey("gsi1", Order.THIRD)))
                         .addAttribute(String.class, a -> a.name("sk4")
                                                           .getter(MaxKeysItem::getSk4)
                                                           .setter(MaxKeysItem::setSk4)
                                                           .tags(StaticAttributeTags.secondarySortKey("gsi1", Order.FOURTH)))
                         .build();

    @Test
    public void equalTo_maxPartitionKeys_allProvided() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("pk1").build(),
                         AttributeValue.builder().s("pk2").build(),
                         AttributeValue.builder().s("pk3").build(),
                         AttributeValue.builder().s("pk4").build()))
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void equalTo_maxSortKeys_allProvided() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("pk1").build(),
                         AttributeValue.builder().s("pk2").build(),
                         AttributeValue.builder().s("pk3").build(),
                         AttributeValue.builder().s("pk4").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sk1").build(),
                         AttributeValue.builder().s("sk2").build(),
                         AttributeValue.builder().s("sk3").build(),
                         AttributeValue.builder().s("sk4").build()))
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 = :AMZN_MAPPED_sk1 AND " +
                                    "#AMZN_MAPPED_sk2 = :AMZN_MAPPED_sk2 AND " +
                                    "#AMZN_MAPPED_sk3 = :AMZN_MAPPED_sk3 AND " +
                                    "#AMZN_MAPPED_sk4 = :AMZN_MAPPED_sk4";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void sortGreaterThan_maxKeys_rightmostSortKey() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("pk1").build(),
                         AttributeValue.builder().s("pk2").build(),
                         AttributeValue.builder().s("pk3").build(),
                         AttributeValue.builder().s("pk4").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sk1").build(),
                         AttributeValue.builder().s("sk2").build(),
                         AttributeValue.builder().s("sk3").build(),
                         AttributeValue.builder().s("sk4").build()))
                     .build();

        Expression expression = QueryConditional.sortGreaterThan(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 = :AMZN_MAPPED_sk1 AND " +
                                    "#AMZN_MAPPED_sk2 = :AMZN_MAPPED_sk2 AND " +
                                    "#AMZN_MAPPED_sk3 = :AMZN_MAPPED_sk3 AND " +
                                    "#AMZN_MAPPED_sk4 > :AMZN_MAPPED_sk4";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void sortBetween_maxKeys() {
        Key key1 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("pk1").build(),
                          AttributeValue.builder().s("pk2").build(),
                          AttributeValue.builder().s("pk3").build(),
                          AttributeValue.builder().s("pk4").build()))
                      .sortValues(Arrays.asList(
                          AttributeValue.builder().s("sk1").build(),
                          AttributeValue.builder().s("sk2").build(),
                          AttributeValue.builder().s("sk3").build(),
                          AttributeValue.builder().s("skA").build()))
                      .build();

        Key key2 = Key.builder()
                      .partitionValues(Arrays.asList(
                          AttributeValue.builder().s("pk1").build(),
                          AttributeValue.builder().s("pk2").build(),
                          AttributeValue.builder().s("pk3").build(),
                          AttributeValue.builder().s("pk4").build()))
                      .sortValues(Arrays.asList(
                          AttributeValue.builder().s("sk1").build(),
                          AttributeValue.builder().s("sk2").build(),
                          AttributeValue.builder().s("sk3").build(),
                          AttributeValue.builder().s("skZ").build()))
                      .build();

        Expression expression = QueryConditional.sortBetween(key1, key2)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 = :AMZN_MAPPED_sk1 AND " +
                                    "#AMZN_MAPPED_sk2 = :AMZN_MAPPED_sk2 AND " +
                                    "#AMZN_MAPPED_sk3 = :AMZN_MAPPED_sk3 AND " +
                                    "#AMZN_MAPPED_sk4 BETWEEN :AMZN_MAPPED_sk4 AND :AMZN_MAPPED_sk42";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void sortBeginsWith_maxKeys() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("pk1").build(),
                         AttributeValue.builder().s("pk2").build(),
                         AttributeValue.builder().s("pk3").build(),
                         AttributeValue.builder().s("pk4").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sk1").build(),
                         AttributeValue.builder().s("sk2").build(),
                         AttributeValue.builder().s("sk3").build(),
                         AttributeValue.builder().s("prefix").build()))
                     .build();

        Expression expression = QueryConditional.sortBeginsWith(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 = :AMZN_MAPPED_sk1 AND " +
                                    "#AMZN_MAPPED_sk2 = :AMZN_MAPPED_sk2 AND " +
                                    "#AMZN_MAPPED_sk3 = :AMZN_MAPPED_sk3 AND " +
                                    "begins_with(#AMZN_MAPPED_sk4, :AMZN_MAPPED_sk4)";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void equalTo_partialSortKeys_firstThree() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("pk1").build(),
                         AttributeValue.builder().s("pk2").build(),
                         AttributeValue.builder().s("pk3").build(),
                         AttributeValue.builder().s("pk4").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sk1").build(),
                         AttributeValue.builder().s("sk2").build(),
                         AttributeValue.builder().s("sk3").build()))
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 = :AMZN_MAPPED_sk1 AND " +
                                    "#AMZN_MAPPED_sk2 = :AMZN_MAPPED_sk2 AND " +
                                    "#AMZN_MAPPED_sk3 = :AMZN_MAPPED_sk3";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void sortGreaterThan_partialSortKeys_secondKey() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("pk1").build(),
                         AttributeValue.builder().s("pk2").build(),
                         AttributeValue.builder().s("pk3").build(),
                         AttributeValue.builder().s("pk4").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sk1").build(),
                         AttributeValue.builder().s("sk2").build()))
                     .build();

        Expression expression = QueryConditional.sortGreaterThan(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 = :AMZN_MAPPED_sk1 AND " +
                                    "#AMZN_MAPPED_sk2 > :AMZN_MAPPED_sk2";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_incompletePartitionKeys_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("pk1").build(),
                         AttributeValue.builder().s("pk2").build(),
                         AttributeValue.builder().s("pk3").build()))
                     .build();

        QueryConditional.keyEqualTo(key).expression(MAX_KEYS_SCHEMA, "gsi1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_tooManySortKeys_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("pk1").build(),
                         AttributeValue.builder().s("pk2").build(),
                         AttributeValue.builder().s("pk3").build(),
                         AttributeValue.builder().s("pk4").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sk1").build(),
                         AttributeValue.builder().s("sk2").build(),
                         AttributeValue.builder().s("sk3").build(),
                         AttributeValue.builder().s("sk4").build(),
                         AttributeValue.builder().s("sk5").build()))
                     .build();

        QueryConditional.keyEqualTo(key).expression(MAX_KEYS_SCHEMA, "gsi1");
    }

    @Test
    public void equalTo_mixedDataTypes() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("stringKey").build(),
                         AttributeValue.builder().n("123").build(),
                         AttributeValue.builder().s("anotherString").build(),
                         AttributeValue.builder().n("456").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sortString").build(),
                         AttributeValue.builder().n("789").build()))
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 = :AMZN_MAPPED_sk1 AND " +
                                    "#AMZN_MAPPED_sk2 = :AMZN_MAPPED_sk2";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalTo_nullInMiddleOfSortKeys_throwsException() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("pk1").build(),
                         AttributeValue.builder().s("pk2").build(),
                         AttributeValue.builder().s("pk3").build(),
                         AttributeValue.builder().s("pk4").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sk1").build(),
                         AttributeValue.builder().nul(true).build(),
                         AttributeValue.builder().s("sk3").build()))
                     .build();

        QueryConditional.keyEqualTo(key)
                        .expression(MAX_KEYS_SCHEMA, "gsi1");
    }

    @Test
    public void equalTo_addMaxPartitionKeysFromStrings() {
        Key key = Key.builder()
                     .addPartitionValue("pk1")
                     .addPartitionValue("pk2")
                     .addPartitionValue("pk3")
                     .addPartitionValue("pk4")
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void sortGreaterThan_addMaxKeysFromStrings() {
        Key key = Key.builder()
                     .addPartitionValue("pk1")
                     .addPartitionValue("pk2")
                     .addPartitionValue("pk3")
                     .addPartitionValue("pk4")
                     .addSortValue("sk1")
                     .addSortValue("sk2")
                     .addSortValue("sk3")
                     .addSortValue("sk4")
                     .build();

        Expression expression = QueryConditional.sortGreaterThan(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 = :AMZN_MAPPED_sk1 AND " +
                                    "#AMZN_MAPPED_sk2 = :AMZN_MAPPED_sk2 AND " +
                                    "#AMZN_MAPPED_sk3 = :AMZN_MAPPED_sk3 AND " +
                                    "#AMZN_MAPPED_sk4 > :AMZN_MAPPED_sk4";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void equalTo_addMaxPartitionKeysFromNumbers() {
        Key key = Key.builder()
                     .addPartitionValue(1)
                     .addPartitionValue(2)
                     .addPartitionValue(3)
                     .addPartitionValue(4)
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void sortLessThan_addMaxKeysFromNumbers() {
        Key key = Key.builder()
                     .addPartitionValue(1)
                     .addPartitionValue(2)
                     .addPartitionValue(3)
                     .addPartitionValue(4)
                     .addSortValue(10)
                     .addSortValue(20)
                     .addSortValue(30)
                     .addSortValue(40)
                     .build();

        Expression expression = QueryConditional.sortLessThan(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 = :AMZN_MAPPED_sk1 AND " +
                                    "#AMZN_MAPPED_sk2 = :AMZN_MAPPED_sk2 AND " +
                                    "#AMZN_MAPPED_sk3 = :AMZN_MAPPED_sk3 AND " +
                                    "#AMZN_MAPPED_sk4 < :AMZN_MAPPED_sk4";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void equalTo_addMaxPartitionKeysFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");
        SdkBytes bytes3 = SdkBytes.fromUtf8String("binary3");
        SdkBytes bytes4 = SdkBytes.fromUtf8String("binary4");

        Key key = Key.builder()
                     .addPartitionValue(bytes1)
                     .addPartitionValue(bytes2)
                     .addPartitionValue(bytes3)
                     .addPartitionValue(bytes4)
                     .build();

        Expression expression = QueryConditional.keyEqualTo(key)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4";
        assertThat(expression.expression(), is(expectedExpression));
    }

    @Test
    public void sortBetween_maxKeysFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");
        SdkBytes bytes3 = SdkBytes.fromUtf8String("binary3");
        SdkBytes bytes4 = SdkBytes.fromUtf8String("binary4");
        SdkBytes sortBytesA = SdkBytes.fromUtf8String("sortA");
        SdkBytes sortBytesZ = SdkBytes.fromUtf8String("sortZ");

        Key key1 = Key.builder()
                      .addPartitionValue(bytes1)
                      .addPartitionValue(bytes2)
                      .addPartitionValue(bytes3)
                      .addPartitionValue(bytes4)
                      .addSortValue(sortBytesA)
                      .build();

        Key key2 = Key.builder()
                      .addPartitionValue(bytes1)
                      .addPartitionValue(bytes2)
                      .addPartitionValue(bytes3)
                      .addPartitionValue(bytes4)
                      .addSortValue(sortBytesZ)
                      .build();

        Expression expression = QueryConditional.sortBetween(key1, key2)
                                                .expression(MAX_KEYS_SCHEMA, "gsi1");

        String expectedExpression = "#AMZN_MAPPED_pk1 = :AMZN_MAPPED_pk1 AND " +
                                    "#AMZN_MAPPED_pk2 = :AMZN_MAPPED_pk2 AND " +
                                    "#AMZN_MAPPED_pk3 = :AMZN_MAPPED_pk3 AND " +
                                    "#AMZN_MAPPED_pk4 = :AMZN_MAPPED_pk4 AND " +
                                    "#AMZN_MAPPED_sk1 BETWEEN :AMZN_MAPPED_sk1 AND :AMZN_MAPPED_sk12";
        assertThat(expression.expression(), is(expectedExpression));
    }
}