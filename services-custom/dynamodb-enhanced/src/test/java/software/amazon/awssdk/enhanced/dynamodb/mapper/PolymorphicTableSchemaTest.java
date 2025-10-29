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

    // ------------------------------
    // Negative validation tests
    // ------------------------------
    @DynamoDbSupertype(@DynamoDbSupertype.Subtype(discriminatorValue = "one", subtypeClass = SimpleBean.class))
    public static class InvalidParentMissingDynamoDbBeanAnnotation extends SimpleBean {
    }

    @Test
    public void shouldThrowException_ifPolymorphicParentNotAnnotatedAsDynamoDbBean() {
        assertThatThrownBy(() -> TableSchemaFactory.fromClass(InvalidParentMissingDynamoDbBeanAnnotation.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Class does not appear to be a valid DynamoDb annotated class");
    }

    @DynamoDbBean
    @DynamoDbSupertype(@DynamoDbSupertype.Subtype(discriminatorValue = "one", subtypeClass = SimpleBean.class))
    public static class SubtypeNotExtendingDeclaredParent {
    }

    @Test
    public void shouldThrowException_ifSubtypeNotExtendingParent() {
        assertThatThrownBy(() -> TableSchemaFactory.fromClass(SubtypeNotExtendingDeclaredParent.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A subtype class [SimpleBean] listed in the @DynamoDbSupertype annotation "
                        + "is not extending the root class.");
    }

    @DynamoDbBean
    @DynamoDbSupertype( {})
    public static class PolymorphicParentWithNoSubtypes {
    }

    @Test
    public void shouldThrowException_ifNoSubtypeDeclared() {
        assertThatThrownBy(() -> TableSchemaFactory.fromClass(PolymorphicParentWithNoSubtypes.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must declare at least one subtype in @DynamoDbSupertype");
    }
}