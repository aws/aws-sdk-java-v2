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
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanTableSchemaAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;

/**
 * Specifies the behavior when this attribute is updated as part of an 'update' operation such as UpdateItem. See
 * documentation of {@link UpdateBehavior} for details on the different behaviors supported and the default behavior.
 */
@SdkPublicApi
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanTableSchemaAttributeTag(BeanTableSchemaAttributeTags.class)
public @interface DynamoDbUpdateBehavior {
    UpdateBehavior value();
}
