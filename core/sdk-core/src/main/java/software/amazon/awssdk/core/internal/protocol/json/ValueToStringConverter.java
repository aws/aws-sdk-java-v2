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

package software.amazon.awssdk.core.internal.protocol.json;

import java.time.Instant;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.util.StringConversion;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Converts various types to Strings. Used for Query Param/Header/Path marshalling.
 */
@SdkInternalApi
public final class ValueToStringConverter {

    /**
     * Simple interface to convert a type to a String.
     *
     * @param <T> Type to convert.
     */
    @FunctionalInterface
    public interface ValueToString<T> extends Function<T, String> {
    }

    /**
     * Identity converter.
     */
    public static final ValueToString<String> FROM_STRING = val -> val;

    public static final ValueToString<Integer> FROM_INTEGER = StringConversion::fromInteger;

    public static final ValueToString<Long> FROM_LONG = StringConversion::fromLong;

    public static final ValueToString<Float> FROM_FLOAT = StringConversion::fromFloat;

    public static final ValueToString<Double> FROM_DOUBLE = StringConversion::fromDouble;

    /**
     * Marshalls boolean as a literal 'true' or 'false' string.
     */
    public static final ValueToString<Boolean> FROM_BOOLEAN = StringConversion::fromBoolean;

    /**
     * Marshalls date to an ISO8601 date string.
     */
    public static final ValueToString<Instant> FROM_INSTANT = DateUtils::formatIso8601Date;

    private ValueToStringConverter() {
    }
}
