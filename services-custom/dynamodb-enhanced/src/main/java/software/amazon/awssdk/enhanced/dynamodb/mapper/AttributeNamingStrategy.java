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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Transforms a source name into the stored attribute name.
 *
 * <p>
 * {@code customerId} is stored as {@code customer_id}.
 * {@snippet :
 *   AttributeNamingStrategy strategy = BuiltInAttributeNamingStrategy.LOWER_SNAKE_CASE;
 *   strategy.transform("customerId");
 * }
 */
@SdkPublicApi
@ThreadSafe
@FunctionalInterface
public interface AttributeNamingStrategy {

    /**
     * Returns the stored attribute name for the source name.
     *
     * @param sourceName the source name
     * @return the stored attribute name
     */
    String transform(String sourceName);
}
