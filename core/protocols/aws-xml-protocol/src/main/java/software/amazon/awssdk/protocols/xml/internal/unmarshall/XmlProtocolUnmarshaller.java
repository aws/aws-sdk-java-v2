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

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.traits.XmlAttributeTrait;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.StringToInstant;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;
import software.amazon.awssdk.protocols.query.unmarshall.XmlErrorUnmarshaller;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.builder.Buildable;

@SdkInternalApi
public final class XmlProtocolUnmarshaller implements XmlErrorUnmarshaller {

    public static final StringToValueConverter.StringToValue<Instant> INSTANT_STRING_TO_VALUE
        = StringToInstant.create(getDefaultTimestampFormats());

    private static final XmlUnmarshallerRegistry REGISTRY = createUnmarshallerRegistry();

    private XmlProtocolUnmarshaller() {
    }

    public static XmlProtocolUnmarshaller create() {
        return new XmlProtocolUnmarshaller();
    }

    public <TypeT extends SdkPojo> TypeT unmarshall(SdkPojo sdkPojo,
                                                    SdkHttpFullResponse response) {

        XmlElement document = XmlResponseParserUtils.parse(sdkPojo, response);
        return unmarshall(sdkPojo, document, response);
    }

    /**
     * This method is also used to unmarshall exceptions. We use this since we've already parsed the XML
     * and the result root is in a different location depending on the protocol/service.
     */
    @Override
    public <TypeT extends SdkPojo> TypeT unmarshall(SdkPojo sdkPojo,
                                                    XmlElement resultRoot,
                                                    SdkHttpFullResponse response) {
        XmlUnmarshallerContext unmarshallerContext = XmlUnmarshallerContext.builder()
                                                                           .response(response)
                                                                           .registry(REGISTRY)
                                                                           .protocolUnmarshaller(this)
                                                                           .build();
        return (TypeT) unmarshall(unmarshallerContext, sdkPojo, resultRoot);
    }

    SdkPojo unmarshall(XmlUnmarshallerContext context, SdkPojo sdkPojo, XmlElement root) {
        for (SdkField<?> field : sdkPojo.sdkFields()) {
            XmlUnmarshaller<Object> unmarshaller = REGISTRY.getUnmarshaller(field.location(), field.marshallingType());

            if (root != null && field.location() == MarshallLocation.PAYLOAD) {
                if (isAttribute(field)) {
                    root.getOptionalAttributeByName(field.unmarshallLocationName())
                        .ifPresent(e -> field.set(sdkPojo, e));
                } else {
                    List<XmlElement> element = isExplicitPayloadMember(field) ?
                                               singletonList(root) :
                                               root.getElementsByName(field.unmarshallLocationName());

                    if (!CollectionUtils.isNullOrEmpty(element)) {
                        Object unmarshalled = unmarshaller.unmarshall(context, element, (SdkField<Object>) field);
                        field.set(sdkPojo, unmarshalled);
                    }
                }
            } else {
                Object unmarshalled = unmarshaller.unmarshall(context, null, (SdkField<Object>) field);
                field.set(sdkPojo, unmarshalled);
            }
        }

        if (!(sdkPojo instanceof Buildable)) {
            throw new RuntimeException("The sdkPojo passed to the unmarshaller is not buildable (must implement "
                                       + "Buildable)");
        }
        return (SdkPojo) ((Buildable) sdkPojo).build();
    }

    private boolean isAttribute(SdkField<?> field) {
        return field.containsTrait(XmlAttributeTrait.class);
    }

    private boolean isExplicitPayloadMember(SdkField<?> field) {
        return field.containsTrait(PayloadTrait.class);
    }

    private static Map<MarshallLocation, TimestampFormatTrait.Format> getDefaultTimestampFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new HashMap<>();
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.ISO_8601);
        return Collections.unmodifiableMap(formats);
    }

    private static XmlUnmarshallerRegistry createUnmarshallerRegistry() {
        return XmlUnmarshallerRegistry
            .builder()
            .statusCodeUnmarshaller(MarshallingType.INTEGER, (context, content, field) -> context.response().statusCode())
            .headerUnmarshaller(MarshallingType.STRING, HeaderUnmarshaller.STRING)
            .headerUnmarshaller(MarshallingType.INTEGER, HeaderUnmarshaller.INTEGER)
            .headerUnmarshaller(MarshallingType.LONG, HeaderUnmarshaller.LONG)
            .headerUnmarshaller(MarshallingType.DOUBLE, HeaderUnmarshaller.DOUBLE)
            .headerUnmarshaller(MarshallingType.BOOLEAN, HeaderUnmarshaller.BOOLEAN)
            .headerUnmarshaller(MarshallingType.INSTANT, HeaderUnmarshaller.INSTANT)
            .headerUnmarshaller(MarshallingType.FLOAT, HeaderUnmarshaller.FLOAT)
            .headerUnmarshaller(MarshallingType.MAP, HeaderUnmarshaller.MAP)

            .payloadUnmarshaller(MarshallingType.STRING, XmlPayloadUnmarshaller.STRING)
            .payloadUnmarshaller(MarshallingType.INTEGER, XmlPayloadUnmarshaller.INTEGER)
            .payloadUnmarshaller(MarshallingType.LONG, XmlPayloadUnmarshaller.LONG)
            .payloadUnmarshaller(MarshallingType.FLOAT, XmlPayloadUnmarshaller.FLOAT)
            .payloadUnmarshaller(MarshallingType.DOUBLE, XmlPayloadUnmarshaller.DOUBLE)
            .payloadUnmarshaller(MarshallingType.BIG_DECIMAL, XmlPayloadUnmarshaller.BIG_DECIMAL)
            .payloadUnmarshaller(MarshallingType.BOOLEAN, XmlPayloadUnmarshaller.BOOLEAN)
            .payloadUnmarshaller(MarshallingType.INSTANT, XmlPayloadUnmarshaller.INSTANT)
            .payloadUnmarshaller(MarshallingType.SDK_BYTES, XmlPayloadUnmarshaller.SDK_BYTES)
            .payloadUnmarshaller(MarshallingType.SDK_POJO, XmlPayloadUnmarshaller::unmarshallSdkPojo)
            .payloadUnmarshaller(MarshallingType.LIST, XmlPayloadUnmarshaller::unmarshallList)
            .payloadUnmarshaller(MarshallingType.MAP, XmlPayloadUnmarshaller::unmarshallMap)
            .build();
    }
}
