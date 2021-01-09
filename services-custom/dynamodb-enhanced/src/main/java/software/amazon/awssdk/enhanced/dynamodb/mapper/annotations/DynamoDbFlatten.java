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
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SdkPublicApi
public @interface DynamoDbFlatten {
    /**
     * @deprecated This is no longer used, the class type of the attribute will be used instead.
     */
    @Deprecated
    Class<?> dynamoDbBeanClass() default Object.class;
}
