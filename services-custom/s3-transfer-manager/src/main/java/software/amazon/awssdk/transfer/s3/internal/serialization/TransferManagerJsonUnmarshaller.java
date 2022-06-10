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

package software.amazon.awssdk.transfer.s3.internal.serialization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Interface for unmarshalling a field from JSON.
 *
 * @param <T> Type to unmarshall into.
 */
@FunctionalInterface
@SdkInternalApi
public interface TransferManagerJsonUnmarshaller<T> {

    TransferManagerJsonUnmarshaller<String> STRING = val -> val;
    TransferManagerJsonUnmarshaller<Short> SHORT = Short::parseShort;
    TransferManagerJsonUnmarshaller<Integer> INTEGER = Integer::parseInt;
    TransferManagerJsonUnmarshaller<Long> LONG = Long::parseLong;
    TransferManagerJsonUnmarshaller<Void> NULL = val -> null;
    TransferManagerJsonUnmarshaller<Float> FLOAT = Float::parseFloat;
    TransferManagerJsonUnmarshaller<Double> DOUBLE = Double::parseDouble;
    TransferManagerJsonUnmarshaller<BigDecimal> BIG_DECIMAL = BigDecimal::new;
    TransferManagerJsonUnmarshaller<Boolean> BOOLEAN = Boolean::parseBoolean;
    TransferManagerJsonUnmarshaller<SdkBytes> SDK_BYTES = content -> SdkBytes.fromByteArray(BinaryUtils.fromBase64(content));

    TransferManagerJsonUnmarshaller<Instant> INSTANT = new TransferManagerJsonUnmarshaller<Instant>() {
        @Override
        public Instant unmarshall(String value) {
            if (value == null) {
                return null;
            }
            return safeParseDate(DateUtils::parseUnixTimestampInstant).apply(value);
        }

        private Function<String, Instant> safeParseDate(Function<String, Instant> dateUnmarshaller) {
            return value -> {
                try {
                    return dateUnmarshaller.apply(value);
                } catch (NumberFormatException e) {
                    throw SdkClientException.builder()
                                            .message("Unable to parse date : " + value)
                                            .cause(e)
                                            .build();
                }
            };
        }
    };

    default T unmarshall(JsonNode jsonContent) {
        return jsonContent != null && !jsonContent.isNull() ? unmarshall(jsonContent.text()) : null;
    }

    T unmarshall(String content);

}
