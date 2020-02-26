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

package software.amazon.awssdk.enhanced.dynamodb.converter.string;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.Converter;

/**
 * Converts a specific Java type to/from a {@link String}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface StringConverter<T> extends Converter<T> {
    /**
     * Convert the provided object into a string.
     */
    default String toString(T object) {
        return object.toString();
    }

    /**
     * Convert the provided string into an object.
     */
    T fromString(String string);
}
