/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.converter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ExactInstanceOfConversionCondition;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.InstanceOfConversionCondition;

/**
 * A condition under which a {@link ItemAttributeValueConverter} is invoked by the SDK.
 *
 * <p>
 * This interface should not be implemented directly. Instead, the {@link #isExactInstanceOf(Class)} and
 * {@link #isInstanceOf(Class)} methods should be used to create instances of this class.
 *
 * <p>
 * See {@link ItemAttributeValueConverter} for more details regarding converter priority.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface ConversionCondition {
    /**
     * Create a condition that resolves to true when the Java type being converted exactly matches the provided type.
     *
     * <p>
     * For example a {@code ConversionCondition.isExactInstanceOf(HashMap.class)} {@link ItemAttributeValueConverter} will only
     * be invoked when the customer requests or provides a {@link HashMap}. Subtypes like {@link LinkedHashMap} will not be
     * handled by this converter.
     *
     * <p>
     * This call will fail with a {@link RuntimeException} if the provided type is null.
     */
    static ConversionCondition isExactInstanceOf(Class<?> clazz) {
        return new ExactInstanceOfConversionCondition(clazz);
    }

    /**
     * Create a condition that resolves to true when the Java type being converted matches or extends the provided type.
     *
     * <p>
     * For example a {@code ConversionCondition.isInstanceOf(Map.class)} {@link ItemAttributeValueConverter} will
     * be invoked when the customer requests or provides any implementation of {@link Map}.
     *
     * <p>
     * This call will fail with a {@link RuntimeException} if the provided type is null.
     */
    static ConversionCondition isInstanceOf(Class<?> clazz) {
        return new InstanceOfConversionCondition(clazz);
    }
}
