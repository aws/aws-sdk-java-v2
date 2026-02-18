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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.updateBehavior;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;

/**
 * Test models for update behavior functionality testing across 4 schema types: Bean, Static, Immutable, and StaticImmutable.
 * Provides simple/nested model variants, each containing writeAlwaysField (WRITE_ALWAYS) and writeOnceField (WRITE_IF_NOT_EXISTS)
 * attributes at root and nested levels. Used by NestedUpdateBehaviorTest to validate @DynamoDbUpdateBehavior annotations.
 */
public final class UpdateBehaviorTestModels {

    private UpdateBehaviorTestModels() {
    }

    @DynamoDbBean
    public static class SimpleBean {
        private String id;
        private String writeAlwaysField;
        private String writeOnceField;
        private List<SimpleBeanChild> childList;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public SimpleBean setId(String id) {
            this.id = id;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_ALWAYS)
        public String getWriteAlwaysField() {
            return writeAlwaysField;
        }

        public SimpleBean setWriteAlwaysField(String writeAlwaysField) {
            this.writeAlwaysField = writeAlwaysField;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getWriteOnceField() {
            return writeOnceField;
        }

        public SimpleBean setWriteOnceField(String writeOnceField) {
            this.writeOnceField = writeOnceField;
            return this;
        }

        public List<SimpleBeanChild> getChildList() {
            return childList == null ? null : Collections.unmodifiableList(childList);
        }

        public SimpleBean setChildList(List<SimpleBeanChild> childList) {
            this.childList = childList == null ? null : Collections.unmodifiableList(childList);
            return this;
        }
    }

    @DynamoDbBean
    public static class NestedBean {
        private String id;
        private String writeAlwaysField;
        private String writeOnceField;
        private NestedBeanChild child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public NestedBean setId(String id) {
            this.id = id;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_ALWAYS)
        public String getWriteAlwaysField() {
            return writeAlwaysField;
        }

        public NestedBean setWriteAlwaysField(String writeAlwaysField) {
            this.writeAlwaysField = writeAlwaysField;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getWriteOnceField() {
            return writeOnceField;
        }

        public NestedBean setWriteOnceField(String writeOnceField) {
            this.writeOnceField = writeOnceField;
            return this;
        }

        public NestedBeanChild getChild() {
            return child;
        }

        public NestedBean setChild(NestedBeanChild child) {
            this.child = child;
            return this;
        }
    }

    @DynamoDbBean
    public static class SimpleBeanChild {
        private String id;
        private String childAlwaysUpdate;
        private String childWriteOnce;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public SimpleBeanChild setId(String id) {
            this.id = id;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_ALWAYS)
        public String getChildAlwaysUpdate() {
            return childAlwaysUpdate;
        }

        public SimpleBeanChild setChildAlwaysUpdate(String childAlwaysUpdate) {
            this.childAlwaysUpdate = childAlwaysUpdate;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getChildWriteOnce() {
            return childWriteOnce;
        }

        public SimpleBeanChild setChildWriteOnce(String childWriteOnce) {
            this.childWriteOnce = childWriteOnce;
            return this;
        }
    }

    @DynamoDbBean
    public static class NestedBeanChild {
        private String childAlwaysUpdate;
        private String childWriteOnce;

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_ALWAYS)
        public String getChildAlwaysUpdate() {
            return childAlwaysUpdate;
        }

        public NestedBeanChild setChildAlwaysUpdate(String childAlwaysUpdate) {
            this.childAlwaysUpdate = childAlwaysUpdate;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getChildWriteOnce() {
            return childWriteOnce;
        }

        public NestedBeanChild setChildWriteOnce(String childWriteOnce) {
            this.childWriteOnce = childWriteOnce;
            return this;
        }
    }

    @DynamoDbImmutable(builder = SimpleImmutableRecord.Builder.class)
    public static final class SimpleImmutableRecord {
        private final String id;
        private final String writeAlwaysField;
        private final String writeOnceField;
        private final List<SimpleImmutableChild> childList;

