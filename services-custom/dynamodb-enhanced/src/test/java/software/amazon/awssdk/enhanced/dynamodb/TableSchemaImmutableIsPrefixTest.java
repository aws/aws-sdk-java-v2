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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Tests that TableSchema.fromImmutableClass() works correctly with immutable classes
 * that have fields using "is" prefix.
 */
public class TableSchemaImmutableIsPrefixTest {

    // Test class for boolean fields with "is" prefix
    @DynamoDbImmutable(builder = Car.Builder.class)
    public static final class Car {
        private final String licensePlate;
        private final boolean isRusty;
        private final boolean isImpounded;

        private Car(Builder b) {
            this.licensePlate = b.licensePlate;
            this.isRusty = b.isRusty;
            this.isImpounded = b.isImpounded;
        }

        @DynamoDbPartitionKey
        public String licensePlate() {
            return this.licensePlate;
        }

        public boolean isRusty() {
            return this.isRusty;
        }

        public boolean isImpounded() {
            return this.isImpounded;
        }

        public static final class Builder {
            private String licensePlate;
            private boolean isRusty;
            private boolean isImpounded;

            public Builder licensePlate(String licensePlate) {
                this.licensePlate = licensePlate;
                return this;
            }

            public Builder isRusty(boolean isRusty) {
                this.isRusty = isRusty;
                return this;
            }

            public Builder isImpounded(boolean isImpounded) {
                this.isImpounded = isImpounded;
                return this;
            }

            public Car build() {
                return new Car(this);
            }
        }
    }

    @Test
    public void fromImmutableClass_withIsPrefixBooleanSetters_shouldCreateSchemaSuccessfully() {
        // This should work without exception
        TableSchema<Car> schema = TableSchema.fromImmutableClass(Car.class);
        
        // Verify the schema was created successfully without exception
        assertThat(schema).isNotNull();
        assertThat(schema.itemType().rawClass()).isEqualTo(Car.class);
        
        // Verify all attributes are mapped correctly
        assertThat(schema.attributeNames()).containsExactlyInAnyOrder(
            "licensePlate", "rusty", "impounded"
        );
    }

    // Test class for non-boolean fields with "is" prefix
    @DynamoDbImmutable(builder = Vehicle.Builder.class)
    public static final class Vehicle {
        private final String licensePlate;
        private final String isModel;
        private final Integer isYear;

        private Vehicle(Builder b) {
            this.licensePlate = b.licensePlate;
            this.isModel = b.isModel;
            this.isYear = b.isYear;
        }

        @DynamoDbPartitionKey
        public String licensePlate() {
            return this.licensePlate;
        }

        public String isModel() {
            return this.isModel;
        }

        public Integer isYear() {
            return this.isYear;
        }

        public static final class Builder {
            private String licensePlate;
            private String isModel;
            private Integer isYear;

            public Builder licensePlate(String licensePlate) {
                this.licensePlate = licensePlate;
                return this;
            }

            public Builder isModel(String isModel) {
                this.isModel = isModel;
                return this;
            }

            public Builder isYear(Integer isYear) {
                this.isYear = isYear;
                return this;
            }

            public Vehicle build() {
                return new Vehicle(this);
            }
        }
    }

    @Test
    public void fromImmutableClass_withIsPrefixNonBooleanFields_shouldNotNormalizeIsPrefix() {
        TableSchema<Vehicle> schema = TableSchema.fromImmutableClass(Vehicle.class);
        
        // Verify the schema was created successfully
        assertThat(schema).isNotNull();
        assertThat(schema.itemType().rawClass()).isEqualTo(Vehicle.class);
        
        // Verify non-boolean "is" prefix fields are not normalized
        assertThat(schema.attributeNames()).containsExactlyInAnyOrder(
            "licensePlate", "isModel", "isYear"
        );
    }
}
