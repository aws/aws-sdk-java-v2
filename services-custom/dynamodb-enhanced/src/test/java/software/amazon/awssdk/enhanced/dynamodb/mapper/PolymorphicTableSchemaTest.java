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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSupertype;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic.FlattenedPolymorphicChild;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic.FlattenedPolymorphicParent;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic.FlattenedPolymorphicParentComposite;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic.NestedPolymorphicChild;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic.NestedPolymorphicParent;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic.RecursivePolymorphicChild;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic.RecursivePolymorphicParent;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic.SimplePolymorphicChildOne;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.polymorphic.SimplePolymorphicParent;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class PolymorphicTableSchemaTest {

    @Test
    public void testSerialize_simplePolymorphicRecord() {
        TableSchema<SimplePolymorphicParent> tableSchema =
            TableSchemaFactory.fromClass(SimplePolymorphicParent.class);

        SimplePolymorphicChildOne record = new SimplePolymorphicChildOne();
        record.setAttributeOne("attributeOneValue");

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(record, false);

        assertThat(itemMap).containsEntry("type", AttributeValue.builder().s("one").build());
        assertThat(itemMap).containsEntry("attributeOne", AttributeValue.builder().s("attributeOneValue").build());

        assertThat(tableSchema.mapToItem(itemMap)).isEqualTo(record);
    }

    @Test
    public void testSerialize_flattenedPolymorphicRecord() {
        TableSchema<FlattenedPolymorphicParent> tableSchema =
            TableSchemaFactory.fromClass(FlattenedPolymorphicParent.class);

        FlattenedPolymorphicParentComposite parentComposite = new FlattenedPolymorphicParentComposite();
        parentComposite.setCompositeAttribute("compositeAttributeValue");

        FlattenedPolymorphicChild record = new FlattenedPolymorphicChild();
        record.setFlattenedPolyParentComposite(parentComposite);
        record.setAttributeOne("attributeOneValue");

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(record, false);

        assertThat(itemMap).containsEntry("type", AttributeValue.builder().s("one").build());
        assertThat(itemMap).containsEntry("attributeOne", AttributeValue.builder().s("attributeOneValue").build());
        assertThat(itemMap).containsEntry("compositeAttribute", AttributeValue.builder().s("compositeAttributeValue").build());
        assertThat(tableSchema.mapToItem(itemMap)).isEqualTo(record);
    }

    @Test
    public void testSerialize_nestedPolymorphicRecord() {
        TableSchema<NestedPolymorphicParent> tableSchema = TableSchemaFactory.fromClass(NestedPolymorphicParent.class);

        SimplePolymorphicChildOne nestedRecord = new SimplePolymorphicChildOne();
        nestedRecord.setAttributeOne("attributeOneValue");
        nestedRecord.setParentAttribute("parentAttributeValue");

        NestedPolymorphicChild record = new NestedPolymorphicChild();
        record.setSimplePolyParent(nestedRecord);

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(record, false);

        assertThat(itemMap).containsEntry("type", AttributeValue.builder().s("nested_one").build());
        assertThat(itemMap).hasEntrySatisfying("simplePolyParent", av ->
            assertThat(av.m()).satisfies(nestedItemMap -> {
                assertThat(nestedItemMap).containsEntry("type", AttributeValue.builder().s("one").build());
                assertThat(nestedItemMap).containsEntry(
                    "attributeOne", AttributeValue.builder().s("attributeOneValue").build());
                assertThat(nestedItemMap).containsEntry(
                    "parentAttribute", AttributeValue.builder().s("parentAttributeValue").build());
            }));
    }

    @Test
    public void testSerialize_recursivePolymorphicRecord() {
        TableSchema<RecursivePolymorphicParent> tableSchema = TableSchemaFactory.fromClass(RecursivePolymorphicParent.class);

        RecursivePolymorphicChild recursiveRecord1 = new RecursivePolymorphicChild();
        recursiveRecord1.setAttributeOne("one");

        RecursivePolymorphicChild recursiveRecord2 = new RecursivePolymorphicChild();
        recursiveRecord2.setAttributeOne("two");

        RecursivePolymorphicChild record = new RecursivePolymorphicChild();
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

    @DynamoDbSupertype(@DynamoDbSupertype.Subtype(discriminatorValue = "one", subtypeClass = SimpleBean.class))
    public static class InvalidParentMissingAnnotation extends SimpleBean {
    }

    @Test
    public void shouldThrowException_ifPolymorphicParentNotAnnotatedAsDynamoDbBean() {
        assertThatThrownBy(() -> PolymorphicTableSchema.create(InvalidParentMissingAnnotation.class, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Class does not appear to be a valid DynamoDb annotated class. [class = \"class software.amazon.awssdk"
                        + ".enhanced.dynamodb.mapper.PolymorphicTableSchemaTest$InvalidParentMissingAnnotation\"]");
    }

    @DynamoDbSupertype(@DynamoDbSupertype.Subtype(discriminatorValue = "one", subtypeClass = SimpleBean.class))
    @DynamoDbBean
    public static class ValidParentSubtypeNotExtendingParent {
    }

    @Test
    public void shouldThrowException_ifSubtypeNotExtendingParent() {
        assertThatThrownBy(() -> PolymorphicTableSchema.create(ValidParentSubtypeNotExtendingParent.class, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A subtype class [SimpleBean] listed in the @DynamoDbSupertype annotation is not extending the root "
                        + "class.");
    }

    @DynamoDbBean
    public static class InvalidParentNoSubtypeAnnotation {
    }

    @Test
    public void shouldThrowException_ifNoSubtypeAnnotation() {
        assertThatThrownBy(() -> PolymorphicTableSchema.create(InvalidParentNoSubtypeAnnotation.class, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A DynamoDb polymorphic class [InvalidParentNoSubtypeAnnotation] "
                        + "must be annotated with @DynamoDbSupertype");
    }
}
