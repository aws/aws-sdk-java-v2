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
 * Annotation to convert the enumeration value to a string.
 *
 * <p>Alternately, the {@link DynamoDbTyped} annotation may be used,</p>
 * <pre class="brush: java">
 * public static enum Status { OPEN, PENDING, CLOSED }
 *
 * &#064;DynamoDBTyped(DynamoDBAttributeType.S)
 * public Status status()
 * </pre>
 *
 * <p>Please note, there are some risks in distributed systems when using
 * enumerations as attributes intead of simply using a String.
 * When adding new values to the enumeration, the enum only changes must
 * be deployed before the enumeration value can be persisted. This will
 * ensure that all systems have the correct code to map it from the item
 * record in DynamoDB to your objects.</p>
 *
 * @see DynamoDbTypeConverted
 */
@DynamoDbTyped(DynamoDbMapperFieldModel.DynamoDbAttributeType.S)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDbTypeConvertedEnum {

}
