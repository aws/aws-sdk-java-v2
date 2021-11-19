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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.FlattenedPolyChildOne;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.FlattenedPolyParent;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.FlattenedPolyParentComposite;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.InvalidBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.NestedPolyChildOne;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.NestedPolyParent;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.RecursivePolyChildOne;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.RecursivePolyParent;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimplePolyChildOne;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimplePolyChildTwo;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimplePolyParent;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolymorphicTableSchemaTest {
    @Test
    public void simple_singleNameMapping() {
        TableSchema<SimplePolyParent> tableSchema =
                TableSchemaFactory.fromClass(SimplePolyParent.class);

        SimplePolyChildOne record = new SimplePolyChildOne();
        record.setType("one");
        record.setAttributeOne("attributeOneValue");

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(record, false);
        
        assertThat(itemMap).containsEntry("type", AttributeValue.builder().s("one").build());
        assertThat(itemMap).containsEntry("attributeOne", AttributeValue.builder().s("attributeOneValue").build());

        assertThat(tableSchema.mapToItem(itemMap)).isEqualTo(record);
    }

    @Test
    public void simple_multipleNameMapping() {
        TableSchema<SimplePolyParent> tableSchema =
                TableSchemaFactory.fromClass(SimplePolyParent.class);

        String[] namesToTest = { "two_a", "two_b" };

        Arrays.stream(namesToTest).forEach(nameToTest -> {
            SimplePolyChildTwo record = new SimplePolyChildTwo();
            record.setType(nameToTest);
            record.setAttributeTwo("attributeTwoValue");

            Map<String, AttributeValue> itemMap = tableSchema.itemToMap(record, false);

            assertThat(itemMap).containsEntry("type", AttributeValue.builder().s(nameToTest).build());
            assertThat(itemMap).containsEntry("attributeTwo", AttributeValue.builder().s("attributeTwoValue").build());

            assertThat(tableSchema.mapToItem(itemMap)).isEqualTo(record);
        });
    }

    @Test
    public void flattenedParent_singleNameMapping() {
        TableSchema<FlattenedPolyParent> tableSchema =
                TableSchemaFactory.fromClass(FlattenedPolyParent.class);

        FlattenedPolyParentComposite parentComposite = new FlattenedPolyParentComposite();
        parentComposite.setType("one");

        FlattenedPolyChildOne record = new FlattenedPolyChildOne();
        record.setFlattenedPolyParentComposite(parentComposite);
        record.setAttributeOne("attributeOneValue");

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(record, false);

        assertThat(itemMap).containsEntry("type", AttributeValue.builder().s("one").build());
        assertThat(itemMap).containsEntry("attributeOne", AttributeValue.builder().s("attributeOneValue").build());

        assertThat(tableSchema.mapToItem(itemMap)).isEqualTo(record);
    }

    @Test
    public void nested_singleNameMapping() {
        TableSchema<NestedPolyParent> tableSchema = TableSchemaFactory.fromClass(NestedPolyParent.class);

        SimplePolyChildOne nestedRecord = new SimplePolyChildOne();
        nestedRecord.setType("one");
        nestedRecord.setAttributeOne("attributeOneValue");

        NestedPolyChildOne record = new NestedPolyChildOne();
        record.setType("nested_one");
        record.setSimplePolyParent(nestedRecord);

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(record, false);

        assertThat(itemMap).containsEntry("type", AttributeValue.builder().s("nested_one").build());
        assertThat(itemMap).hasEntrySatisfying("simplePolyParent", av ->
                assertThat(av.m()).satisfies(nestedItemMap -> {
                    assertThat(nestedItemMap).containsEntry("type", AttributeValue.builder().s("one").build());
                    assertThat(nestedItemMap).containsEntry(
                            "attributeOne", AttributeValue.builder().s("attributeOneValue").build());
        }));

        assertThat(tableSchema.mapToItem(itemMap)).isEqualTo(record);
    }

    @Test
    public void recursive_singleNameMapping() {
        TableSchema<RecursivePolyParent> tableSchema = TableSchemaFactory.fromClass(RecursivePolyParent.class);

        RecursivePolyChildOne recursiveRecord1 = new RecursivePolyChildOne();
        recursiveRecord1.setType("recursive_one");
        recursiveRecord1.setAttributeOne("one");

        RecursivePolyChildOne recursiveRecord2 = new RecursivePolyChildOne();
        recursiveRecord2.setType("recursive_one");
        recursiveRecord2.setAttributeOne("two");

        RecursivePolyChildOne record = new RecursivePolyChildOne();
        record.setType("recursive_one");
        record.setRecursivePolyParent(recursiveRecord1);
        record.setRecursivePolyParentOne(recursiveRecord2);
        record.setAttributeOne("parent");

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(record, false);

        assertThat(itemMap).containsEntry("type", AttributeValue.builder().s("recursive_one").build());
        assertThat(itemMap).hasEntrySatisfying("recursivePolyParent", av ->
                assertThat(av.m()).satisfies(nestedItemMap -> {
                    assertThat(nestedItemMap).containsEntry(
                            "type", AttributeValue.builder().s("recursive_one").build());
                    assertThat(nestedItemMap).containsEntry(
                            "attributeOne", AttributeValue.builder().s("one").build());
                }));
        assertThat(itemMap).hasEntrySatisfying("recursivePolyParentOne", av ->
                assertThat(av.m()).satisfies(nestedItemMap -> {
                    assertThat(nestedItemMap).containsEntry(
                            "type", AttributeValue.builder().s("recursive_one").build());
                    assertThat(nestedItemMap).containsEntry(
                            "attributeOne", AttributeValue.builder().s("two").build());
                }));

        assertThat(tableSchema.mapToItem(itemMap)).isEqualTo(record);
    }

    @DynamoDbSubtypes(@DynamoDbSubtypes.Subtype(name = "one", subtypeClass = SimpleBean.class))
    public static class InvalidParentMissingAnnotation extends SimpleBean {
    }

    @Test
    public void parentNotAnnotated_invalid() {
        assertThatThrownBy(() -> PolymorphicTableSchema.create(InvalidParentMissingAnnotation.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid DynamoDb annotated class")
                .hasMessageContaining("InvalidParentMissingAnnotation");
    }

    @DynamoDbSubtypes(@DynamoDbSubtypes.Subtype(name = "one", subtypeClass = SimpleBean.class))
    @DynamoDbBean
    public static class ValidParentSubtypeNotExtendingParent {
    }

    @Test
    public void subtypeNotExtendingParent_invalid() {
        assertThatThrownBy(() -> PolymorphicTableSchema.create(ValidParentSubtypeNotExtendingParent.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not extending the root class")
                .hasMessageContaining("SimpleBean");
    }

    @DynamoDbBean
    public static class InvalidParentNoSubtypeAnnotation {
    }

    @Test
    public void invalidParentNoSubtypeAnnotation_invalid() {
        assertThatThrownBy(() -> PolymorphicTableSchema.create(InvalidParentNoSubtypeAnnotation.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be annotated with @DynamoDbSubtypes")
                .hasMessageContaining("InvalidParentNoSubtypeAnnotation");
    }

    @DynamoDbSubtypes(@DynamoDbSubtypes.Subtype(name = {}, subtypeClass = InvalidParentNameEmptySubtype.class))
    @DynamoDbBean
    public static class InvalidParentNameEmpty {
    }

    @DynamoDbBean
    public static class InvalidParentNameEmptySubtype extends InvalidParentNameEmpty {
    }

    @Test
    public void invalidParentNameEmpty_invalid() {
        assertThatThrownBy(() -> PolymorphicTableSchema.create(InvalidParentNameEmpty.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subtype must have one or more names associated with it")
                .hasMessageContaining("InvalidParentNameEmptySubtype");
    }
}
