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

package software.amazon.awssdk.core.protocol;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkBytes;

/**
 * Represents the various types supported for marshalling.
 *
 * @param <T> Java type bound to the marshalling type.
 */
@SdkProtectedApi
public interface MarshallingType<T> {

    /**
     * Used when a value is null (and thus type can't be determined).
     */
    MarshallingType<Void> NULL = () -> Void.class;

    MarshallingType<String> STRING = () -> String.class;

    MarshallingType<Integer> INTEGER = () -> Integer.class;

    MarshallingType<Long> LONG = () -> Long.class;

    MarshallingType<Float> FLOAT = () -> Float.class;

    MarshallingType<Double> DOUBLE = () -> Double.class;

    MarshallingType<BigDecimal> BIG_DECIMAL = () -> BigDecimal.class;

    MarshallingType<Boolean> BOOLEAN = () -> Boolean.class;

    MarshallingType<Instant> INSTANT = () -> Instant.class;

    MarshallingType<SdkBytes> SDK_BYTES = () -> SdkBytes.class;

    MarshallingType<InputStream> STREAM = () -> InputStream.class;

    MarshallingType<StructuredPojo> STRUCTURED = () -> StructuredPojo.class;

    MarshallingType<List> LIST = () -> List.class;

    MarshallingType<Map> MAP = () -> Map.class;

    Class<T> getTargetClass();

}
