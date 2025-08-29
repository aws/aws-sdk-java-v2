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

package software.amazon.awssdk.enhanced.dynamodb.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Denotes this class as mapping to a number of different subtype classes. Determination of which subtype to use in any given
 * situation is made based on a single attribute that is designated as the 'subtype discriminator' (see
 * {@link DynamoDbSubtypeDiscriminator}). This annotation may only be applied to a class that is also a valid DynamoDb annotated
 * class (either {@link DynamoDbBean} or {@link DynamoDbImmutable}), and likewise every subtype class must also be a valid
 * DynamoDb annotated class.
 * <p>
 * Example:
 * <p><pre>
 * {@code
 * @DynamoDbBean
 * @DynamoDbSupertype( {
 *   @Subtype(discriminatorValue = "CAT", subtypeClass = Cat.class),
 *   @Subtype(discriminatorValue = "DOG", subtypeClass = Dog.class) } )
 * public class Animal {
 *    @DynamoDbSubtypeDiscriminator
 *    String getType() { ... }
 *
 *    ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SdkPublicApi
public @interface DynamoDbSupertype {
    Subtype[] value();

    @interface Subtype {
        String discriminatorValue();

        Class<?> subtypeClass();
    }
}

