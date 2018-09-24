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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.internal.util.AwsDateUtils;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.DateUtils;

public class StringToValueConverter {

    @FunctionalInterface
    public interface StringToValue<T> extends Function<String, T> {
    }

    /**
     * Identity converter.
     */
    public static final StringToValue<String> TO_STRING = val -> val;

    public static final StringToValue<Integer> TO_INTEGER = Integer::parseInt;

    public static final StringToValue<Long> TO_LONG = Long::parseLong;

    public static final StringToValue<Float> TO_FLOAT = Float::parseFloat;

    public static final StringToValue<Double> TO_DOUBLE = Double::parseDouble;

    public static final StringToValue<Boolean> TO_BOOLEAN = Boolean::parseBoolean;

    public static final StringToValue<Instant> TO_INSTANT = AwsDateUtils::parseServiceSpecificInstant;

    public static final StringToValue<Instant> TO_INSTANT_ISO = DateUtils::parseRfc1123Date;

    public static final StringToValue<SdkBytes> TO_SDK_BYTES = StringToValueConverter::toSdkBytes;

    private StringToValueConverter() {
    }

    private static SdkBytes toSdkBytes(String s) {
        return SdkBytes.fromByteArray(BinaryUtils.fromBase64(s));
    }

}
