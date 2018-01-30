/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * @Deprecated - Replaced by {@link DynamoDbTyped}
 */
@Deprecated
@DynamoDb
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDbScalarAttribute {

    /**
     * Optional parameter when the name of the attribute as stored in DynamoDB
     * should differ from the name used by the getter / setter.
     */
    String attributeName() default "";

    /**
     * The scalar attirbute type.
     * @see software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
     */
    ScalarAttributeType type();

}
