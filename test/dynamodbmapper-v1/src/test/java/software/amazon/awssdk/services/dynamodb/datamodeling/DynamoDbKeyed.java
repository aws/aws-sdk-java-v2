/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.services.dynamodb.model.KeyType;

/**
 * Annotation for marking a property a key for a modeled class.
 *
 * <pre class="brush: java">
 * &#064;DynamoDBKeyed(KeyType.HASH)
 * public UUID getKey()
 * </pre>
 *
 * <p>Alternately, the short-formed {@link DynamoDbHashKey}, and
 * {@link DynamoDbRangeKey} may be used directly on the field/getter.</p>
 *
 * <p>May be used as a meta-annotation.</p>
 */
@DynamoDb
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DynamoDbKeyed {

    /**
     * The primary key type; either {@code HASH} or {@code RANGE}.
     */
    KeyType value();

}
