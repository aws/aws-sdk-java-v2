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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
 * Test models specifically designed for update behavior functionality testing. These models focus on the "attr" attribute
 * annotated with @DynamoDbUpdateBehavior annotation and are used by NestedUpdateBehaviorTest.
 */
public final class UpdateBehaviorTestModels {

    private UpdateBehaviorTestModels() {
    }

    @DynamoDbBean
    public static class SimpleBeanWithList {
        private String id;
        private String attr;
        private List<SimpleBeanChild> childList;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public SimpleBeanWithList setId(String id) {
            this.id = id;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getAttr() {
            return attr;
        }

        public SimpleBeanWithList setAttr(String attr) {
            this.attr = attr;
            return this;
        }

        public List<SimpleBeanChild> getChildList() {
            return childList;
        }

        public SimpleBeanWithList setChildList(List<SimpleBeanChild> childList) {
            this.childList = childList;
            return this;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof SimpleBeanWithList)) {
                return false;
            }

            SimpleBeanWithList that = (SimpleBeanWithList) o;
            return Objects.equals(id, that.id) && Objects.equals(attr, that.attr) && Objects.equals(childList, that.childList);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(id);
            result = 31 * result + Objects.hashCode(attr);
            result = 31 * result + Objects.hashCode(childList);
            return result;
        }
    }

    @DynamoDbBean
    public static class SimpleBeanChild {
        private String id;
        private String attr;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public SimpleBeanChild setId(String id) {
            this.id = id;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getAttr() {
            return attr;
        }

        public SimpleBeanChild setAttr(String attr) {
            this.attr = attr;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SimpleBeanChild that = (SimpleBeanChild) o;
            return Objects.equals(id, that.id) && Objects.equals(attr, that.attr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attr);
        }
    }

    @DynamoDbBean
    public static class NestedBeanWithList {
        private String id;
        private String attr;
        private NestedBeanChild level2;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public NestedBeanWithList setId(String id) {
            this.id = id;
            return this;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getAttr() {
            return attr;
        }

        public NestedBeanWithList setAttr(String attr) {
            this.attr = attr;
            return this;
        }

        public NestedBeanChild getLevel2() {
            return level2;
        }

        public NestedBeanWithList setLevel2(NestedBeanChild level2) {
            this.level2 = level2;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NestedBeanWithList that = (NestedBeanWithList) o;
            return Objects.equals(id, that.id) && Objects.equals(attr, that.attr)
                   && Objects.equals(level2, that.level2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attr, level2);
        }
    }

    @DynamoDbBean
    public static class NestedBeanChild {
        private String attr;

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getAttr() {
            return attr;
        }

        public NestedBeanChild setAttr(String attr) {
            this.attr = attr;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NestedBeanChild that = (NestedBeanChild) o;
            return Objects.equals(attr, that.attr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attr
            );
        }
    }

    @DynamoDbImmutable(builder = SimpleImmutableRecordWithList.Builder.class)
    public static final class SimpleImmutableRecordWithList {
        private final String id;
        private final String attr;
        private final List<SimpleImmutableChild> childList;

        private SimpleImmutableRecordWithList(Builder b) {
            this.id = b.id;
            this.attr = b.attr;
            this.childList = b.childList;
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getAttr() {
            return attr;
        }

        public List<SimpleImmutableChild> getChildList() {
            return childList == null ? null : Collections.unmodifiableList(childList);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String id;
            private String attr;
            private List<SimpleImmutableChild> childList;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder attr(String attr) {
                this.attr = attr;
                return this;
            }

            public Builder childList(List<SimpleImmutableChild> childList) {
                this.childList = childList;
                return this;
            }

            public SimpleImmutableRecordWithList build() {
                return new SimpleImmutableRecordWithList(this);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SimpleImmutableRecordWithList that = (SimpleImmutableRecordWithList) o;
            return Objects.equals(id, that.id) && Objects.equals(attr, that.attr)
                   && Objects.equals(childList, that.childList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attr, childList);
        }
    }

    @DynamoDbImmutable(builder = SimpleImmutableChild.Builder.class)
    public static final class SimpleImmutableChild {
        private final String id;
        private final String attr;

        private SimpleImmutableChild(Builder b) {
            this.id = b.id;
            this.attr = b.attr;
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getAttr() {
            return attr;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String id;
            private String attr;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder attr(String attr) {
                this.attr = attr;
                return this;
            }

            public SimpleImmutableChild build() {
                return new SimpleImmutableChild(this);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SimpleImmutableChild that = (SimpleImmutableChild) o;
            return Objects.equals(id, that.id) && Objects.equals(attr, that.attr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attr);
        }
    }

    @DynamoDbImmutable(builder = NestedImmutableRecordWithList.Builder.class)
    public static final class NestedImmutableRecordWithList {
        private final String id;
        private final String attr;
        private final NestedImmutableChildRecordWithList level2;

        private NestedImmutableRecordWithList(Builder b) {
            this.id = b.id;
            this.attr = b.attr;

            this.level2 = b.level2;
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getAttr() {
            return attr;
        }

        public NestedImmutableChildRecordWithList getLevel2() {
            return level2;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String id;
            private String attr;
            private NestedImmutableChildRecordWithList level2;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder attr(String attr) {
                this.attr = attr;
                return this;
            }

            public Builder level2(NestedImmutableChildRecordWithList level2) {
                this.level2 = level2;
                return this;
            }

            public NestedImmutableRecordWithList build() {
                return new NestedImmutableRecordWithList(this);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NestedImmutableRecordWithList that = (NestedImmutableRecordWithList) o;
            return Objects.equals(id, that.id) && Objects.equals(attr, that.attr)
                   && Objects.equals(level2, that.level2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attr, level2);
        }
    }

    @DynamoDbImmutable(builder = NestedImmutableChildRecordWithList.Builder.class)
    public static final class NestedImmutableChildRecordWithList {
        private final String attr;

        private NestedImmutableChildRecordWithList(Builder b) {
            this.attr = b.attr;

        }

        @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
        public String getAttr() {
            return attr;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String attr;

            public Builder attr(String attr) {
                this.attr = attr;
                return this;
            }

            public NestedImmutableChildRecordWithList build() {
                return new NestedImmutableChildRecordWithList(this);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NestedImmutableChildRecordWithList that = (NestedImmutableChildRecordWithList) o;
            return Objects.equals(attr, that.attr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attr);
        }
    }

    public static class SimpleStaticRecordWithList {
        private String id;
        private String attr;

        public String getId() {
            return id;
        }

        public SimpleStaticRecordWithList setId(String id) {
            this.id = id;
            return this;
        }

        public String getAttr() {
            return attr;
        }

        public SimpleStaticRecordWithList setAttr(String attr) {
            this.attr = attr;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SimpleStaticRecordWithList that = (SimpleStaticRecordWithList) o;
            return Objects.equals(id, that.id) && Objects.equals(attr, that.attr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attr);
        }
    }

    public static class NestedStaticRecordWithList {
        private String id;
        private String attr;
        private NestedStaticChildRecordWithList level2;

        public String getId() {
            return id;
        }

        public NestedStaticRecordWithList setId(String id) {
            this.id = id;
            return this;
        }

        public String getAttr() {
            return attr;
        }

        public NestedStaticRecordWithList setAttr(String attr) {
            this.attr = attr;
            return this;
        }

        public NestedStaticChildRecordWithList getLevel2() {
            return level2;
        }

        public NestedStaticRecordWithList setLevel2(NestedStaticChildRecordWithList level2) {
            this.level2 = level2;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NestedStaticRecordWithList that = (NestedStaticRecordWithList) o;
            return Objects.equals(id, that.id) && Objects.equals(attr, that.attr)
                   && Objects.equals(level2, that.level2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attr, level2);
        }
    }

    public static class NestedStaticChildRecordWithList {
        private String attr;

        public String getAttr() {
            return attr;
        }

        public NestedStaticChildRecordWithList setAttr(String attr) {
            this.attr = attr;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NestedStaticChildRecordWithList that = (NestedStaticChildRecordWithList) o;
            return Objects.equals(attr, that.attr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attr);
        }
    }

    public static TableSchema<SimpleStaticRecordWithList> buildStaticSchemaForSimpleRecordWithList() {
        return StaticTableSchema.builder(SimpleStaticRecordWithList.class)
                                .newItemSupplier(SimpleStaticRecordWithList::new)
                                .addAttribute(String.class, a -> a.name("id")
                                                                  .getter(SimpleStaticRecordWithList::getId)
                                                                  .setter(SimpleStaticRecordWithList::setId)
                                                                  .tags(primaryPartitionKey()))
                                .addAttribute(String.class, a -> a.name("attr")
                                                                  .getter(SimpleStaticRecordWithList::getAttr)
                                                                  .setter(SimpleStaticRecordWithList::setAttr)
                                                                  .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                                .build();
    }

    public static TableSchema<NestedStaticRecordWithList> buildStaticSchemaForNestedRecordWithList() {
        TableSchema<NestedStaticChildRecordWithList> level2Schema =
            StaticTableSchema.builder(NestedStaticChildRecordWithList.class)
                             .newItemSupplier(NestedStaticChildRecordWithList::new)
                             .addAttribute(String.class, a -> a.name("attr")
                                                               .getter(NestedStaticChildRecordWithList::getAttr)
                                                               .setter(NestedStaticChildRecordWithList::setAttr)
                                                               .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                             .build();

        return StaticTableSchema.builder(NestedStaticRecordWithList.class)
                                .newItemSupplier(NestedStaticRecordWithList::new)
                                .addAttribute(String.class, a -> a.name("id")
                                                                  .getter(NestedStaticRecordWithList::getId)
                                                                  .setter(NestedStaticRecordWithList::setId)
                                                                  .tags(primaryPartitionKey()))
                                .addAttribute(String.class, a -> a.name("attr")
                                                                  .getter(NestedStaticRecordWithList::getAttr)
                                                                  .setter(NestedStaticRecordWithList::setAttr)
                                                                  .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                                .addAttribute(EnhancedType.documentOf(NestedStaticChildRecordWithList.class, level2Schema),
                                              a -> a.name("level2")
                                                    .getter(NestedStaticRecordWithList::getLevel2)
                                                    .setter(NestedStaticRecordWithList::setLevel2))
                                .build();
    }

    public static TableSchema<SimpleImmutableRecordWithList> buildStaticImmutableSchemaForSimpleRecordWithList() {
        TableSchema<SimpleImmutableChild> childSchema =
            StaticImmutableTableSchema.builder(SimpleImmutableChild.class,
                                               SimpleImmutableChild.Builder.class)
                                      .newItemBuilder(SimpleImmutableChild::builder,
                                                      SimpleImmutableChild.Builder::build)
                                      .addAttribute(String.class, a -> a.name("id")
                                                                        .getter(SimpleImmutableChild::getId)
                                                                        .setter(SimpleImmutableChild.Builder::id)
                                                                        .tags(primaryPartitionKey()))
                                      .addAttribute(String.class, a -> a.name("attr")
                                                                        .getter(SimpleImmutableChild::getAttr)
                                                                        .setter(SimpleImmutableChild.Builder::attr)
                                                                        .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                                      .build();

        return StaticImmutableTableSchema.builder(SimpleImmutableRecordWithList.class,
                                                  SimpleImmutableRecordWithList.Builder.class)
                                         .newItemBuilder(SimpleImmutableRecordWithList::builder,
                                                         SimpleImmutableRecordWithList.Builder::build)
                                         .addAttribute(String.class, a -> a.name("id")
                                                                           .getter(SimpleImmutableRecordWithList::getId)
                                                                           .setter(SimpleImmutableRecordWithList.Builder::id)
                                                                           .tags(primaryPartitionKey()))
                                         .addAttribute(String.class, a -> a.name("attr")
                                                                           .getter(SimpleImmutableRecordWithList::getAttr)
                                                                           .setter(SimpleImmutableRecordWithList.Builder::attr)
                                                                           .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                                         .addAttribute(EnhancedType.listOf(EnhancedType.documentOf(SimpleImmutableChild.class,
                                                                                                   childSchema)),
                                                       a -> a.name("childList")
                                                             .getter(SimpleImmutableRecordWithList::getChildList)
                                                             .setter(SimpleImmutableRecordWithList.Builder::childList))
                                         .build();
    }

    public static TableSchema<NestedImmutableRecordWithList> buildStaticImmutableSchemaForNestedRecordWithList() {
        TableSchema<NestedImmutableChildRecordWithList> level2Schema =
            StaticImmutableTableSchema.builder(NestedImmutableChildRecordWithList.class,
                                               NestedImmutableChildRecordWithList.Builder.class)
                                      .newItemBuilder(NestedImmutableChildRecordWithList::builder,
                                                      NestedImmutableChildRecordWithList.Builder::build)
                                      .addAttribute(String.class, a -> a.name("attr")
                                                                        .getter(NestedImmutableChildRecordWithList::getAttr)
                                                                        .setter(NestedImmutableChildRecordWithList.Builder::attr)
                                                                        .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                                      .build();

        return StaticImmutableTableSchema.builder(NestedImmutableRecordWithList.class,
                                                  NestedImmutableRecordWithList.Builder.class)
                                         .newItemBuilder(NestedImmutableRecordWithList::builder,
                                                         NestedImmutableRecordWithList.Builder::build)
                                         .addAttribute(String.class, a -> a.name("id")
                                                                           .getter(NestedImmutableRecordWithList::getId)
                                                                           .setter(NestedImmutableRecordWithList.Builder::id)
                                                                           .tags(primaryPartitionKey()))
                                         .addAttribute(String.class, a -> a.name("attr")
                                                                           .getter(NestedImmutableRecordWithList::getAttr)
                                                                           .setter(NestedImmutableRecordWithList.Builder::attr)
                                                                           .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                                         .addAttribute(EnhancedType.documentOf(NestedImmutableChildRecordWithList.class,
                                                                               level2Schema),
                                                       a -> a.name("level2")
                                                             .getter(NestedImmutableRecordWithList::getLevel2)
                                                             .setter(NestedImmutableRecordWithList.Builder::level2))
                                         .build();
    }
}