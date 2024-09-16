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
 * This annotation is used to flatten all the attributes of a separate DynamoDb bean that is stored in the current bean
 * object and add them as top level attributes to the record that is read and written to the database. The target bean
 * to flatten must be specified as part of this annotation.
 * The flattening behavior can be controlled by the prefix value of the annotation.
 * The default behavior is that no prefix is applied (this is done for backwards compatability).
 * If a String value is supplied then that is prefixed to the attribute names.
 * If a value of {@code DynamoDbFlatten.AUTO_PREFIX} is supplied then the attribute name of the flattened bean appended
 * with a period ('.') is used as the prefix.
 *
 * Example, given the following classes:
 * <pre>{@code
 * @DynamoDbBean
 * public class Flattened {
 *     String getValue();
 * }
 *
 * @DynamoDbBean
 * public class Record {
 *     @DynamoDbFlatten
 *     Flattened getNoPrefix(); // translates to attribute 'value'
 *     @DynamoDbFlatten(prefix = "prefix-")
 *     Flattened getExplicitPrefix(); // translates to attribute 'prefix-value'
 *     @DynamoDbFlatten(prefix = DynamoDbFlatten.AUTO_PREFIX)
 *     Flattened getInferredPrefix(); // translates to attribute 'inferredPrefix.value'
 *     @DynamoDbAttribute("custom")
 *     @DynamoDbFlatten(prefix = DynamoDbFlatten.AUTO_PREFIX)
 *     Flattened getFlattened(); // translates to attribute 'custom.value'
 * }
 *}</pre>
 * They would be mapped as such:
 * <pre>{@code
 * {
 *     "value": {"S": "..."},
 *     "prefix-value": {"S": "..."},
 *     "inferredPrefix.value": {"S": "..."},
 *     "custom.value": {"S": "..."},
 * }
 * }</pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SdkPublicApi
public @interface DynamoDbFlatten {
    /**
     * Values used to denote that the mapper should append the current attribute name to flattened fields.
     */
    String AUTO_PREFIX = "AUTO_PREFIX";

    /**
     * @deprecated This is no longer used, the class type of the attribute will be used instead.
     */
    @Deprecated
    Class<?> dynamoDbBeanClass() default Object.class;

    /**
     * Optional prefix to append to the flattened bean attributes in the schema.
     * Specifying a value of {@code DynamoDbFlatten.AUTO_PREFIX} will use the annotated methods attribute name as the
     * prefix.
     * default: {@code ""} (No prefix)
     */
    String prefix() default "";
}
