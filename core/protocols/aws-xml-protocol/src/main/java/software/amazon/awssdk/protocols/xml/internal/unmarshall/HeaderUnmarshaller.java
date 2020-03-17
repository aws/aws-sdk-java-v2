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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import static software.amazon.awssdk.utils.StringUtils.replacePrefixIgnoreCase;
import static software.amazon.awssdk.utils.StringUtils.startsWithIgnoreCase;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

@SdkInternalApi
public final class HeaderUnmarshaller {
    public static final XmlUnmarshaller<String> STRING = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_STRING);
    public static final XmlUnmarshaller<Integer> INTEGER = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_INTEGER);
    public static final XmlUnmarshaller<Long> LONG = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_LONG);
    public static final XmlUnmarshaller<Float> FLOAT = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_FLOAT);
    public static final XmlUnmarshaller<Double> DOUBLE = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_DOUBLE);
    public static final XmlUnmarshaller<Boolean> BOOLEAN = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_BOOLEAN);
    public static final XmlUnmarshaller<Instant> INSTANT =
        new SimpleHeaderUnmarshaller<>(XmlProtocolUnmarshaller.INSTANT_STRING_TO_VALUE);

    public static final XmlUnmarshaller<Map<String, ?>> MAP = ((context, content, field) -> {
        Map<String, String> result = new HashMap<>();
        context.response().headers().entrySet().stream()
               .filter(e -> startsWithIgnoreCase(e.getKey(), field.locationName()))
               .forEach(e -> result.put(replacePrefixIgnoreCase(e.getKey(), field.locationName(), ""),
                                        String.join(",", e.getValue())));
        return result;
    });

    private HeaderUnmarshaller() {
    }

    private static class SimpleHeaderUnmarshaller<T> implements XmlUnmarshaller<T> {

        private final StringToValueConverter.StringToValue<T> stringToValue;

        private SimpleHeaderUnmarshaller(StringToValueConverter.StringToValue<T> stringToValue) {
            this.stringToValue = stringToValue;
        }

        @Override
        public T unmarshall(XmlUnmarshallerContext context, List<XmlElement> content, SdkField<T> field) {
            return context.response().firstMatchingHeader(field.locationName())
                          .map(s -> stringToValue.convert(s, field))
                          .orElse(null);
        }
    }
}
