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

package software.amazon.awssdk.protocols.xml.internal.marshall;

import java.time.Instant;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.protocols.core.ValueToStringConverter;

@SdkInternalApi
public final class HeaderMarshaller {

    public static final XmlMarshaller<String> STRING = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_STRING);

    public static final XmlMarshaller<Integer> INTEGER = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_INTEGER);

    public static final XmlMarshaller<Long> LONG = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_LONG);

    public static final XmlMarshaller<Double> DOUBLE = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_DOUBLE);

    public static final XmlMarshaller<Float> FLOAT = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_FLOAT);

    public static final XmlMarshaller<Boolean> BOOLEAN = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_BOOLEAN);

    public static final XmlMarshaller<Instant> INSTANT =
        new SimpleHeaderMarshaller<>(XmlProtocolMarshaller.INSTANT_VALUE_TO_STRING);

    public static final XmlMarshaller<Map<String, ?>> MAP = new SimpleHeaderMarshaller<Map<String, ?>>(null) {
        @Override
        public void marshall(Map<String, ?> map, XmlMarshallerContext context, String paramName,
                             SdkField<Map<String, ?>> sdkField) {
            if (!shouldEmit(map)) {
                return;
            }

            for (Map.Entry<String, ?> entry : map.entrySet()) {
                String key = entry.getKey().startsWith(paramName) ? entry.getKey()
                                                                  : paramName + entry.getKey();

                XmlMarshaller marshaller = context.marshallerRegistry().getMarshaller(MarshallLocation.HEADER, entry.getValue());

                marshaller.marshall(entry.getValue(), context, key, null);
            }
        }

        @Override
        protected boolean shouldEmit(Map map) {
            return map != null && !map.isEmpty();
        }
    };


    private HeaderMarshaller() {
    }

    private static class SimpleHeaderMarshaller<T> implements XmlMarshaller<T> {
        private final ValueToStringConverter.ValueToString<T> converter;

        private SimpleHeaderMarshaller(ValueToStringConverter.ValueToString<T> converter) {
            this.converter = converter;
        }

        @Override
        public void marshall(T val, XmlMarshallerContext context, String paramName, SdkField<T> sdkField) {
            if (!shouldEmit(val)) {
                return;
            }

            context.request().putHeader(paramName, converter.convert(val, sdkField));
        }

        protected boolean shouldEmit(T val) {
            return val != null;
        }
    }
}
