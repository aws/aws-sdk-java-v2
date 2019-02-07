/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interface for marking a class property as an attribute in a DynamoDB table.
 * Applied to the getter method or the class field for a modeled property. If
 * the annotation is applied directly to the class field, the corresponding
 * getter and setter must be declared in the same class.
 * <p>
 * This annotation is optional when the name of the DynamoDB attribute matches
 * the name of the property declared in the class. When they differ, use this
 * annotation with the attributeName() parameter to specify which DynamoDB
 * attribute this property corresponds to. Furthermore, the
 * {@link DynamoDbMapper} class assumes Java naming conventions, and will
 * lower-case the first character of a getter method's property name to
 * determine the name of the property. E.g., a method value() will map to the
 * DynamoDB attribute "value". Similarly, a method isValid() maps to the
 * DynamoDB attribute "valid".
 * <p>
 * Even getter method not marked with this annotation are assumed to be modeled
 * properties, unless marked with {@link DynamoDbIgnore}.
 */
@DynamoDb
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDbAttribute {

    /**
     * Optional parameter when the name of the attribute as stored in DynamoDB
     * should differ from the name used by the getter / setter.
     */
    String attributeName() default "";

    /**
     * Optional parameter when using {@link DynamoDbFlattened}; identifies
     * the field/property name on the target class to map as the attribute.
     * @see DynamoDbFlattened
     */
    String mappedBy() default "";
}
