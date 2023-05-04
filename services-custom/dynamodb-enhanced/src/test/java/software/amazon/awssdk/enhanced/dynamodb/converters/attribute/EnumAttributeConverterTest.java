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

package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.EnumAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static org.assertj.core.api.Assertions.assertThat;

public class EnumAttributeConverterTest {

    @Test
    public void transformFromDefault_returnsToString() {
        EnumAttributeConverter<Vehicle> vehicleConverter = EnumAttributeConverter.create(Vehicle.class);
        AttributeValue attribute = vehicleConverter.transformFrom(Vehicle.TRUCK);

        assertThat(attribute.s()).isEqualTo("TRUCK");
    }

    @Test
    public void transformToDefault_returnsEnum() {
        EnumAttributeConverter<Vehicle> vehicleConverter = EnumAttributeConverter.create(Vehicle.class);

        Vehicle bike = vehicleConverter.transformTo(AttributeValue.fromS("BIKE"));

        assertThat(bike).isEqualTo(Vehicle.BIKE);
    }

    @Test
    public void transformFromDefault_returnsToString_2() {
        EnumAttributeConverter<Animal> animalConverter = EnumAttributeConverter.create(Animal.class);
        AttributeValue attribute = animalConverter.transformFrom(Animal.CAT);

        assertThat(attribute.s()).isEqualTo("I am a Cat!");
    }

    @Test
    public void transformToDefault_returnsEnum_2() {
        EnumAttributeConverter<Animal> animalConverter = EnumAttributeConverter.create(Animal.class);

        Animal dog = animalConverter.transformTo(AttributeValue.fromS("I am a Dog!"));

        assertThat(dog).isEqualTo(Animal.DOG);
    }

    @Test
    public void transformFromWithNames_returnsName() {
        EnumAttributeConverter<Person> personConverter = EnumAttributeConverter.createWithNameAsKeys(Person.class);
        AttributeValue attribute = personConverter.transformFrom(Person.JANE);

        assertThat(attribute.s()).isEqualTo("JANE");

        assertThat(Person.JANE.toString()).isEqualTo("I am a cool person");
    }

    @Test
    public void transformToWithNames_returnsEnum() {
        EnumAttributeConverter<Person> personConverter = EnumAttributeConverter.createWithNameAsKeys(Person.class);

        Person john = personConverter.transformTo(AttributeValue.fromS("JOHN"));

        assertThat(Person.JOHN.toString()).isEqualTo("I am a cool person");

        assertThat(john).isEqualTo(Person.JOHN);
    }

    private static enum Vehicle {
        CAR,
        BIKE,
        TRUCK
    }

    private static enum Animal {
        DOG,
        CAT;

        @Override
        public String toString() {
            switch (this) {
                case DOG:
                    return "I am a Dog!";
                case CAT:
                    return "I am a Cat!";
                default:
                    return null;
            }
        }
    }

    private static enum Person {
        JOHN,
        JANE;

        @Override
        public String toString() {
            return "I am a cool person";
        }
    }
}
