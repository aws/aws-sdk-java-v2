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
import java.time.temporal.ChronoUnit;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.TimeToLiveAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag;

/**
 * Annotation used to mark an attribute in a DynamoDB-enhanced client model as a Time-To-Live (TTL) field.
 * <p>
 * This annotation allows automatic computation and assignment of a TTL value based on another field (the {@code baseField})
 * and a time offset defined by {@code duration} and {@code unit}. The TTL value is stored in epoch seconds and
 * can be configured to expire items from the table automatically.
 * <p>
 * To use this, the annotated method should return a {@link Long} value, which will be populated by the SDK at write time.
 * The {@code baseField} can be a temporal type such as {@link java.time.Instant}, {@link java.time.LocalDate},
 * {@link java.time.LocalDateTime}, etc., or a {@link Long} representing epoch seconds directly, serving as the reference point
 * for TTL calculation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@BeanTableSchemaAttributeTag(TimeToLiveAttributeTags.class)
@SdkPublicApi
public @interface DynamoDbTimeToLiveAttribute {

    /**
     * The name of the attribute whose value will serve as the base for TTL computation.
     * This can be a temporal type (e.g., {@link java.time.Instant}, {@link java.time.LocalDateTime})
     * or a {@link Long} representing epoch seconds.
     *
     * @return the attribute name to use as the base timestamp for TTL
     */
    String baseField() default "";

    /**
     * The amount of time to add to the {@code baseField} when computing the TTL value.
     * The resulting time will be converted to epoch seconds.
     *
     * @return the time offset to apply to the base field
     */
    long duration() default 0;

    /**
     * The time unit associated with the {@code duration}. Defaults to {@link ChronoUnit#SECONDS}.
     *
     * @return the time unit to use with the duration
     */
    ChronoUnit unit() default ChronoUnit.SECONDS;
}
