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

package software.amazon.awssdk.http;

import java.util.Locale;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Enum for available HTTP methods.
 */
@SdkProtectedApi
public enum SdkHttpMethod {

    GET,
    POST,
    PUT,
    DELETE,
    HEAD,
    PATCH,
    OPTIONS,;

    /**
     * @param value Raw string representing value of enum
     * @return HttpMethodName enum or null if value is not present.
     * @throws IllegalArgumentException If value does not represent a known enum value.
     */
    public static SdkHttpMethod fromValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        final String upperCaseValue = value.toUpperCase(Locale.ENGLISH);
        return Stream.of(values())
                .filter(h -> h.name().equals(upperCaseValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported HTTP method name " + value));
    }

}
