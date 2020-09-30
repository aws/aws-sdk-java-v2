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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class StaticImmutableTableSchemaExtendTest {
    private static final ImmutableRecord TEST_RECORD = ImmutableRecord.builder()
                                                                      .id("id123")
                                                                      .attribute1("one")
                                                                      .attribute2(2)
                                                                      .attribute3("three")
                                                                      .build();

    private static final Map<String, AttributeValue> ITEM_MAP;

    static {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("id", AttributeValue.builder().s("id123").build());
        map.put("attribute1", AttributeValue.builder().s("one").build());
        map.put("attribute2", AttributeValue.builder().n("2").build());
        map.put("attribute3", AttributeValue.builder().s("three").build());
        ITEM_MAP = Collections.unmodifiableMap(map);
    }

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
                   .addAttribute(int.class, a -> a.name("attribute2")
                                                  .getter(ImmutableRecord::attribute2)
                                                  .setter(ImmutableRecord.Builder::attribute2))
                   .extend(TableSchema.builder(SuperRecord.class, SuperRecord.Builder.class)
                                      .addAttribute(String.class, a -> a.name("attribute3")
                                                                        .getter(SuperRecord::attribute3)
                                                                        .setter(SuperRecord.Builder::attribute3))
                                      .build())
                   .build();

    @Test
    public void itemToMap() {
        Map<String, AttributeValue> result = immutableTableSchema.itemToMap(TEST_RECORD, false);

        assertThat(result).isEqualTo(ITEM_MAP);
    }

    @Test
    public void mapToItem() {
        ImmutableRecord record = immutableTableSchema.mapToItem(ITEM_MAP);

        assertThat(record).isEqualTo(TEST_RECORD);
    }

    public static class ImmutableRecord extends SuperRecord {
        private final String id;
        private final String attribute1;
        private final int attribute2;

        public ImmutableRecord(Builder b) {
            super(b);
            this.id = b.id;
            this.attribute1 = b.attribute1;
            this.attribute2 = b.attribute2;
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

        public int attribute2() {
            return attribute2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ImmutableRecord that = (ImmutableRecord) o;

            if (attribute2 != that.attribute2) return false;
            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            return attribute1 != null ? attribute1.equals(that.attribute1) : that.attribute1 == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (attribute1 != null ? attribute1.hashCode() : 0);
            result = 31 * result + attribute2;
            return result;
        }

        public static class Builder extends SuperRecord.Builder<Builder> {
            private String id;
            private String attribute1;
            private int attribute2;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder attribute1(String attribute1) {
                this.attribute1 = attribute1;
                return this;
            }

            public Builder attribute2(int attribute2) {
                this.attribute2 = attribute2;
                return this;
            }

            public ImmutableRecord build() {
                return new ImmutableRecord(this);
            }
        }
    }

    public static class SuperRecord {
        private final String attribute3;

        public SuperRecord(Builder<?> b) {

            this.attribute3 = b.attribute3;
        }

        public String attribute3() {
            return attribute3;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SuperRecord that = (SuperRecord) o;

            return attribute3 != null ? attribute3.equals(that.attribute3) : that.attribute3 == null;
        }

        @Override
        public int hashCode() {
            return attribute3 != null ? attribute3.hashCode() : 0;
        }

        public static class Builder<T extends Builder<T>> {
            private String attribute3;

            public T attribute3(String attribute3) {
                this.attribute3 = attribute3;
                return (T) this;
            }
        }
    }
}