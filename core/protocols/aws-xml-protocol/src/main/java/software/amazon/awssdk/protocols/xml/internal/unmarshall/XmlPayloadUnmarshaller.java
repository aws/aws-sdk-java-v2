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

import static java.util.Collections.singletonList;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

@SdkInternalApi
public final class XmlPayloadUnmarshaller {
    public static final XmlUnmarshaller<String> STRING = new SimpleTypePayloadUnmarshaller<>(StringToValueConverter.TO_STRING);
    public static final XmlUnmarshaller<Integer> INTEGER = new SimpleTypePayloadUnmarshaller<>(StringToValueConverter.TO_INTEGER);
    public static final XmlUnmarshaller<Long> LONG = new SimpleTypePayloadUnmarshaller<>(StringToValueConverter.TO_LONG);
    public static final XmlUnmarshaller<Float> FLOAT = new SimpleTypePayloadUnmarshaller<>(StringToValueConverter.TO_FLOAT);
    public static final XmlUnmarshaller<Double> DOUBLE = new SimpleTypePayloadUnmarshaller<>(StringToValueConverter.TO_DOUBLE);
    public static final XmlUnmarshaller<BigDecimal> BIG_DECIMAL =
        new SimpleTypePayloadUnmarshaller<>(StringToValueConverter.TO_BIG_DECIMAL);
    public static final XmlUnmarshaller<Boolean> BOOLEAN = new SimpleTypePayloadUnmarshaller<>(StringToValueConverter.TO_BOOLEAN);
    public static final XmlUnmarshaller<Instant> INSTANT =
        new SimpleTypePayloadUnmarshaller<>(XmlProtocolUnmarshaller.INSTANT_STRING_TO_VALUE);
    public static final XmlUnmarshaller<SdkBytes> SDK_BYTES =
        new SimpleTypePayloadUnmarshaller<>(StringToValueConverter.TO_SDK_BYTES);

    private XmlPayloadUnmarshaller() {
    }

    public static SdkPojo unmarshallSdkPojo(XmlUnmarshallerContext context, List<XmlElement> content, SdkField<SdkPojo> field) {
        return context.protocolUnmarshaller()
                      .unmarshall(context, field.constructor().get(), content.get(0));
    }

    public static List<?> unmarshallList(XmlUnmarshallerContext context, List<XmlElement> content, SdkField<List<?>> field) {
        ListTrait listTrait = field.getTrait(ListTrait.class);
        List<Object> list = new ArrayList<>();

        getMembers(content, listTrait).forEach(member -> {
            XmlUnmarshaller unmarshaller = context.getUnmarshaller(listTrait.memberFieldInfo().location(),
                                                                   listTrait.memberFieldInfo().marshallingType());
            list.add(unmarshaller.unmarshall(context, singletonList(member), listTrait.memberFieldInfo()));
        });
        return list;
    }

    private static List<XmlElement> getMembers(List<XmlElement> content, ListTrait listTrait) {
        String memberLocation = listTrait.memberLocationName() != null ? listTrait.memberLocationName()
                                                                       : listTrait.memberFieldInfo().locationName();

        return listTrait.isFlattened() ?
               content :
               content.get(0).getElementsByName(memberLocation);
    }

    public static Map<String, ?> unmarshallMap(XmlUnmarshallerContext context, List<XmlElement> content,
                                               SdkField<Map<String, ?>> field) {
        Map<String, Object> map = new HashMap<>();
        MapTrait mapTrait = field.getTrait(MapTrait.class);
        SdkField mapValueSdkField = mapTrait.valueFieldInfo();

        getEntries(content, mapTrait).forEach(entry -> {
            XmlElement key = entry.getElementByName(mapTrait.keyLocationName());
            XmlElement value = entry.getElementByName(mapTrait.valueLocationName());
            XmlUnmarshaller unmarshaller = context.getUnmarshaller(mapValueSdkField.location(),
                                                                   mapValueSdkField.marshallingType());
            map.put(key.textContent(),
                    unmarshaller.unmarshall(context, singletonList(value), mapValueSdkField));
        });
        return map;
    }

    private static List<XmlElement> getEntries(List<XmlElement> content, MapTrait mapTrait) {
        return mapTrait.isFlattened() ?
               content :
               content.get(0).getElementsByName("entry");
    }

    /**
     * Base payload unmarshaller for simple types of xml protocol
     * @param <T> Type to unmarshall
     */
    private static class SimpleTypePayloadUnmarshaller<T> implements XmlUnmarshaller<T> {
        private final StringToValueConverter.StringToValue<T> converter;

        private SimpleTypePayloadUnmarshaller(StringToValueConverter.StringToValue<T> converter) {
            this.converter = converter;
        }

        @Override
        public T unmarshall(XmlUnmarshallerContext context, List<XmlElement> content, SdkField<T> field) {
            if (content == null) {
                return null;
            }
            return converter.convert(content.get(0).textContent(), field);
        }
    }
}
