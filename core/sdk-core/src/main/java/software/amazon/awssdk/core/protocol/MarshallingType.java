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

package software.amazon.awssdk.core.protocol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPojo;

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
    MarshallingType<Void> NULL = newType(Void.class);

    MarshallingType<String> STRING = newType(String.class);

    MarshallingType<Integer> INTEGER = newType(Integer.class);

    MarshallingType<Long> LONG = newType(Long.class);

    MarshallingType<Float> FLOAT = newType(Float.class);

    MarshallingType<Double> DOUBLE = newType(Double.class);

    MarshallingType<BigDecimal> BIG_DECIMAL = newType(BigDecimal.class);

    MarshallingType<Boolean> BOOLEAN = newType(Boolean.class);

    MarshallingType<Instant> INSTANT = newType(Instant.class);

    MarshallingType<SdkBytes> SDK_BYTES = newType(SdkBytes.class);

    MarshallingType<SdkPojo> SDK_POJO = newType(SdkPojo.class);

    MarshallingType<List<?>> LIST = newType(List.class);

    MarshallingType<Map<String, ?>> MAP = newType(Map.class);

    Class<? super T> getTargetClass();

    static <T> MarshallingType<T> newType(Class<? super T> clzz) {
        return new MarshallingType<T>() {

            @Override
            public Class<? super T> getTargetClass() {
                return clzz;
            }

            @Override
            public String toString() {
                return clzz.getSimpleName();
            }
        };

    }

}