        private SimpleImmutableRecord(Builder b) {
            this.id = b.id;
            this.writeAlwaysField = b.writeAlwaysField;
            this.writeOnceField = b.writeOnceField;
            this.childList = b.childList;
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_ALWAYS)
        public String getWriteAlwaysField() {
            return writeAlwaysField;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getWriteOnceField() {
            return writeOnceField;
        }

        public List<SimpleImmutableChild> getChildList() {
            return childList == null ? null : Collections.unmodifiableList(childList);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String id;
            private String writeAlwaysField;
            private String writeOnceField;
            private List<SimpleImmutableChild> childList;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder writeAlwaysField(String writeAlwaysField) {
                this.writeAlwaysField = writeAlwaysField;
                return this;
            }

            public Builder writeOnceField(String writeOnceField) {
                this.writeOnceField = writeOnceField;
                return this;
            }

            public Builder childList(List<SimpleImmutableChild> childList) {
                this.childList = childList == null ? null : Collections.unmodifiableList(childList);
                return this;
            }

            public SimpleImmutableRecord build() {
                return new SimpleImmutableRecord(this);
            }
        }
    }

    @DynamoDbImmutable(builder = NestedImmutableRecord.Builder.class)
    public static final class NestedImmutableRecord {
        private final String id;
        private final String writeAlwaysField;
        private final String writeOnceField;
        private final NestedImmutableChild child;

        private NestedImmutableRecord(Builder b) {
            this.id = b.id;
            this.writeAlwaysField = b.writeAlwaysField;
            this.writeOnceField = b.writeOnceField;
            this.child = b.child;
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_ALWAYS)
        public String getWriteAlwaysField() {
            return writeAlwaysField;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getWriteOnceField() {
            return writeOnceField;
        }

        public NestedImmutableChild getChild() {
            return child;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String id;
            private String writeAlwaysField;
            private String writeOnceField;
            private NestedImmutableChild child;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder writeAlwaysField(String writeAlwaysField) {
                this.writeAlwaysField = writeAlwaysField;
                return this;
            }

            public Builder writeOnceField(String writeOnceField) {
                this.writeOnceField = writeOnceField;
                return this;
            }

            public Builder child(NestedImmutableChild child) {
                this.child = child;
                return this;
            }

            public NestedImmutableRecord build() {
                return new NestedImmutableRecord(this);
            }
        }
    }

    @DynamoDbImmutable(builder = SimpleImmutableChild.Builder.class)
    public static final class SimpleImmutableChild {
        private final String id;
        private final String childAlwaysUpdate;
        private final String childWriteOnce;

        private SimpleImmutableChild(Builder b) {
            this.id = b.id;
            this.childAlwaysUpdate = b.childAlwaysUpdate;
            this.childWriteOnce = b.childWriteOnce;
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_ALWAYS)
        public String getChildAlwaysUpdate() {
            return childAlwaysUpdate;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getChildWriteOnce() {
            return childWriteOnce;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String id;
            private String childAlwaysUpdate;
            private String childWriteOnce;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder childAlwaysUpdate(String childAlwaysUpdate) {
                this.childAlwaysUpdate = childAlwaysUpdate;
                return this;
            }

            public Builder childWriteOnce(String childWriteOnce) {
                this.childWriteOnce = childWriteOnce;
                return this;
            }

            public SimpleImmutableChild build() {
                return new SimpleImmutableChild(this);
            }
        }
    }

    @DynamoDbImmutable(builder = NestedImmutableChild.Builder.class)
    public static final class NestedImmutableChild {
        private final String childAlwaysUpdate;
        private final String childWriteOnce;

        private NestedImmutableChild(Builder b) {
            this.childAlwaysUpdate = b.childAlwaysUpdate;
            this.childWriteOnce = b.childWriteOnce;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_ALWAYS)
        public String getChildAlwaysUpdate() {
            return childAlwaysUpdate;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getChildWriteOnce() {
            return childWriteOnce;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String childAlwaysUpdate;
            private String childWriteOnce;

            public Builder childAlwaysUpdate(String childAlwaysUpdate) {
                this.childAlwaysUpdate = childAlwaysUpdate;
                return this;
            }

            public Builder childWriteOnce(String childWriteOnce) {
                this.childWriteOnce = childWriteOnce;
                return this;
            }

            public NestedImmutableChild build() {
                return new NestedImmutableChild(this);
            }
        }
    }

    public static class SimpleStaticRecord {
        private String id;
        private String writeAlwaysField;
        private String writeOnceField;

        public String getId() {
            return id;
        }

        public SimpleStaticRecord setId(String id) {
            this.id = id;
            return this;
        }

        public String getWriteAlwaysField() {
            return writeAlwaysField;
        }

