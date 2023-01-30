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

import static software.amazon.awssdk.transfer.s3.internal.serialization.TransferManagerMarshallingUtils.getUnmarshaller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.traits.MapTrait;
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

    TransferManagerJsonUnmarshaller<String> STRING = (val, sdkField) -> val;
    TransferManagerJsonUnmarshaller<Short> SHORT = (val, sdkField) -> Short.parseShort(val);
    TransferManagerJsonUnmarshaller<Integer> INTEGER = (val, sdkField) -> Integer.parseInt(val);
    TransferManagerJsonUnmarshaller<Long> LONG = (val, sdkField) -> Long.parseLong(val);
    TransferManagerJsonUnmarshaller<Void> NULL = (val, sdkField) -> null;
    TransferManagerJsonUnmarshaller<Float> FLOAT = (val, sdkField) -> Float.parseFloat(val);
    TransferManagerJsonUnmarshaller<Double> DOUBLE = (val, sdkField) -> Double.parseDouble(val);
    TransferManagerJsonUnmarshaller<BigDecimal> BIG_DECIMAL = (val, sdkField) -> new BigDecimal(val);
    TransferManagerJsonUnmarshaller<Boolean> BOOLEAN = (val, sdkField) -> Boolean.parseBoolean(val);
    TransferManagerJsonUnmarshaller<SdkBytes> SDK_BYTES =
        (content, sdkField) -> SdkBytes.fromByteArray(BinaryUtils.fromBase64(content));

    TransferManagerJsonUnmarshaller<Instant> INSTANT = new TransferManagerJsonUnmarshaller<Instant>() {
        @Override
        public Instant unmarshall(String value, SdkField<?> field) {
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

    TransferManagerJsonUnmarshaller<Map<String, Object>> MAP = new TransferManagerJsonUnmarshaller<Map<String, Object>>() {

        @Override
        public Map<String, Object> unmarshall(JsonNode jsonContent, SdkField<?> field) {
            if (jsonContent == null) {
                return null;
            }

            SdkField<Object> valueInfo = field.getTrait(MapTrait.class).valueFieldInfo();

            Map<String, Object> map = new HashMap<>();
            jsonContent.asObject().forEach((fieldName, value) -> {
                TransferManagerJsonUnmarshaller<?> unmarshaller = getUnmarshaller(valueInfo.marshallingType());
                map.put(fieldName, unmarshaller.unmarshall(value));
            });
            return map;
        }

        @Override
        public Map<String, Object> unmarshall(String content, SdkField<?> field) {
            return unmarshall(JsonNode.parser().parse(content), field);
        }
    };

    default T unmarshall(JsonNode jsonContent, SdkField<?> field) {
        return jsonContent != null && !jsonContent.isNull() ? unmarshall(jsonContent.text(), field) : null;
    }

    default T unmarshall(JsonNode jsonContent) {
        return unmarshall(jsonContent, null);
    }

    T unmarshall(String content, SdkField<?> field);

}
