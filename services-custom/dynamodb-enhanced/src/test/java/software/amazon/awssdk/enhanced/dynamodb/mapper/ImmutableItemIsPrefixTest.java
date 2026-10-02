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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

public class ImmutableItemIsPrefixTest {

    @Test
    public void fromImmutableClass_withIsPrefixBooleanSetters_shouldCreateSchemaSuccessfully() {
        // This should work without exception
        assertThatThrownBy(() -> TableSchema.fromImmutableClass(Car.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(
                "A method was found on the immutable class that does not appear to have a matching setter");
    }

    @DynamoDbImmutable(builder = Car.Builder.class)
    public static final class Car {
        private final String licensePlate;
        private final boolean isImpounded;

        private Car(Builder b) {
            this.licensePlate = b.licensePlate;
            this.isImpounded = b.isImpounded;
        }

        @DynamoDbPartitionKey
        public String licensePlate() {
            return this.licensePlate;
        }

        public boolean isImpounded() {
            return this.isImpounded;
        }

        public static final class Builder {
            private String licensePlate;
            private boolean isImpounded;

            public Builder licensePlate(String licensePlate) {
                this.licensePlate = licensePlate;
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
}
