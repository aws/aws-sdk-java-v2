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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import static software.amazon.awssdk.awscore.util.AwsHeader.AWS_REQUEST_ID;

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
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.StringToInstant;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.query.XmlDomParser;
import software.amazon.awssdk.protocols.query.XmlElement;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.builder.SdkBuilder;

@SdkInternalApi
public final class XmlProtocolUnmarshaller<TypeT extends SdkPojo> {

    public static final StringToValueConverter.StringToValue<Instant> INSTANT_STRING_TO_VALUE
        = StringToInstant.create(getDefaultTimestampFormats());

    private static final XmlUnmarshallerRegistry REGISTRY = createUnmarshallerRegistry();

    /**
     * If response shape has explicit payload, then root element is a member of response and should
     * be used when population fields. In this case, this value is set to True.
     * If no explicit payload member is present, root element can be ignored and this value is set to False.
     */
    private final boolean useRootElement;

    public XmlProtocolUnmarshaller(boolean useRootElement) {
        this.useRootElement = useRootElement;
    }

    public Pair<TypeT, Map<String, String>> unmarshall(SdkPojo sdkPojo,
                                                       SdkHttpFullResponse response) throws Exception {

        XmlUnmarshallerContext unmarshallerContext = XmlUnmarshallerContext.builder()
                                                                           .response(response)
                                                                           .registry(REGISTRY)
                                                                           .protocolUnmarshaller(this)
                                                                           .build();

        XmlElement document = hasPayloadMembers(sdkPojo) && response.content().isPresent()
                              ? XmlDomParser.parse(response.content().get()) : null;

        XmlElement resultRoot = document != null && useRootElement ? XmlElement.builder().addChildElement(document).build()
                                                                   : document;

        return Pair.of((TypeT) unmarshall(unmarshallerContext, sdkPojo, resultRoot), parseMetadata(document));
    }

    SdkPojo unmarshall(XmlUnmarshallerContext context, SdkPojo sdkPojo, XmlElement root) {
        for (SdkField<?> field : sdkPojo.sdkFields()) {
            XmlUnmarshaller<Object> unmarshaller = REGISTRY.getUnmarshaller(field.location(), field.marshallingType());

            if (root != null && field.location() == MarshallLocation.PAYLOAD) {
                List<XmlElement> element = root.getElementsByName(field.unmarshallLocationName());
                if (!CollectionUtils.isNullOrEmpty(element)) {
                    Object unmarshalled = unmarshaller.unmarshall(context, element, (SdkField<Object>) field);
                    field.set(sdkPojo, unmarshalled);
                }
            } else {
                Object unmarshalled = unmarshaller.unmarshall(context, null, (SdkField<Object>) field);
                field.set(sdkPojo, unmarshalled);
            }
        }
        return ((SdkBuilder<?, SdkPojo>) sdkPojo).build();
    }

    private Map<String, String> parseMetadata(XmlElement document) {
        if (document == null) {
            return new HashMap<>();
        }

        XmlElement responseMetadata = document.getElementByName("ResponseMetadata");
        Map<String, String> metadata = new HashMap<>();
        if (responseMetadata != null) {
            responseMetadata.children().forEach(c -> metadata.put(metadataKeyName(c), c.textContent()));
        }
        XmlElement requestId = document.getElementByName("requestId");
        if (requestId != null) {
            metadata.put(AWS_REQUEST_ID, requestId.textContent());
        }
        return metadata;
    }

    private String metadataKeyName(XmlElement c) {
        return c.elementName().equals("RequestId") ? AWS_REQUEST_ID : c.elementName();
    }

    private boolean hasPayloadMembers(SdkPojo sdkPojo) {
        return sdkPojo.sdkFields().stream()
                      .filter(f -> f.location() == MarshallLocation.PAYLOAD)
                      .findAny()
                      .isPresent();
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
