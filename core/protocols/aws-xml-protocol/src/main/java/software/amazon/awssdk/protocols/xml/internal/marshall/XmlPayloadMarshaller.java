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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.XmlAttributeTrait;
import software.amazon.awssdk.core.traits.XmlAttributesTrait;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.protocols.core.ValueToStringConverter;

@SdkInternalApi
public class XmlPayloadMarshaller {

    public static final XmlMarshaller<String> STRING = new BasePayloadMarshaller<>(ValueToStringConverter.FROM_STRING);

    public static final XmlMarshaller<Integer> INTEGER = new BasePayloadMarshaller<>(ValueToStringConverter.FROM_INTEGER);

    public static final XmlMarshaller<Long> LONG = new BasePayloadMarshaller<>(ValueToStringConverter.FROM_LONG);

    public static final XmlMarshaller<Float> FLOAT = new BasePayloadMarshaller<>(ValueToStringConverter.FROM_FLOAT);

    public static final XmlMarshaller<Double> DOUBLE = new BasePayloadMarshaller<>(ValueToStringConverter.FROM_DOUBLE);

    public static final XmlMarshaller<BigDecimal> BIG_DECIMAL =
        new BasePayloadMarshaller<>(ValueToStringConverter.FROM_BIG_DECIMAL);

    public static final XmlMarshaller<Boolean> BOOLEAN = new BasePayloadMarshaller<>(ValueToStringConverter.FROM_BOOLEAN);

    public static final XmlMarshaller<Instant> INSTANT =
        new BasePayloadMarshaller<>(XmlProtocolMarshaller.INSTANT_VALUE_TO_STRING);

    public static final XmlMarshaller<SdkBytes> SDK_BYTES = new BasePayloadMarshaller<>(ValueToStringConverter.FROM_SDK_BYTES);

    public static final XmlMarshaller<SdkPojo> SDK_POJO = new BasePayloadMarshaller<SdkPojo>(null) {
        @Override
        public void marshall(SdkPojo val, XmlMarshallerContext context, String paramName,
                             SdkField<SdkPojo> sdkField, ValueToStringConverter.ValueToString<SdkPojo> converter) {
            context.protocolMarshaller().doMarshall(val);
        }
    };

    public static final XmlMarshaller<List<?>> LIST = new BasePayloadMarshaller<List<?>>(null) {

        @Override
        public void marshall(List<?> val, XmlMarshallerContext context, String paramName, SdkField<List<?>> sdkField) {
            if (!shouldEmit(val, paramName)) {
                return;
            }

            marshall(val, context, paramName, sdkField, null);
        }

        @Override
        public void marshall(List<?> list, XmlMarshallerContext context, String paramName,
                             SdkField<List<?>> sdkField, ValueToStringConverter.ValueToString<List<?>> converter) {
            ListTrait listTrait = sdkField
                .getOptionalTrait(ListTrait.class)
                .orElseThrow(() -> new IllegalStateException(paramName + " member is missing ListTrait"));

            if (!listTrait.isFlattened()) {
                context.xmlGenerator().startElement(paramName);
            }

            SdkField memberField = listTrait.memberFieldInfo();
            String memberLocationName = listMemberLocationName(listTrait, paramName);

            for (Object listMember : list) {
                context.marshall(MarshallLocation.PAYLOAD, listMember, memberLocationName, memberField);
            }

            if (!listTrait.isFlattened()) {
                context.xmlGenerator().endElement();
            }
        }

        private String listMemberLocationName(ListTrait listTrait, String listLocationName) {
            String locationName = listTrait.memberLocationName();

            if (locationName == null) {
                locationName = listTrait.isFlattened() ? listLocationName : "member";
            }

            return locationName;
        }

        @Override
        protected boolean shouldEmit(List list, String paramName) {
            return super.shouldEmit(list, paramName) &&
                   (!list.isEmpty() || !(list instanceof SdkAutoConstructList));
        }
    };

    // We ignore flattened trait for maps. For rest-xml, none of the services have flattened maps
    public static final XmlMarshaller<Map<String, ?>> MAP = new BasePayloadMarshaller<Map<String, ?>>(null) {

        @Override
        public void marshall(Map<String, ?> map, XmlMarshallerContext context, String paramName,
                             SdkField<Map<String, ?>> sdkField, ValueToStringConverter.ValueToString<Map<String, ?>> converter) {

            MapTrait mapTrait = sdkField.getOptionalTrait(MapTrait.class)
                                        .orElseThrow(() -> new IllegalStateException(paramName + " member is missing MapTrait"));

            for (Map.Entry<String, ?> entry : map.entrySet()) {
                context.xmlGenerator().startElement("entry");
                context.marshall(MarshallLocation.PAYLOAD, entry.getKey(), mapTrait.keyLocationName(), null);
                context.marshall(MarshallLocation.PAYLOAD, entry.getValue(), mapTrait.valueLocationName(),
                                 mapTrait.valueFieldInfo());
                context.xmlGenerator().endElement();
            }
        }

        @Override
        protected boolean shouldEmit(Map map, String paramName) {
            return super.shouldEmit(map, paramName) &&
                   (!map.isEmpty() || !(map instanceof SdkAutoConstructMap));
        }
    };

    private XmlPayloadMarshaller() {
    }

    /**
     * Base payload marshaller for xml protocol. Marshalling happens only when both element name and value are present.
     *
     * Marshalling for simple types is done in the base class.
     * Complex types should override the
     * {@link #marshall(Object, XmlMarshallerContext, String, SdkField, ValueToStringConverter.ValueToString)} method.
     *
     * @param <T> Type to marshall
     */
    private static class BasePayloadMarshaller<T> implements XmlMarshaller<T> {

        private final ValueToStringConverter.ValueToString<T> converter;

        private BasePayloadMarshaller(ValueToStringConverter.ValueToString<T> converter) {
            this.converter = converter;
        }

        @Override
        public void marshall(T val, XmlMarshallerContext context, String paramName, SdkField<T> sdkField) {
            if (!shouldEmit(val, paramName)) {
                return;
            }

            // Should ignore marshalling for xml attribute
            if (isXmlAttribute(sdkField)) {
                return;
            }

            if (sdkField != null && sdkField.getOptionalTrait(XmlAttributesTrait.class).isPresent()) {
                XmlAttributesTrait attributeTrait = sdkField.getTrait(XmlAttributesTrait.class);
                Map<String, String> attributes = attributeTrait.attributes()
                                                               .entrySet()
                                                               .stream()
                                                               .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(),
                                                                                                            e.getValue()
                                                                                                             .attributeGetter()
                                                                                                             .apply(val)),
                                                                        HashMap::putAll);
                context.xmlGenerator().startElement(paramName, attributes);
            } else {
                context.xmlGenerator().startElement(paramName);
            }

            marshall(val, context, paramName, sdkField, converter);
            context.xmlGenerator().endElement();
        }

        void marshall(T val, XmlMarshallerContext context, String paramName, SdkField<T> sdkField,
                      ValueToStringConverter.ValueToString<T> converter) {
            context.xmlGenerator().xmlWriter().value(converter.convert(val, sdkField));
        }

        protected boolean shouldEmit(T val, String paramName) {
            return val != null && paramName != null;
        }

        private boolean isXmlAttribute(SdkField<T> sdkField) {
            return sdkField != null && sdkField.getOptionalTrait(XmlAttributeTrait.class).isPresent();
        }
    }

}
