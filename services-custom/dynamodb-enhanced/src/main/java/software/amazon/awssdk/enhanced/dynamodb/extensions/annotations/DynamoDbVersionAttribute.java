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
 * <p>
 * <b>Default behavior:</b> startAt=-1, incrementBy=1, initialValue=1. First version will be 1.
 * <p>
 * See {@link software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension.Builder#initialValue(Long)}
 * for details on ambiguity handling and SDK v1 migration support.
 */
@SdkPublicApi
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanTableSchemaAttributeTag(VersionRecordAttributeTags.class)
public @interface DynamoDbVersionAttribute {
    /**
     * The starting value for the version attribute.
     * Default value when not set: {@code -1}, which enables {@link #initialValue()} behavior.
     * <p>
     * Cannot be used with {@link #initialValue()} - setting both will throw IllegalArgumentException.
     *
     * @return the starting value, must be -1 or non-negative
     * @deprecated Use {@link #initialValue()} instead.
     */
    @Deprecated
    long startAt() default -1;

    /**
     * The amount to increment the version by with each update.
     * Default value - {@code 1}.
     *
     * @return the increment value, must be greater than 0
     */
    long incrementBy() default 1;

    /**
     * The initial version value for new records.
     * Default value - {@code 1}.
     * Cannot be used with deprecated {@link #startAt()} when startAt >= 0.
     *
     * @return the initial version for new records, must be non-negative
     */
    long initialValue() default 1;
}
