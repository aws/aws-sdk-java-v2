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
 * This annotation is used to flatten attributes into top-level attributes of the record that is read and written to the
 * database.
 * <p>
 * This annotation supports two types of flattening:
 * <ul>
 *   <li><b>Object flattening:</b> Flattens all attributes of a separate DynamoDb bean object</li>
 *   <li><b>Map flattening:</b> Flattens all key-value pairs of a {@code Map<String, String>} as individual attributes</li>
 * </ul>
 * <p>
 * The type of flattening is automatically determined based on the annotated property's type:
 * <ul>
 *   <li>If the property is a {@code Map<String, String>}, map flattening is used</li>
 *   <li>If the property is a DynamoDb bean class, object flattening is used</li>
 * </ul>
 * <p>
 * <b>Constraints:</b>
 * <ul>
 *   <li>Flattened attribute names cannot conflict with existing class attributes (validated at schema creation for objects,
 *   validated at runtime for maps)</li>
 *   <li>Only one {@code @DynamoDbFlatten Map<String, String>} property per class (validated at schema creation)</li>
 *   <li>Map flattening requires exactly {@code Map<String, String>} type (validated at schema creation)</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@SdkPublicApi
public @interface DynamoDbFlatten {
    /**
     * @deprecated This is no longer used, the class type of the attribute will be used instead.
     */
    @Deprecated
    Class<?> dynamoDbBeanClass() default Object.class;
}
