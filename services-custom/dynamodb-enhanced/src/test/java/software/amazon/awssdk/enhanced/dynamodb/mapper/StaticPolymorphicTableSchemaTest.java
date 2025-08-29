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
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.subtypeName;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
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
                                                      .setter(Animal.Builder::species)
                                                      .tags(subtypeName()))
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
                                    .staticSubtypes(StaticSubtype.builder(Cat.class).name("CAT").tableSchema(CAT_TABLE_SCHEMA).build(),
                                                    StaticSubtype.builder(Snake.class).name("SNAKE").tableSchema(SNAKE_TABLE_SCHEMA).build())
                                    .build();

    private static final Cat CAT = Cat.builder().id("cat:1").species("CAT").breed("persian").build();
    private static final Snake SNAKE = Snake.builder().id("snake:1").species("SNAKE").isVenomous(true).build();

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
    public void shouldThrowExceptionWhenBuildingPolymorphicTableSchema_givenNoSubtypes() {
        assertThatThrownBy(() -> StaticPolymorphicTableSchema.builder(Animal.class)
                                                             .rootTableSchema(ROOT_ANIMAL_TABLE_SCHEMA)
                                                             .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessage("A polymorphic TableSchema must have at least one associated subtype");
    }

    @Test
    public void shouldThrowExceptionWhenBuildingPolymorphicTableSchema_givenNoRootTableSchema() {
        assertThatThrownBy(() -> StaticPolymorphicTableSchema.builder(Animal.class)
                                                             .staticSubtypes(StaticSubtype.builder(Cat.class)
                                                                                          .name("CAT")
                                                                                          .tableSchema(CAT_TABLE_SCHEMA)
                                                                                          .build(),
                                                                             StaticSubtype.builder(Snake.class)
                                                                                          .name("SNAKE")
                                                                                          .tableSchema(SNAKE_TABLE_SCHEMA)
                                                                                          .build())
                                                             .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessage("rootTableSchema must not be null.");

    }

    @Test
    public void shouldThrowExceptionWhenBuildingPolymorphicTableSchema_givenDuplicateSubtypeName() {
        assertThatThrownBy(() -> StaticPolymorphicTableSchema.builder(Animal.class)
                                                             .rootTableSchema(ROOT_ANIMAL_TABLE_SCHEMA)
                                                             .staticSubtypes(StaticSubtype.builder(Cat.class)
                                                                                          .name("CAT")
                                                                                          .tableSchema(CAT_TABLE_SCHEMA)
                                                                                          .build(),
                                                                             StaticSubtype.builder(Snake.class)
                                                                                          .name("CAT")
                                                                                          .tableSchema(SNAKE_TABLE_SCHEMA)
                                                                                          .build())
                                                             .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Duplicate subtype names are not permitted. [name = \"CAT\"]");

    }

    @Test
    public void shouldSerializePolymorphicObjectToAttributesMap() {
        assertThat(ANIMAL_TABLE_SCHEMA.itemToMap(CAT, false)).isEqualTo(CAT_MAP);
        assertThat(ANIMAL_TABLE_SCHEMA.itemToMap(SNAKE, false)).isEqualTo(SNAKE_MAP);
        assertThat(ANIMAL_TABLE_SCHEMA.itemToMap(CAT, true)).isEqualTo(CAT_MAP);
        assertThat(ANIMAL_TABLE_SCHEMA.itemToMap(SNAKE, true)).isEqualTo(SNAKE_MAP);
    }

    @Test
    public void shouldSerializePolymorphicObjectToSpecificAttributesMap_givenListOfAttributes() {
        Map<String, AttributeValue> result = ANIMAL_TABLE_SCHEMA.itemToMap(CAT, Arrays.asList("id", "breed"));

        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("id", AttributeValue.builder().s("cat:1").build());
        assertThat(result).containsEntry("breed", AttributeValue.builder().s("persian").build());
    }

    @Test
    public void shouldThrowCastException_whenSerializingPolymorphicObjectWithMismatchedType() {
        Cat cat = Cat.builder().id("cat:1").species("SNAKE").breed("persian").build();

        assertThatThrownBy(() -> ANIMAL_TABLE_SCHEMA.itemToMap(cat, false))
            .isInstanceOf(ClassCastException.class)
            .hasMessage("Cannot cast software.amazon.awssdk.enhanced.dynamodb.mapper.StaticPolymorphicTableSchemaTest$Cat to "
                        + "software.amazon.awssdk.enhanced.dynamodb.mapper.StaticPolymorphicTableSchemaTest$Snake");
    }

    @Test
    public void shouldThrowExceptionWhenSerializingPolymorphicObject_GivenInvalidDiscriminatorValue() {
        Cat cat = Cat.builder().id("cat:1").species("DOG").breed("persian").build();

        assertThatThrownBy(() -> ANIMAL_TABLE_SCHEMA.itemToMap(cat, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The subtype name 'DOG' could not be matched to any declared subtypes of the polymorphic table schema.");
    }

    @Test
    public void shouldThrowExceptionWhenSerializingPolymorphicObject_GivenNullDiscriminatorValue() {
        Cat cat = Cat.builder().id("cat:1").breed("persian").build();

        assertThatThrownBy(() -> ANIMAL_TABLE_SCHEMA.itemToMap(cat, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The subtype name could not be read from the item, either because it is missing "
                        + "or because it is not a string.");
    }

    @Test
    public void shouldThrowExceptionWhenSerializingPolymorphicObject_GivenEmptyDiscriminatorValue() {
        Cat cat = Cat.builder().id("cat:1").species("").breed("persian").build();

        assertThatThrownBy(() -> ANIMAL_TABLE_SCHEMA.itemToMap(cat, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The subtype name could not be read from the item, either because it is missing "
                        + "or because it is not a string.");
    }

    @Test
    public void shouldDeserializeDatabaseRecordsToPolymorphicObjects() {
        assertThat(ANIMAL_TABLE_SCHEMA.mapToItem(CAT_MAP)).isEqualTo(CAT);
        assertThat(ANIMAL_TABLE_SCHEMA.mapToItem(SNAKE_MAP)).isEqualTo(SNAKE);
    }

    @Test
    public void shouldDeserializeDatabaseRecordsToPolymorphicObjects_givenSubtypeCollection() {
        List<StaticSubtype<? extends Animal>> subtypeCollection =
            Arrays.asList(
                StaticSubtype.builder(Cat.class).name("CAT").tableSchema(CAT_TABLE_SCHEMA).build(),
                StaticSubtype.builder(Snake.class).name("SNAKE").tableSchema(SNAKE_TABLE_SCHEMA).build());

        TableSchema<Animal> tableSchema =
            StaticPolymorphicTableSchema.builder(Animal.class)
                                        .rootTableSchema(ROOT_ANIMAL_TABLE_SCHEMA)
                                        .staticSubtypes(subtypeCollection)
                                        .build();

        assertThat(tableSchema.mapToItem(CAT_MAP)).isEqualTo(CAT);
        assertThat(tableSchema.mapToItem(SNAKE_MAP)).isEqualTo(SNAKE);
    }

    @Test
    public void shouldBringTheCorrectAttributeValuePolymorphic_givenAttributeName() {
        assertThat(ANIMAL_TABLE_SCHEMA.attributeValue(CAT, "breed"))
            .isEqualTo(AttributeValue.builder().s("persian").build());
    }

    @Test
    public void polymorphicTableSchemaShouldHaveTheCorrectItemType() {
        assertThat(ANIMAL_TABLE_SCHEMA.itemType()).isEqualTo(EnhancedType.of(Animal.class));
    }

    @Test
    public void polymorphicTableSchemaShouldHaveTheCorrectAttributeNames() {
        assertThat(ANIMAL_TABLE_SCHEMA.attributeNames()).containsExactlyInAnyOrder("id", "species");
    }

    @Test
    public void polymorphicTableSchemaShouldNotBeAbstract() {
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
            return this.id;
        }

        public String species() {
            return this.species;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Animal animal = (Animal) o;

            if (id != null ? !id.equals(animal.id) : animal.id != null) {
                return false;
            }
            return species != null ? species.equals(animal.species) : animal.species == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (species != null ? species.hashCode() : 0);
            return result;
        }

        @SuppressWarnings("unchecked")
        public static class Builder<T extends Builder<T>> {
            private String id;
            private String species;

            protected Builder() {
            }

            public T species(String species) {
                this.species = species;
                return (T) this;
            }

            public T id(String id) {
                this.id = id;
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
            return this.breed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            Cat cat = (Cat) o;

            return breed != null ? breed.equals(cat.breed) : cat.breed == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (breed != null ? breed.hashCode() : 0);
            return result;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends Animal.Builder<Builder> {
            private String breed;

            public Builder breed(String breed) {
                this.breed = breed;
                return this;
            }

            public Cat build() {
                return new Cat(this);
            }
        }
    }

    private static class Snake extends Animal {
        private final Boolean isVenomous;

        private Snake(Builder b) {
            super(b);
            this.isVenomous = b.isVenomous;
        }

        public Boolean isVenomous() {
            return this.isVenomous;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            Snake snake = (Snake) o;

            return isVenomous != null ? isVenomous.equals(snake.isVenomous) : snake.isVenomous == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (isVenomous != null ? isVenomous.hashCode() : 0);
            return result;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends Animal.Builder<Builder> {
            private Boolean isVenomous;

            public Builder isVenomous(Boolean isVenomous) {
                this.isVenomous = isVenomous;
                return this;
            }

            public Snake build() {
                return new Snake(this);
            }
        }
    }
}