        public SimpleStaticRecord setWriteAlwaysField(String writeAlwaysField) {
            this.writeAlwaysField = writeAlwaysField;
            return this;
        }

        public String getWriteOnceField() {
            return writeOnceField;
        }

        public SimpleStaticRecord setWriteOnceField(String writeOnceField) {
            this.writeOnceField = writeOnceField;
            return this;
        }
    }

    public static class NestedStaticRecord {
        private String id;
        private String writeAlwaysField;
        private String writeOnceField;
        private NestedStaticChildRecord child;

        public String getId() {
            return id;
        }

        public NestedStaticRecord setId(String id) {
            this.id = id;
            return this;
        }

        public String getWriteAlwaysField() {
            return writeAlwaysField;
        }

        public NestedStaticRecord setWriteAlwaysField(String writeAlwaysField) {
            this.writeAlwaysField = writeAlwaysField;
            return this;
        }

        public String getWriteOnceField() {
            return writeOnceField;
        }

        public NestedStaticRecord setWriteOnceField(String writeOnceField) {
            this.writeOnceField = writeOnceField;
            return this;
        }

        public NestedStaticChildRecord getChild() {
            return child;
        }

        public NestedStaticRecord setChild(NestedStaticChildRecord child) {
            this.child = child;
            return this;
        }
    }

    public static class NestedStaticChildRecord {
        private String childAlwaysUpdate;
        private String childWriteOnce;

        public String getChildAlwaysUpdate() {
            return childAlwaysUpdate;
        }

        public NestedStaticChildRecord setChildAlwaysUpdate(String childAlwaysUpdate) {
            this.childAlwaysUpdate = childAlwaysUpdate;
            return this;
        }

        public String getChildWriteOnce() {
            return childWriteOnce;
        }

        public NestedStaticChildRecord setChildWriteOnce(String childWriteOnce) {
            this.childWriteOnce = childWriteOnce;
            return this;
        }
    }

