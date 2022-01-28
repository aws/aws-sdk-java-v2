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

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypeName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes.Subtype;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class PolymorphismUnionTypeIntegrationTest {
    private static DynamoDbEnhancedClient client;
    private static DynamoDbTable<Cage> cagesTable;

    @BeforeClass
    public static void setup() {
        DynamoDbClient llc = DynamoDbClient.create();
        client = DynamoDbEnhancedClient.builder().dynamoDbClient(llc).build();
        cagesTable = client.table("cages", TableSchema.fromClass(Cage.class));
        cagesTable.createTable();
        llc.waiter().waitUntilTableExists(r -> r.tableName("cages"));

        Cat cat1 = new Cat();
        Cat cat2 = new Cat();
        cat1.setCatType(CatType.TABBY);
        cat2.setCatType(CatType.SIAMESE);

        Cage cats = new Cage();
        cats.setId(UUID.randomUUID());
        cats.setAnimals(new ArrayList<>());
        cats.getAnimals().add(cat1);
        cats.getAnimals().add(cat2);

        cagesTable.putItem(cats);


        Dog dog1 = new Dog();
        Dog dog2 = new Dog();
        dog1.setDogType(DogType.COLLIE);
        dog2.setDogType(DogType.WIENER);

        Cage dogs = new Cage();
        dogs.setId(UUID.randomUUID());
        dogs.setAnimals(new ArrayList<>());
        dogs.getAnimals().add(dog1);
        dogs.getAnimals().add(dog2);

        cagesTable.putItem(dogs);
    }

    @AfterClass
    public static void teardown() {
        cagesTable.deleteTable();
    }

    @Test
    public void getAllCages() {
        cagesTable.scan()
                  .items()
                  .forEach(System.out::println);
    }

    @DynamoDbBean
    public static class Cage {
        private UUID id;
        private List<Animal> animals;

        @DynamoDbAttribute("id")
        @DynamoDbPartitionKey
        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        @DynamoDbAttribute("animals")
        public List<Animal> getAnimals() {
            return animals;
        }

        public void setAnimals(List<Animal> animals) {
            this.animals = animals;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Cage.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("animals=" + animals)
                .toString();
        }
    }

    @DynamoDbBean
    @DynamoDbSubtypes({ @Subtype(name = "cat", subtypeClass = Cat.class),
                        @Subtype(name = "dog", subtypeClass = Dog.class) })
    public abstract static class Animal {
        @DynamoDbAttribute("animalType")
        @DynamoDbSubtypeName
        public abstract String getAnimalType();
        public void setAnimalType(String type) {}
    }

    @DynamoDbBean
    public static class Cat extends Animal {
        private CatType catType;

        @Override
        public String getAnimalType() {
            return "cat";
        }

        @DynamoDbAttribute("catType")
        public CatType getCatType() {
            return catType;
        }

        public void setCatType(CatType catType) {
            this.catType = catType;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Cat.class.getSimpleName() + "[", "]")
                .add("catType=" + catType)
                .toString();
        }
    }

    @DynamoDbBean
    public static class Dog extends Animal {
        private DogType dogType;

        @Override
        public String getAnimalType() {
            return "dog";
        }

        @DynamoDbAttribute("dogType")
        public DogType getDogType() {
            return dogType;
        }

        public void setDogType(DogType dogType) {
            this.dogType = dogType;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Dog.class.getSimpleName() + "[", "]")
                .add("dogType=" + dogType)
                .toString();
        }
    }

    public enum CatType {
        TABBY,
        SIAMESE
    }

    public enum DogType {
        COLLIE,
        WIENER
    }
}
