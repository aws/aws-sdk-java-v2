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
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class StaticImmutableTableSchemaFlattenTest {
    private static final ImmutableRecord TEST_RECORD =
        ImmutableRecord.builder()
                       .id("id123")
                       .attribute1("1")
                       .child1(
                           ImmutableRecord.builder()
                                          .attribute1("2a")
                                          .child1(
                                              ImmutableRecord.builder()
                                                             .attribute1("3a")
                                                             .build()
                                          )
                                          .child2(
                                              ImmutableRecord.builder()
                                                             .attribute1("3b")
                                                             .build()
                                          )
                                          .build()
                       )
                       .child2(
                           ImmutableRecord.builder()
                                          .attribute1("2b")
                                          .child1(
                                              ImmutableRecord.builder()
                                                             .attribute1("4a")
                                                             .build()
                                          )
                                          .child2(
                                              ImmutableRecord.builder()
                                                             .attribute1("4b")
                                                             .build()
                                          )
                                          .build()
                       )
                       .build();

    private static final Map<String, AttributeValue> ITEM_MAP;

    static {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("id", AttributeValue.builder().s("id123").build());
        map.put("attribute1", AttributeValue.builder().s("1").build());
        map.put("attribute2a", AttributeValue.builder().s("2a").build());
        map.put("attribute2b", AttributeValue.builder().s("2b").build());
        map.put("attribute3a", AttributeValue.builder().s("3a").build());
        map.put("attribute3b", AttributeValue.builder().s("3b").build());
        map.put("attribute4a", AttributeValue.builder().s("4a").build());
        map.put("attribute4b", AttributeValue.builder().s("4b").build());

        ITEM_MAP = Collections.unmodifiableMap(map);
    }

    private final TableSchema<ImmutableRecord> childTableSchema4a =
        TableSchema.builder(ImmutableRecord.class, ImmutableRecord.Builder.class)
                   .newItemBuilder(ImmutableRecord::builder, ImmutableRecord.Builder::build)
                   .addAttribute(String.class, a -> a.name("attribute4a")
                                                     .getter(ImmutableRecord::attribute1)
                                                     .setter(ImmutableRecord.Builder::attribute1))
                   .build();

    private final TableSchema<ImmutableRecord> childTableSchema4b =
        TableSchema.builder(ImmutableRecord.class, ImmutableRecord.Builder.class)
                   .newItemBuilder(ImmutableRecord::builder, ImmutableRecord.Builder::build)
                   .addAttribute(String.class, a -> a.name("attribute4b")
                                                     .getter(ImmutableRecord::attribute1)
                                                     .setter(ImmutableRecord.Builder::attribute1))
                   .build();

    private final TableSchema<ImmutableRecord> childTableSchema3a =
        TableSchema.builder(ImmutableRecord.class, ImmutableRecord.Builder.class)
                   .newItemBuilder(ImmutableRecord::builder, ImmutableRecord.Builder::build)
                   .addAttribute(String.class, a -> a.name("attribute3a")
                                                     .getter(ImmutableRecord::attribute1)
                                                     .setter(ImmutableRecord.Builder::attribute1))
                   .build();

    private final TableSchema<ImmutableRecord> childTableSchema3b =
        TableSchema.builder(ImmutableRecord.class, ImmutableRecord.Builder.class)
                   .newItemBuilder(ImmutableRecord::builder, ImmutableRecord.Builder::build)
                   .addAttribute(String.class, a -> a.name("attribute3b")
                                                     .getter(ImmutableRecord::attribute1)
                                                     .setter(ImmutableRecord.Builder::attribute1))
                   .build();

    private final TableSchema<ImmutableRecord> childTableSchema2a =
        TableSchema.builder(ImmutableRecord.class, ImmutableRecord.Builder.class)
                   .newItemBuilder(ImmutableRecord::builder, ImmutableRecord.Builder::build)
                   .addAttribute(String.class, a -> a.name("attribute2a")
                                                     .getter(ImmutableRecord::attribute1)
                                                     .setter(ImmutableRecord.Builder::attribute1))
                   .flatten(childTableSchema3a, ImmutableRecord::getChild1, ImmutableRecord.Builder::child1)
                   .flatten(childTableSchema3b, ImmutableRecord::getChild2, ImmutableRecord.Builder::child2)
                   .build();

    private final TableSchema<ImmutableRecord> childTableSchema2b =
        TableSchema.builder(ImmutableRecord.class, ImmutableRecord.Builder.class)
                   .newItemBuilder(ImmutableRecord::builder, ImmutableRecord.Builder::build)
                   .addAttribute(String.class, a -> a.name("attribute2b")
                                                     .getter(ImmutableRecord::attribute1)
                                                     .setter(ImmutableRecord.Builder::attribute1))
                   .flatten(childTableSchema4a, ImmutableRecord::getChild1, ImmutableRecord.Builder::child1)
                   .flatten(childTableSchema4b, ImmutableRecord::getChild2, ImmutableRecord.Builder::child2)
                   .build();

    private final TableSchema<ImmutableRecord> immutableTableSchema =
        TableSchema.builder(ImmutableRecord.class, ImmutableRecord.Builder.class)
                   .newItemBuilder(ImmutableRecord::builder, ImmutableRecord.Builder::build)
                   .addAttribute(String.class, a -> a.name("id")
                                                     .getter(ImmutableRecord::id)
                                                     .setter(ImmutableRecord.Builder::id)
                                                     .tags(primaryPartitionKey()))
                   .addAttribute(String.class, a -> a.name("attribute1")
                                                     .getter(ImmutableRecord::attribute1)
                                                     .setter(ImmutableRecord.Builder::attribute1))
                   .flatten(childTableSchema2a, ImmutableRecord::getChild1, ImmutableRecord.Builder::child1)
                   .flatten(childTableSchema2b, ImmutableRecord::getChild2, ImmutableRecord.Builder::child2)
                   .build();

    @Test
    public void itemToMap_completeRecord() {
        Map<String, AttributeValue> result = immutableTableSchema.itemToMap(TEST_RECORD, false);

        assertThat(result).isEqualTo(ITEM_MAP);
    }

    @Test
    public void itemToMap_specificAttributes() {
        Map<String, AttributeValue> result =
            immutableTableSchema.itemToMap(TEST_RECORD, Arrays.asList("attribute1", "attribute2a", "attribute4b"));

        Map<String, AttributeValue> expectedResult = new HashMap<>();
        expectedResult.put("attribute1", AttributeValue.builder().s("1").build());
        expectedResult.put("attribute2a", AttributeValue.builder().s("2a").build());
        expectedResult.put("attribute4b", AttributeValue.builder().s("4b").build());

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void itemToMap_specificAttribute() {
        AttributeValue result = immutableTableSchema.attributeValue(TEST_RECORD, "attribute4b");
        assertThat(result).isEqualTo(AttributeValue.builder().s("4b").build());
    }

    @Test
    public void mapToItem() {
        ImmutableRecord record = immutableTableSchema.mapToItem(ITEM_MAP);

        assertThat(record).isEqualTo(TEST_RECORD);
    }

    @Test
    public void attributeNames() {
        Collection<String> result = immutableTableSchema.attributeNames();

        assertThat(result).containsExactlyInAnyOrder(ITEM_MAP.keySet().toArray(new String[]{}));
    }

    public static class ImmutableRecord {
        private final String id;
        private final String attribute1;
        private final ImmutableRecord child1;
        private final ImmutableRecord child2;

        private ImmutableRecord(Builder b) {
            this.id = b.id;
            this.attribute1 = b.attribute1;
            this.child1 = b.child1;
            this.child2 = b.child2;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public String attribute1() {
            return attribute1;
        }

        public ImmutableRecord getChild1() {
            return child1;
        }

        public ImmutableRecord getChild2() {
            return child2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ImmutableRecord that = (ImmutableRecord) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (attribute1 != null ? !attribute1.equals(that.attribute1) : that.attribute1 != null) return false;
            if (child1 != null ? !child1.equals(that.child1) : that.child1 != null) return false;
            return child2 != null ? child2.equals(that.child2) : that.child2 == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (attribute1 != null ? attribute1.hashCode() : 0);
            result = 31 * result + (child1 != null ? child1.hashCode() : 0);
            result = 31 * result + (child2 != null ? child2.hashCode() : 0);
            return result;
        }

        public static class Builder {
            private String id;
            private String attribute1;
            private ImmutableRecord child1;
            private ImmutableRecord child2;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder attribute1(String attribute1) {
                this.attribute1 = attribute1;
                return this;
            }

            public Builder child1(ImmutableRecord child1) {
                this.child1 = child1;
                return this;
            }

            public Builder child2(ImmutableRecord child2) {
                this.child2 = child2;
                return this;
            }

            public ImmutableRecord build() {
                return new ImmutableRecord(this);
            }
        }
    }
}