    public static TableSchema<SimpleStaticRecord> buildStaticSchemaForSimpleRecord() {
        return StaticTableSchema
            .builder(SimpleStaticRecord.class)
            .newItemSupplier(SimpleStaticRecord::new)
            .addAttribute(String.class, a -> a.name("id")
                                              .getter(SimpleStaticRecord::getId)
                                              .setter(SimpleStaticRecord::setId)
                                              .tags(primaryPartitionKey()))
            .addAttribute(String.class, a -> a.name("writeAlwaysField")
                                              .getter(SimpleStaticRecord::getWriteAlwaysField)
                                              .setter(SimpleStaticRecord::setWriteAlwaysField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_ALWAYS)))
            .addAttribute(String.class, a -> a.name("writeOnceField")
                                              .getter(SimpleStaticRecord::getWriteOnceField)
                                              .setter(SimpleStaticRecord::setWriteOnceField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
            .build();
    }

    public static TableSchema<NestedStaticRecord> buildStaticSchemaForNestedRecord() {
        TableSchema<NestedStaticChildRecord> childSchema = StaticTableSchema
            .builder(NestedStaticChildRecord.class)
            .newItemSupplier(NestedStaticChildRecord::new)
            .addAttribute(String.class, a -> a.name("childAlwaysUpdate")
                                              .getter(NestedStaticChildRecord::getChildAlwaysUpdate)
                                              .setter(NestedStaticChildRecord::setChildAlwaysUpdate)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_ALWAYS)))
            .addAttribute(String.class, a -> a.name("childWriteOnce")
                                              .getter(NestedStaticChildRecord::getChildWriteOnce)
                                              .setter(NestedStaticChildRecord::setChildWriteOnce)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
            .build();

        return StaticTableSchema
            .builder(NestedStaticRecord.class)
            .newItemSupplier(NestedStaticRecord::new)
            .addAttribute(String.class, a -> a.name("id")
                                              .getter(NestedStaticRecord::getId)
                                              .setter(NestedStaticRecord::setId)
                                              .tags(primaryPartitionKey()))
            .addAttribute(String.class, a -> a.name("writeAlwaysField")
                                              .getter(NestedStaticRecord::getWriteAlwaysField)
                                              .setter(NestedStaticRecord::setWriteAlwaysField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_ALWAYS)))
            .addAttribute(String.class, a -> a.name("writeOnceField")
                                              .getter(NestedStaticRecord::getWriteOnceField)
                                              .setter(NestedStaticRecord::setWriteOnceField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
            .addAttribute(EnhancedType.documentOf(NestedStaticChildRecord.class, childSchema),
                          a -> a.name("child")
                                .getter(NestedStaticRecord::getChild)
                                .setter(NestedStaticRecord::setChild))
            .build();
    }

    public static TableSchema<NestedStaticRecord> buildStaticSchemaForNestedRecord_NoChildSchemaDefined() {
        TableSchema<NestedStaticChildRecord> childSchema = null;

        return StaticTableSchema
            .builder(NestedStaticRecord.class)
            .newItemSupplier(NestedStaticRecord::new)
            .addAttribute(String.class, a -> a.name("id")
                                              .getter(NestedStaticRecord::getId)
                                              .setter(NestedStaticRecord::setId)
                                              .tags(primaryPartitionKey()))
            .addAttribute(String.class, a -> a.name("writeAlwaysField")
                                              .getter(NestedStaticRecord::getWriteAlwaysField)
                                              .setter(NestedStaticRecord::setWriteAlwaysField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_ALWAYS)))
            .addAttribute(String.class, a -> a.name("writeOnceField")
                                              .getter(NestedStaticRecord::getWriteOnceField)
                                              .setter(NestedStaticRecord::setWriteOnceField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
            .addAttribute(EnhancedType.documentOf(NestedStaticChildRecord.class, childSchema),
                          a -> a.name("child")
                                .getter(NestedStaticRecord::getChild)
                                .setter(NestedStaticRecord::setChild))
            .build();
    }

    public static TableSchema<SimpleImmutableRecord> buildStaticImmutableSchemaForSimpleRecord() {
        TableSchema<SimpleImmutableChild> childSchema = StaticImmutableTableSchema
            .builder(SimpleImmutableChild.class, SimpleImmutableChild.Builder.class)
            .newItemBuilder(SimpleImmutableChild::builder, SimpleImmutableChild.Builder::build)
            .addAttribute(String.class, a -> a.name("id")
                                              .getter(SimpleImmutableChild::getId)
                                              .setter(SimpleImmutableChild.Builder::id)
                                              .tags(primaryPartitionKey()))
            .addAttribute(String.class, a -> a.name(
                                                  "childAlwaysUpdate")
                                              .getter(SimpleImmutableChild::getChildAlwaysUpdate)
                                              .setter(SimpleImmutableChild.Builder::childAlwaysUpdate)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_ALWAYS)))
            .addAttribute(String.class, a -> a.name(
                                                  "childWriteOnce")
                                              .getter(SimpleImmutableChild::getChildWriteOnce)
                                              .setter(SimpleImmutableChild.Builder::childWriteOnce)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
            .build();

        return StaticImmutableTableSchema
            .builder(SimpleImmutableRecord.class, SimpleImmutableRecord.Builder.class)
            .newItemBuilder(SimpleImmutableRecord::builder, SimpleImmutableRecord.Builder::build)
            .addAttribute(String.class, a -> a.name("id")
                                              .getter(SimpleImmutableRecord::getId)
                                              .setter(SimpleImmutableRecord.Builder::id)
                                              .tags(primaryPartitionKey()))
            .addAttribute(String.class, a -> a.name("writeAlwaysField")
                                              .getter(SimpleImmutableRecord::getWriteAlwaysField)
                                              .setter(SimpleImmutableRecord.Builder::writeAlwaysField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_ALWAYS)))
            .addAttribute(String.class, a -> a.name("writeOnceField")
                                              .getter(SimpleImmutableRecord::getWriteOnceField)
                                              .setter(SimpleImmutableRecord.Builder::writeOnceField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
            .addAttribute(EnhancedType.listOf(EnhancedType.documentOf(SimpleImmutableChild.class,
                                                                      childSchema)),
                          a -> a.name("childList")
                                .getter(SimpleImmutableRecord::getChildList)
                                .setter(SimpleImmutableRecord.Builder::childList))
            .build();
    }

    public static TableSchema<NestedImmutableRecord> buildStaticImmutableSchemaForNestedRecord() {
        TableSchema<NestedImmutableChild> childSchema = StaticImmutableTableSchema
            .builder(NestedImmutableChild.class, NestedImmutableChild.Builder.class)
            .newItemBuilder(NestedImmutableChild::builder, NestedImmutableChild.Builder::build)
            .addAttribute(String.class, a -> a.name("childAlwaysUpdate")
                                              .getter(NestedImmutableChild::getChildAlwaysUpdate)
                                              .setter(NestedImmutableChild.Builder::childAlwaysUpdate)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_ALWAYS)))
            .addAttribute(String.class, a -> a.name("childWriteOnce")
                                              .getter(NestedImmutableChild::getChildWriteOnce)
                                              .setter(NestedImmutableChild.Builder::childWriteOnce)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
            .build();

        return StaticImmutableTableSchema
            .builder(NestedImmutableRecord.class, NestedImmutableRecord.Builder.class)
            .newItemBuilder(NestedImmutableRecord::builder, NestedImmutableRecord.Builder::build)
            .addAttribute(String.class, a -> a.name("id")
                                              .getter(NestedImmutableRecord::getId)
                                              .setter(NestedImmutableRecord.Builder::id)
                                              .tags(primaryPartitionKey()))
            .addAttribute(String.class, a -> a.name("writeAlwaysField")
                                              .getter(NestedImmutableRecord::getWriteAlwaysField)
                                              .setter(NestedImmutableRecord.Builder::writeAlwaysField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_ALWAYS)))
            .addAttribute(String.class, a -> a.name("writeOnceField")
                                              .getter(NestedImmutableRecord::getWriteOnceField)
                                              .setter(NestedImmutableRecord.Builder::writeOnceField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
            .addAttribute(EnhancedType.documentOf(NestedImmutableChild.class, childSchema),
                          a -> a.name("child")
                                .getter(NestedImmutableRecord::getChild)
                                .setter(NestedImmutableRecord.Builder::child))
            .build();
    }

    public static TableSchema<NestedImmutableRecord> buildStaticImmutableSchema_NoChildSchemaDefined() {
        TableSchema<NestedImmutableChild> childSchema = null;

        return StaticImmutableTableSchema
            .builder(NestedImmutableRecord.class, NestedImmutableRecord.Builder.class)
            .newItemBuilder(NestedImmutableRecord::builder, NestedImmutableRecord.Builder::build)
            .addAttribute(String.class, a -> a.name("id")
                                              .getter(NestedImmutableRecord::getId)
                                              .setter(NestedImmutableRecord.Builder::id)
                                              .tags(primaryPartitionKey()))
            .addAttribute(String.class, a -> a.name("writeAlwaysField")
                                              .getter(NestedImmutableRecord::getWriteAlwaysField)
                                              .setter(NestedImmutableRecord.Builder::writeAlwaysField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_ALWAYS)))
            .addAttribute(String.class, a -> a.name("writeOnceField")
                                              .getter(NestedImmutableRecord::getWriteOnceField)
                                              .setter(NestedImmutableRecord.Builder::writeOnceField)
                                              .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
            .addAttribute(EnhancedType.documentOf(NestedImmutableChild.class, childSchema),
                          a -> a.name("child")
                                .getter(NestedImmutableRecord::getChild)
                                .setter(NestedImmutableRecord.Builder::child))
            .build();
    }

    /**
     * Creates a SimpleBean with initial values for testing update behavior.
     */
    public static SimpleBean createSimpleBean() {
        return new SimpleBean()
            .setId("1")
            .setWriteAlwaysField("initial_writeAlways")
            .setWriteOnceField("initial_writeOnce")
            .setChildList(Arrays.asList(
                new SimpleBeanChild().setId("child1")
                                     .setChildAlwaysUpdate("child1_initial_writeAlways")
                                     .setChildWriteOnce("child1_initial_writeOnce"),
                new SimpleBeanChild().setId("child2")
                                     .setChildAlwaysUpdate("child2_initial_writeAlways")
                                     .setChildWriteOnce("child2_initial_writeOnce")));
    }

    /**
     * Creates a NestedBean with initial values for testing update behavior.
     */
    public static NestedBean createNestedBean() {
        return new NestedBean()
            .setId("1")
            .setWriteAlwaysField("initial_writeAlways")
            .setWriteOnceField("initial_writeOnce")
            .setChild(new NestedBeanChild()
                           .setChildAlwaysUpdate("child_initial_writeAlways")
                           .setChildWriteOnce("child_initial_writeOnce"));
    }

    /**
     * Creates a SimpleImmutableRecord with initial values for testing update behavior.
     */
    public static SimpleImmutableRecord createSimpleImmutableRecord() {
        return SimpleImmutableRecord
            .builder()
            .id("1")
            .writeAlwaysField("initial_writeAlways")
            .writeOnceField("initial_writeOnce")
            .childList(Arrays.asList(
                SimpleImmutableChild.builder()
                    .id("child1")
                    .childAlwaysUpdate("child1_initial_writeAlways")
                    .childWriteOnce("child1_initial_writeOnce")
                    .build(),
                SimpleImmutableChild.builder()
                    .id("child2")
                    .childAlwaysUpdate("child2_initial_writeAlways")
                    .childWriteOnce("child2_initial_writeOnce")
                    .build()))
            .build();
    }

    /**
     * Creates a NestedImmutableRecord with initial values for testing update behavior.
     */
    public static NestedImmutableRecord createNestedImmutableRecord() {
        return NestedImmutableRecord
            .builder()
            .id("1")
            .writeAlwaysField("initial_writeAlways")
            .writeOnceField("initial_writeOnce")
            .child(NestedImmutableChild
                .builder()
                .childAlwaysUpdate("child_initial_writeAlways")
                .childWriteOnce("child_initial_writeOnce")
                .build())
            .build();
    }

    /**
     * Creates a SimpleStaticRecord with initial values for testing update behavior.
     */
    public static SimpleStaticRecord createSimpleStaticRecord() {
        return new SimpleStaticRecord()
            .setId("1")
            .setWriteAlwaysField("initial_writeAlways")
            .setWriteOnceField("initial_writeOnce");
    }

    /**
     * Creates a NestedStaticRecord with initial values for testing update behavior.
     */
    public static NestedStaticRecord createNestedStaticRecord() {
        return new NestedStaticRecord()
            .setId("1")
            .setWriteAlwaysField("initial_writeAlways")
            .setWriteOnceField("initial_writeOnce")
            .setChild(new NestedStaticChildRecord()
                .setChildAlwaysUpdate("child_initial_writeAlways")
                .setChildWriteOnce("child_initial_writeOnce"));
    }

    /**
     * Test model with an invalid root-level attribute name containing the reserved '_NESTED_ATTR_UPDATE_' pattern. Used to test
     * validation of attribute names that conflict with internal DynamoDB Enhanced Client conventions.
     */
    @DynamoDbBean
    public static class BeanWithInvalidRootAttributeName {
        private String id;
        private Instant attr_NESTED_ATTR_UPDATE_;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public BeanWithInvalidRootAttributeName setId(String id) {
            this.id = id;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public Instant getAttr_NESTED_ATTR_UPDATE_() {
            return attr_NESTED_ATTR_UPDATE_;
        }

        public BeanWithInvalidRootAttributeName setAttr_NESTED_ATTR_UPDATE_(Instant attr_NESTED_ATTR_UPDATE_) {
            this.attr_NESTED_ATTR_UPDATE_ = attr_NESTED_ATTR_UPDATE_;
            return this;
        }
    }

    /**
     * Test model with an invalid nested attribute name containing the reserved '_NESTED_ATTR_UPDATE_' pattern. Used to test
     * validation of nested attribute names that conflict with internal DynamoDB Enhanced Client conventions.
     */
    @DynamoDbBean
    public static class BeanWithInvalidNestedAttributeName {
        private String id;
        private ChildBeanWithInvalidAttributeName child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public BeanWithInvalidNestedAttributeName setId(String id) {
            this.id = id;
            return this;
        }

        public ChildBeanWithInvalidAttributeName getChild() {
            return child;
        }

        public BeanWithInvalidNestedAttributeName setChild(ChildBeanWithInvalidAttributeName child) {
            this.child = child;
            return this;
        }

        @DynamoDbBean
        public static class ChildBeanWithInvalidAttributeName {
            private String id;
            private Instant childAttr_NESTED_ATTR_UPDATE_;

            @DynamoDbPartitionKey
            public String getId() {
                return id;
            }

            public ChildBeanWithInvalidAttributeName setId(String id) {
                this.id = id;
                return this;
            }

            @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
            public Instant getChildAttr_NESTED_ATTR_UPDATE_() {
                return childAttr_NESTED_ATTR_UPDATE_;
            }

            public ChildBeanWithInvalidAttributeName setChildAttr_NESTED_ATTR_UPDATE_(Instant childAttr_NESTED_ATTR_UPDATE_) {
                this.childAttr_NESTED_ATTR_UPDATE_ = childAttr_NESTED_ATTR_UPDATE_;
                return this;
            }
        }
    }
}