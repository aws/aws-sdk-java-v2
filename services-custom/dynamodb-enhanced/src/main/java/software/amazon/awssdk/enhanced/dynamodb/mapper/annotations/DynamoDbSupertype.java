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
 * Denotes this class as mapping to multiple subtype classes. Determination of which subtype to use is based on a single
 * attribute (the 'discriminator'). The attribute name is configured per-subtype, defaulting to "type".
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SdkPublicApi
public @interface DynamoDbSupertype {
    Subtype[] value();

    /** DynamoDB attribute name which holds the discriminator value; defaults to "type" */
    String discriminatorAttributeName() default "type";

    /**
     * Declare one concrete subtype: its discriminator value, the attribute name (defaults to "type"),
     * and the subtype’s Java class.
     */
    @interface Subtype {
        /** Value stored in the discriminator attribute for this subtype */
        String discriminatorValue() default "";

        /** The concrete Java class for this subtype */
        Class<?> subtypeClass();
    }
}

