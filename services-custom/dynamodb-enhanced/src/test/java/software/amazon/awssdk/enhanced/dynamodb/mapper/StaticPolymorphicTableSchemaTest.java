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
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class StaticPolymorphicTableSchemaTest {

    // ============================================================
    // Root: Person (immutable)
    // ============================================================
    private static final StaticImmutableTableSchema<Person, Person.Builder> ROOT_PERSON_SCHEMA =
        StaticImmutableTableSchema.builder(Person.class, Person.Builder.class)
                                  .addAttribute(String.class, a -> a.name("id")
                                                                    .getter(Person::id)
                                                                    .setter(Person.Builder::id)
                                                                    .tags(primaryPartitionKey()))
                                  .addAttribute(String.class, a -> a.name("type")
                                                                    .getter(Person::type)
                                                                    .setter(Person.Builder::type))
                                  .newItemBuilder(Person::builder, Person.Builder::build)
                                  .build();

    // ============================================================
    // Subtypes (Employee, Manager)
    // ============================================================
    private static final TableSchema<Employee> EMPLOYEE_SCHEMA =
        StaticImmutableTableSchema.builder(Employee.class, Employee.Builder.class)
                                  .addAttribute(String.class, a -> a.name("department")
                                                                    .getter(Employee::department)
                                                                    .setter(Employee.Builder::department))
                                  .newItemBuilder(Employee::builder, Employee.Builder::build)
                                  .extend(ROOT_PERSON_SCHEMA)
                                  .build();

    private static final TableSchema<Manager> MANAGER_SCHEMA =
        StaticImmutableTableSchema.builder(Manager.class, Manager.Builder.class)
                                  .addAttribute(Integer.class, a -> a.name("level")
                                                                     .getter(Manager::level)
                                                                     .setter(Manager.Builder::level))
                                  .newItemBuilder(Manager::builder, Manager.Builder::build)
                                  .extend(ROOT_PERSON_SCHEMA)
                                  .build();

    // ============================================================
    // Polymorphic schema (Person)
    // ============================================================
    private static final TableSchema<Person> PERSON_SCHEMA =
        StaticPolymorphicTableSchema.builder(Person.class)
                                    .rootTableSchema(ROOT_PERSON_SCHEMA)
                                    .discriminatorAttributeName("type")
                                    .addStaticSubtype(
                                        StaticSubtype.builder(Employee.class).name("EMPLOYEE").tableSchema(EMPLOYEE_SCHEMA).build(),
                                        StaticSubtype.builder(Manager.class).name("MANAGER").tableSchema(MANAGER_SCHEMA).build())
                                    .build();

    // ============================================================
    // Sample items
    // ============================================================
    private static final Employee EMPLOYEE =
        Employee.builder()
                .id("p:1")
                .type("EMPLOYEE")
                .department("engineering")
                .build();

    private static final Manager MANAGER =
        Manager.builder()
               .id("p:2")
               .type("MANAGER")
               .level(7)
               .build();

    // ============================================================
    // Sample maps
    // ============================================================
    private static final Map<String, AttributeValue> EMPLOYEE_MAP;
    private static final Map<String, AttributeValue> MANAGER_MAP;

    static {
        Map<String, AttributeValue> employeeAttributes = new HashMap<>();
        employeeAttributes.put("id", AttributeValue.builder().s("p:1").build());
        employeeAttributes.put("type", AttributeValue.builder().s("EMPLOYEE").build());
        employeeAttributes.put("department", AttributeValue.builder().s("engineering").build());
        EMPLOYEE_MAP = Collections.unmodifiableMap(employeeAttributes);

        Map<String, AttributeValue> managerAttributes = new HashMap<>();
        managerAttributes.put("id", AttributeValue.builder().s("p:2").build());
        managerAttributes.put("type", AttributeValue.builder().s("MANAGER").build());
        managerAttributes.put("level", AttributeValue.builder().n("7").build());
        MANAGER_MAP = Collections.unmodifiableMap(managerAttributes);
    }

    // ============================================================
    // Negative validations
    // ============================================================
    @Test
    public void shouldThrowWhenNoSubtypes() {
        assertThatThrownBy(() ->
                               StaticPolymorphicTableSchema.builder(Person.class)
                                                           .rootTableSchema(ROOT_PERSON_SCHEMA)
                                                           .discriminatorAttributeName("type")
                                                           .addStaticSubtype() // none
                                                           .build()
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A polymorphic TableSchema must have at least one subtype");
    }

    @Test
    public void shouldThrowWhenNoRootSchema() {
        assertThatThrownBy(() ->
                               StaticPolymorphicTableSchema.builder(Person.class)
                                                           .discriminatorAttributeName("type")
                                                           .addStaticSubtype(
                                                               StaticSubtype.builder(Employee.class)
                                                                            .name("EMPLOYEE")
                                                                            .tableSchema(EMPLOYEE_SCHEMA)
                                                                            .build())
                                                           .build()
        )
            .isInstanceOf(NullPointerException.class)
            .hasMessage("rootTableSchema must not be null.");
    }

    @Test
    public void shouldThrowOnDuplicateNames() {
        assertThatThrownBy(() ->
                               StaticPolymorphicTableSchema.builder(Person.class)
                                                           .rootTableSchema(ROOT_PERSON_SCHEMA)
                                                           .discriminatorAttributeName("type")
                                                           .addStaticSubtype(
                                                               StaticSubtype.builder(Employee.class).name("EMPLOYEE").tableSchema(EMPLOYEE_SCHEMA).build(),
                                                               StaticSubtype.builder(Manager.class).name("EMPLOYEE").tableSchema(MANAGER_SCHEMA).build())
                                                           .build()
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate subtype discriminator: EMPLOYEE");
    }

    // ============================================================
    // Serialization / Deserialization
    // ============================================================
    @Test
    public void shouldSerializeToMap() {
        assertThat(PERSON_SCHEMA.itemToMap(EMPLOYEE, false)).isEqualTo(EMPLOYEE_MAP);
        assertThat(PERSON_SCHEMA.itemToMap(MANAGER, false)).isEqualTo(MANAGER_MAP);

        // also when ignoreNulls == true
        assertThat(PERSON_SCHEMA.itemToMap(EMPLOYEE, true)).isEqualTo(EMPLOYEE_MAP);
        assertThat(PERSON_SCHEMA.itemToMap(MANAGER, true)).isEqualTo(MANAGER_MAP);
    }

    @Test
    public void shouldSerializePartialAttributes() {
        Map<String, AttributeValue> result =
            PERSON_SCHEMA.itemToMap(EMPLOYEE, Arrays.asList("id", "department"));
        assertThat(result)
            .containsOnlyKeys("id", "department")
            .containsEntry("id", AttributeValue.builder().s("p:1").build())
            .containsEntry("department", AttributeValue.builder().s("engineering").build());
    }

    @Test
    public void shouldDeserializeFromMap() {
        assertThat(PERSON_SCHEMA.mapToItem(EMPLOYEE_MAP))
            .usingRecursiveComparison()
            .isEqualTo(EMPLOYEE);

        assertThat(PERSON_SCHEMA.mapToItem(MANAGER_MAP))
            .usingRecursiveComparison()
            .isEqualTo(MANAGER);
    }

    @Test
    public void shouldReturnCorrectAttributeValue() {
        assertThat(PERSON_SCHEMA.attributeValue(EMPLOYEE, "department"))
            .isEqualTo(AttributeValue.builder().s("engineering").build());
    }

    @Test
    public void metadataAndTypeChecks() {
        assertThat(PERSON_SCHEMA.itemType().rawClass()).isEqualTo(Person.class);
        assertThat(PERSON_SCHEMA.attributeNames()).containsExactlyInAnyOrder("id", "type");
        assertThat(PERSON_SCHEMA.isAbstract()).isFalse();
    }

    @Test
    public void polymorphicTableSchemaShouldTakeMetadataFromRoot() {
        assertThat(PERSON_SCHEMA.tableMetadata()).isEqualTo(ROOT_PERSON_SCHEMA.tableMetadata());
    }

    // Even if subtypes are registered in the wrong order, the schema should still
    // match the most specific subtype (Manager instead of Employee).
    @Test
    public void resolvesMostSpecificSubtype_evenIfRegisteredAfterParent() {
        TableSchema<Person> schema =
            StaticPolymorphicTableSchema.builder(Person.class)
                                        .rootTableSchema(ROOT_PERSON_SCHEMA)
                                        .discriminatorAttributeName("type")
                                        .addStaticSubtype(
                                            StaticSubtype.builder(Employee.class).name("EMPLOYEE").tableSchema(EMPLOYEE_SCHEMA).build(),
                                            StaticSubtype.builder(Manager.class).name("MANAGER").tableSchema(MANAGER_SCHEMA).build())
                                        .build();

        Map<String, AttributeValue> out = schema.itemToMap(MANAGER, false);
        assertThat(out).isEqualTo(MANAGER_MAP);
    }

    // Items created before polymorphism was introduced (without a discriminator)
    // can still be deserialized using the root schema if fallback is enabled.
    @Test
    public void fallsBackToRootSchema_whenDiscriminatorIsMissing_andFallbackEnabled() {
        TableSchema<Person> schema =
            StaticPolymorphicTableSchema.builder(Person.class)
                                        .rootTableSchema(ROOT_PERSON_SCHEMA)
                                        .discriminatorAttributeName("type")
                                        .allowMissingDiscriminatorFallbackToRoot(true)
                                        .addStaticSubtype(
                                            StaticSubtype.builder(Employee.class).name("EMPLOYEE").tableSchema(EMPLOYEE_SCHEMA).build(),
                                            StaticSubtype.builder(Manager.class).name("MANAGER").tableSchema(MANAGER_SCHEMA).build())
                                        .build();

        Map<String, AttributeValue> legacy = new HashMap<>();
        legacy.put("id", AttributeValue.builder().s("legacy:1").build()); // no "type"

        assertThat(schema.mapToItem(legacy))
            .usingRecursiveComparison()
            .isEqualTo(Person.builder().id("legacy:1").type(null).build());
    }

    // ============================================================
    // NEW SCENARIO 1: “Diamond-like” with interfaces -> register concrete leaf
    // ============================================================

    /**
     * Director is a concrete class implementing two interfaces “below” Manager.
     */
    static class Director extends Manager {
        private final String scope;

        Director(Builder b) {
            super(b);
            this.scope = b.scope;
        }

        public String scope() {
            return scope;
        }

        static class Builder extends Manager.Builder {
            private String scope;

            @Override
            public Builder id(String v) {
                super.id(v);
                return this;
            }

            @Override
            public Builder type(String v) {
                super.type(v);
                return this;
            }

            @Override
            public Builder department(String v) {
                super.department(v);
                return this;
            }

            @Override
            public Builder level(Integer v) {
                super.level(v);
                return this;
            }

            public Builder scope(String v) {
                this.scope = v;
                return this;
            }

            @Override
            public Director build() {
                return new Director(this);
            }
        }

        static Builder builder() {
            return new Builder();
        }
    }

    private static final Director DIRECTOR =
        Director.builder()
                .id("p:3")
                .type("DIRECTOR")
                .department("engineering")
                .level(9)
                .scope("global")
                .build();

    private static final Map<String, AttributeValue> DIRECTOR_MAP;

    static {
        Map<String, AttributeValue> d = new HashMap<>();
        d.put("id", AttributeValue.builder().s("p:3").build());
        d.put("type", AttributeValue.builder().s("DIRECTOR").build());
        d.put("department", AttributeValue.builder().s("engineering").build());
        d.put("level", AttributeValue.builder().n("9").build());
        d.put("scope", AttributeValue.builder().s("global").build());
        DIRECTOR_MAP = Collections.unmodifiableMap(d);
    }

    /**
     * When the hierarchy forms a “diamond” via interfaces, registering the *concrete* leaf (Director) is unambiguous and both
     * serialization and deserialization work as expected.
     */
    @Test
    public void diamondLikeInterfaces_resolveByConcreteLeafSubtype() {
        // Build a Director schema that includes inherited attributes as well.
        StaticImmutableTableSchema<Director, Director.Builder> directorSchema =
            StaticImmutableTableSchema.builder(Director.class, Director.Builder.class)
                                      .addAttribute(String.class, a -> a.name("department")
                                                                        .getter(Director::department)          // inherited
                                                                        // from Employee
                                                                        .setter(Director.Builder::department)) // inherited setter
                                      .addAttribute(Integer.class, a -> a.name("level")
                                                                         .getter(Director::level)               // inherited
                                                                         // from Manager
                                                                         .setter(Director.Builder::level))
                                      .addAttribute(String.class, a -> a.name("scope")
                                                                        .getter(Director::scope)
                                                                        .setter(Director.Builder::scope))
                                      .newItemBuilder(Director::builder, Director.Builder::build)
                                      .extend(ROOT_PERSON_SCHEMA)   // still inherit id/type from Person
                                      .build();

        TableSchema<Person> schema =
            StaticPolymorphicTableSchema.builder(Person.class)
                                        .rootTableSchema(ROOT_PERSON_SCHEMA)
                                        .discriminatorAttributeName("type")
                                        .addStaticSubtype(
                                            StaticSubtype.builder(Employee.class).name("EMPLOYEE").tableSchema(EMPLOYEE_SCHEMA).build(),
                                            StaticSubtype.builder(Manager.class).name("MANAGER").tableSchema(MANAGER_SCHEMA).build(),
                                            StaticSubtype.builder(Director.class).name("DIRECTOR").tableSchema(directorSchema).build()
                                        )
                                        .build();

        assertThat(schema.itemToMap(DIRECTOR, false)).isEqualTo(DIRECTOR_MAP);
        assertThat(schema.mapToItem(DIRECTOR_MAP))
            .usingRecursiveComparison()
            .isEqualTo(DIRECTOR);
    }


    // ============================================================
    // NEW SCENARIO 2: Incompatible subtype registration is rejected
    // ============================================================

    /**
     * A concrete class not related to Person; used to assert validation.
     */
    static class Unrelated {
        final String id;

        Unrelated(String id) {
            this.id = id;
        }

        static class Builder {
            String id;

            public Builder id(String v) {
                this.id = v;
                return this;
            }

            public Unrelated build() {
                return new Unrelated(id);
            }
        }

        static Builder builder() {
            return new Builder();
        }
    }

    private static final StaticImmutableTableSchema<Unrelated, Unrelated.Builder> UNRELATED_SCHEMA =
        StaticImmutableTableSchema.builder(Unrelated.class, Unrelated.Builder.class)
                                  .addAttribute(String.class, a -> a.name("id")
                                                                    .getter(u -> u.id)
                                                                    .setter(Unrelated.Builder::id)
                                                                    .tags(primaryPartitionKey()))
                                  .newItemBuilder(Unrelated::builder, Unrelated.Builder::build)
                                  .build();

    /**
     * Trying to register a subtype that does not extend/implement the root (Person) fails validation with a clear message.
     */
    @SuppressWarnings( {"rawtypes", "unchecked"})
    @Test
    public void registeringIncompatibleSubtype_isRejectedWithClearMessage() {
        // Raw type on purpose to simulate a user mistake that bypasses generics.
        StaticSubtype bad =
            StaticSubtype.builder(Unrelated.class)
                         .name("X")
                         .tableSchema(UNRELATED_SCHEMA)
                         .build();

        assertThatThrownBy(() ->
                               StaticPolymorphicTableSchema.builder(Person.class)
                                                           .rootTableSchema(ROOT_PERSON_SCHEMA)
                                                           .discriminatorAttributeName("type")
                                                           .addStaticSubtype(bad)
                                                           .build()
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not assignable to Person");
    }

    // ============================================================
    // NEW SCENARIO 3: Unknown discriminator is a hard error
    // ============================================================
    @Test
    public void unknownDiscriminator_isHardError_noSilentFallback() {
        Map<String, AttributeValue> unknown = new HashMap<>();
        unknown.put("id", AttributeValue.builder().s("p:999").build());
        unknown.put("type", AttributeValue.builder().s("INTERN").build()); // not registered

        assertThatThrownBy(() -> PERSON_SCHEMA.mapToItem(unknown))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown discriminator 'INTERN'");
    }

    // ============================================================
    // Simple immutable beans for the tests (Person / Employee / Manager)
    // ============================================================

    // Base (Person)
    static class Person {
        private final String id;
        private final String type;

        Person(Builder b) {
            this.id = b.id;
            this.type = b.type;
        }

        static class Builder {
            protected String id;
            protected String type;

            public Builder id(String v) {
                this.id = v;
                return this;
            }

            public Builder type(String v) {
                this.type = v;
                return this;
            }

            public Person build() {
                return new Person(this);
            }
        }

        static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public String type() {
            return type;
        }
    }

    // Mid-level (Employee)
    static class Employee extends Person {
        private final String department;

        Employee(Builder b) {
            super(b);
            this.department = b.department;
        }

        static class Builder extends Person.Builder {
            private String department;

            @Override
            public Builder id(String v) {
                super.id(v);
                return this;
            }

            @Override
            public Builder type(String v) {
                super.type(v);
                return this;
            }

            public Builder department(String v) {
                this.department = v;
                return this;
            }

            @Override
            public Employee build() {
                return new Employee(this);
            }
        }

        static Builder builder() {
            return new Builder();
        }

        public String department() {
            return department;
        }
    }

    // Bottom-level (Manager)
    static class Manager extends Employee {
        private final Integer level;

        Manager(Builder b) {
            super(b);
            this.level = b.level;
        }

        static class Builder extends Employee.Builder {
            private Integer level;

            @Override
            public Builder id(String v) {
                super.id(v);
                return this;
            }

            @Override
            public Builder type(String v) {
                super.type(v);
                return this;
            }

            @Override
            public Builder department(String v) {
                super.department(v);
                return this;
            }

            public Builder level(Integer v) {
                this.level = v;
                return this;
            }

            @Override
            public Manager build() {
                return new Manager(this);
            }
        }

        static Builder builder() {
            return new Builder();
        }

        public Integer level() {
            return level;
        }
    }
}
