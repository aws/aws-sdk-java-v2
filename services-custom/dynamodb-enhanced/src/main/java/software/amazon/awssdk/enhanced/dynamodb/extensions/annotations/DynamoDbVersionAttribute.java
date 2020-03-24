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

package software.amazon.awssdk.enhanced.dynamodb.extensions.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.VersionRecordAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag;

/**
 * Denotes this attribute as recording the version record number to be used for optimistic locking. Every time a record
 * with this attribute is written to the database it will be incremented and a condition added to the request to check
 * for an exact match of the old version.
 */
@SdkPublicApi
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanTableSchemaAttributeTag(VersionRecordAttributeTags.class)
public @interface DynamoDbVersionAttribute {
}
