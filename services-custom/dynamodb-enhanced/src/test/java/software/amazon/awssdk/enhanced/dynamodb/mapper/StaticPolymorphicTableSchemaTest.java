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
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class StaticPolymorphicTableSchemaTest {

    @SuppressWarnings("rawtypes")
    private static final StaticImmutableTableSchema<Animal, Animal.Builder> ROOT_ANIMAL_TABLE_SCHEMA =
        StaticImmutableTableSchema.builder(Animal.class, Animal.Builder.class)
                                  .addAttribute(String.class,
                                                a -> a.name("id")
                                                      .getter(Animal::id)
                                                      .setter(Animal.Builder::id)
                                                      .tags(primaryPartitionKey()))
                                  .addAttribute(String.class,
                                                a -> a.name("species")
                                                      .getter(Animal::species)
                                                      .setter(Animal.Builder::species))
                                  .build();

    private static final TableSchema<Cat> CAT_TABLE_SCHEMA =
        StaticImmutableTableSchema.builder(Cat.class, Cat.Builder.class)
                                  .addAttribute(String.class,
                                                a -> a.name("breed")
                                                      .getter(Cat::breed)
                                                      .setter(Cat.Builder::breed))
                                  .newItemBuilder(Cat::builder, Cat.Builder::build)
                                  .extend(ROOT_ANIMAL_TABLE_SCHEMA)
                                  .build();

    private static final TableSchema<Snake> SNAKE_TABLE_SCHEMA =
        StaticImmutableTableSchema.builder(Snake.class, Snake.Builder.class)
                                  .addAttribute(Boolean.class,
                                                a -> a.name("isVenomous")
                                                      .getter(Snake::isVenomous)
                                                      .setter(Snake.Builder::isVenomous))
                                  .newItemBuilder(Snake::builder, Snake.Builder::build)
                                  .extend(ROOT_ANIMAL_TABLE_SCHEMA)
                                  .build();

    private static final TableSchema<Animal> ANIMAL_TABLE_SCHEMA =
        StaticPolymorphicTableSchema.builder(Animal.class)
                                    .rootTableSchema(ROOT_ANIMAL_TABLE_SCHEMA)
                                    .discriminatorAttributeName("species")
                                    .addStaticSubtype(
                                        StaticSubtype.builder(Cat.class).name("CAT").tableSchema(CAT_TABLE_SCHEMA).build(),
                                        StaticSubtype.builder(Snake.class).name("SNAKE").tableSchema(SNAKE_TABLE_SCHEMA).build())
                                    .build();

    private static final Cat CAT =
        Cat.builder().id("cat:1").species("CAT").breed("persian").build();
    private static final Snake SNAKE =
        Snake.builder().id("snake:1").species("SNAKE").isVenomous(true).build();

    private static final Map<String, AttributeValue> CAT_MAP;
    private static final Map<String, AttributeValue> SNAKE_MAP;

    static {
        Map<String, AttributeValue> catMap = new HashMap<>();
        catMap.put("id", AttributeValue.builder().s("cat:1").build());
        catMap.put("species", AttributeValue.builder().s("CAT").build());
        catMap.put("breed", AttributeValue.builder().s("persian").build());
        CAT_MAP = Collections.unmodifiableMap(catMap);

        Map<String, AttributeValue> snakeMap = new HashMap<>();
        snakeMap.put("id", AttributeValue.builder().s("snake:1").build());
        snakeMap.put("species", AttributeValue.builder().s("SNAKE").build());
        snakeMap.put("isVenomous", AttributeValue.builder().bool(true).build());
        SNAKE_MAP = Collections.unmodifiableMap(snakeMap);
    }

    @Test
    public void shouldThrowWhenNoSubtypes() {
        assertThatThrownBy(() -> StaticPolymorphicTableSchema.builder(Animal.class)
                                                             .rootTableSchema(ROOT_ANIMAL_TABLE_SCHEMA)
                                                             .discriminatorAttributeName("species")
                                                             .addStaticSubtype()
                                                             .build()
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A polymorphic TableSchema must have at least one subtype");
    }

    @Test
    public void shouldThrowWhenNoRootSchema() {
        assertThatThrownBy(() -> StaticPolymorphicTableSchema.builder(Animal.class)
                                                             .discriminatorAttributeName("species")
                                                             .addStaticSubtype(
                                                                 StaticSubtype.builder(Cat.class)
                                                                              .name("CAT")
                                                                              .tableSchema(CAT_TABLE_SCHEMA)
                                                                              .build()
                                                             )
                                                             .build()
        )
            .isInstanceOf(NullPointerException.class)
            .hasMessage("rootTableSchema must not be null.");
    }

    @Test
    public void shouldThrowOnDuplicateNames() {
        assertThatThrownBy(() -> StaticPolymorphicTableSchema.builder(Animal.class)
                                                             .rootTableSchema(ROOT_ANIMAL_TABLE_SCHEMA)
                                                             .discriminatorAttributeName("species")
                                                             .addStaticSubtype(
                                                                 StaticSubtype.builder(Cat.class).name("CAT").tableSchema(CAT_TABLE_SCHEMA).build(),
                                                                 StaticSubtype.builder(Snake.class).name("CAT").tableSchema(SNAKE_TABLE_SCHEMA).build()
                                                             )
                                                             .build()
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Duplicate subtype names are not permitted. [name = \"CAT\"]");
    }

    @Test
    public void shouldSerializeToMap() {
        assertThat(ANIMAL_TABLE_SCHEMA.itemToMap(CAT, false)).isEqualTo(CAT_MAP);
        assertThat(ANIMAL_TABLE_SCHEMA.itemToMap(SNAKE, false)).isEqualTo(SNAKE_MAP);
        // discriminator is injected even when ignoreNulls == true
        assertThat(ANIMAL_TABLE_SCHEMA.itemToMap(CAT, true)).isEqualTo(CAT_MAP);
        assertThat(ANIMAL_TABLE_SCHEMA.itemToMap(SNAKE, true)).isEqualTo(SNAKE_MAP);
    }

    @Test
    public void shouldSerializePartialAttributes() {
        Map<String, AttributeValue> result =
            ANIMAL_TABLE_SCHEMA.itemToMap(CAT, Arrays.asList("id", "breed"));
        assertThat(result)
            .containsOnlyKeys("id", "breed")
            .containsEntry("id", AttributeValue.builder().s("cat:1").build())
            .containsEntry("breed", AttributeValue.builder().s("persian").build());
    }

    @Test
    public void shouldDeserializeFromMap() {
        assertThat(ANIMAL_TABLE_SCHEMA.mapToItem(CAT_MAP)).isEqualTo(CAT);
        assertThat(ANIMAL_TABLE_SCHEMA.mapToItem(SNAKE_MAP)).isEqualTo(SNAKE);
    }

    @Test
    public void shouldResolveSubtypeCollection() {
        StaticSubtype<? extends Animal>[] subs = new StaticSubtype[] {
            StaticSubtype.builder(Cat.class).name("CAT").tableSchema(CAT_TABLE_SCHEMA).build(),
            StaticSubtype.builder(Snake.class).name("SNAKE").tableSchema(SNAKE_TABLE_SCHEMA).build()};
        TableSchema<Animal> schema = StaticPolymorphicTableSchema.builder(Animal.class)
                                                                 .rootTableSchema(ROOT_ANIMAL_TABLE_SCHEMA)
                                                                 .discriminatorAttributeName("species")
                                                                 .addStaticSubtype(subs)
                                                                 .build();
        assertThat(schema.mapToItem(CAT_MAP)).isEqualTo(CAT);
        assertThat(schema.mapToItem(SNAKE_MAP)).isEqualTo(SNAKE);
    }

    @Test
    public void shouldReturnCorrectAttributeValue() {
        assertThat(ANIMAL_TABLE_SCHEMA.attributeValue(CAT, "breed"))
            .isEqualTo(AttributeValue.builder().s("persian").build());
    }

    @Test
    public void metadataAndTypeChecks() {
        assertThat(ANIMAL_TABLE_SCHEMA.itemType().rawClass()).isEqualTo(Animal.class);
        assertThat(ANIMAL_TABLE_SCHEMA.attributeNames()).containsExactlyInAnyOrder("id", "species");
        assertThat(ANIMAL_TABLE_SCHEMA.isAbstract()).isFalse();
    }

    @Test
    public void polymorphicTableSchemaShouldTakeTheMetadataFromTheRootTableSchema() {
        assertThat(ANIMAL_TABLE_SCHEMA.tableMetadata()).isEqualTo(ROOT_ANIMAL_TABLE_SCHEMA.tableMetadata());
    }

    private static class Animal {
        private final String id;
        private final String species;

        protected Animal(Builder<?> b) {
            this.id = b.id;
            this.species = b.species;
        }

        public String id() {
            return id;
        }

        public String species() {
            return species;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Animal)) {
                return false;
            }
            Animal that = (Animal) o;
            return Objects.equals(id, that.id) &&
                   Objects.equals(species, that.species);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, species);
        }

        public static class Builder<T extends Builder<T>> {
            private String id;
            private String species;

            protected Builder() {
            }

            public T id(String id) {
                this.id = id;
                return (T) this;
            }

            public T species(String sp) {
                this.species = sp;
                return (T) this;
            }
        }
    }

    private static class Cat extends Animal {
        private final String breed;

        private Cat(Builder b) {
            super(b);
            this.breed = b.breed;
        }

        public String breed() {
            return breed;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends Animal.Builder<Builder> {
            private String breed;

            public Builder breed(String b) {
                this.breed = b;
                return this;
            }

            public Cat build() {
                return new Cat(this);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) {
                return false;
            }
            Cat that = (Cat) o;
            return Objects.equals(breed, that.breed);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), breed);
        }
    }

    private static class Snake extends Animal {
        private final Boolean isVenomous;

        private Snake(Builder b) {
            super(b);
            this.isVenomous = b.isVenomous;
        }

        public Boolean isVenomous() {
            return isVenomous;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends Animal.Builder<Builder> {
            private Boolean isVenomous;

            public Builder isVenomous(Boolean v) {
                this.isVenomous = v;
                return this;
            }

            public Snake build() {
                return new Snake(this);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) {
                return false;
            }
            Snake that = (Snake) o;
            return Objects.equals(isVenomous, that.isVenomous);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), isVenomous);
        }
    }
}
