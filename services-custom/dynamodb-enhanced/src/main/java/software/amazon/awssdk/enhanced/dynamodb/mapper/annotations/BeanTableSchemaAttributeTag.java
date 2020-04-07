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
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;

/**
 * This meta-annotation is not used directly in DynamoDb beans, it is used to annotate other annotations that are
 * used with DynamoDb beans. You should use this meta-annotation if you are creating new annotations for the
 * BeanTableSchema.
 *
 * Meta-annotation for BeanTableSchema annotations that are used to assign an {@link StaticAttributeTag} to a property on the
 * bean. When an annotation that is annotated with this meta-annotation is found on a property being scanned by the
 * {@link BeanTableSchema} then a static method
 * named 'attributeTagFor' will be invoked passing in a single argument which is the property annotation itself.
 *
 * See {@link BeanTableSchemaAttributeTags} for an example of how to implement the {@link StaticAttributeTag} suppliers for
 * bean mapper annotations.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SdkPublicApi
public @interface BeanTableSchemaAttributeTag {
    Class<?> value();
}
