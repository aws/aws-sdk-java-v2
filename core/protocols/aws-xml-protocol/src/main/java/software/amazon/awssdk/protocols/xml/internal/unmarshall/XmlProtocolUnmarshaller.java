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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.traits.TraitType;
import software.amazon.awssdk.core.traits.XmlAttributeTrait;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.StringToInstant;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.query.unmarshall.XmlDomParser;
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

    public <TypeT extends SdkPojo> TypeT unmarshall(SdkPojo sdkPojo, SdkHttpFullResponse response) {
        XmlElement document = hasXmlPayload(sdkPojo, response) ? XmlResponseParserUtils.parse(sdkPojo, response) : null;
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

            if (field.location() != MarshallLocation.PAYLOAD) {
                Object unmarshalled = unmarshaller.unmarshall(context, null, (SdkField<Object>) field);
                field.set(sdkPojo, unmarshalled);
                continue;
            }

            if (isExplicitPayloadMember(field)) {
                InputStream content = context.response().content().orElse(null);
                if (field.marshallingType() == MarshallingType.SDK_BYTES) {
                    SdkBytes value = content == null ? SdkBytes.fromByteArrayUnsafe(new byte[0])
                                                     : SdkBytes.fromInputStream(content);
                    field.set(sdkPojo, value);
                    continue;
                }
                if (field.marshallingType() == MarshallingType.STRING) {
                    // TODO: If we ever break protected APIs, just parse this as a string and remove XML-wrapping
                    // compatibility for S3.
                    if (content == null) {
                        field.set(sdkPojo, "");
                    } else {
                        setExplicitStringPayload(unmarshaller, context, sdkPojo, root, field);
                    }
                    continue;
                }
                if (root != null && !isAttribute(field)) {
                    Object unmarshalled = unmarshaller.unmarshall(context, singletonList(root), (SdkField<Object>) field);
                    field.set(sdkPojo, unmarshalled);
                    continue;
                }
            }

            if (root == null) {
                continue;
            }

            if (isAttribute(field)) {
                root.getOptionalAttributeByName(field.unmarshallLocationName())
                    .ifPresent(e -> field.set(sdkPojo, e));
                continue;
            }

            List<XmlElement> element = root.getElementsByName(field.unmarshallLocationName());
            if (!CollectionUtils.isNullOrEmpty(element)) {
                Object unmarshalled = unmarshaller.unmarshall(context, element, (SdkField<Object>) field);
                field.set(sdkPojo, unmarshalled);
            }
        }

        if (!(sdkPojo instanceof Buildable)) {
            throw new RuntimeException("The sdkPojo passed to the unmarshaller is not buildable (must implement "
                                       + "Buildable)");
        }
        return (SdkPojo) ((Buildable) sdkPojo).build();
    }

    private void setExplicitStringPayload(XmlUnmarshaller<Object> unmarshaller, XmlUnmarshallerContext context,
                                             SdkPojo sdkPojo, XmlElement element, SdkField<?> field) {
        SdkBytes sdkBytes = SdkBytes.fromInputStream(context.response().content().get());
        String stringPayload = sdkBytes.asUtf8String();
        if (hasS3XmlEnvelopePrefix(stringPayload)) {
            InputStream inputStream = new ByteArrayInputStream(sdkBytes.asByteArray());
            XmlElement document = XmlDomParser.parse(inputStream);
            Object unmarshalled = unmarshaller.unmarshall(context, singletonList(document), (SdkField<Object>) field);
            field.set(sdkPojo, unmarshalled);
        } else {
            if (stringPayload.isEmpty()) {
                if (element == null) {
                    // User passed in empty String
                    field.set(sdkPojo, "");
                } else {
                    // InputStream may have already been read
                    Object unmarshalled = unmarshaller.unmarshall(context, singletonList(element), (SdkField<Object>) field);
                    field.set(sdkPojo, unmarshalled);
                }
            } else {
                field.set(sdkPojo, stringPayload);
            }
        }
    }

    // Handle S3 GetBucketPolicy(), which returns JSON so we wrap with XML
    private boolean hasS3XmlEnvelopePrefix(String payload) {
        String s3XmlEnvelopePrefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Policy><![CDATA[";
        return payload.startsWith(s3XmlEnvelopePrefix);
    }

    private boolean isAttribute(SdkField<?> field) {
        return field.containsTrait(XmlAttributeTrait.class, TraitType.XML_ATTRIBUTE_TRAIT);
    }

    private boolean isExplicitPayloadMember(SdkField<?> field) {
        return field.containsTrait(PayloadTrait.class, TraitType.PAYLOAD_TRAIT);
    }

    private boolean hasXmlPayload(SdkPojo sdkPojo, SdkHttpFullResponse response) {
        return sdkPojo.sdkFields()
                      .stream()
                      .anyMatch(f -> isPayloadMemberOnUnmarshall(f) && !isExplicitBlobPayloadMember(f)
                                     && !isExplicitStringPayloadMember(f))
            && response.content().isPresent();
    }

    private boolean isExplicitBlobPayloadMember(SdkField<?> f) {
        return isExplicitPayloadMember(f) && f.marshallingType() == MarshallingType.SDK_BYTES;
    }

    private boolean isExplicitStringPayloadMember(SdkField<?> f) {
        return isExplicitPayloadMember(f) && f.marshallingType() == MarshallingType.STRING;
    }

    private boolean isPayloadMemberOnUnmarshall(SdkField<?> f) {
        return f.location() == MarshallLocation.PAYLOAD || isInUri(f.location());
    }

    private static boolean isInUri(MarshallLocation location) {
        switch (location) {
            case PATH:
            case QUERY_PARAM:
                return true;
            default:
                return false;
        }
    }

    private static Map<MarshallLocation, TimestampFormatTrait.Format> getDefaultTimestampFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new EnumMap<>(MarshallLocation.class);
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
            .headerUnmarshaller(MarshallingType.SHORT, HeaderUnmarshaller.SHORT)
            .headerUnmarshaller(MarshallingType.DOUBLE, HeaderUnmarshaller.DOUBLE)
            .headerUnmarshaller(MarshallingType.BOOLEAN, HeaderUnmarshaller.BOOLEAN)
            .headerUnmarshaller(MarshallingType.INSTANT, HeaderUnmarshaller.INSTANT)
            .headerUnmarshaller(MarshallingType.FLOAT, HeaderUnmarshaller.FLOAT)
            .headerUnmarshaller(MarshallingType.MAP, HeaderUnmarshaller.MAP)
            .headerUnmarshaller(MarshallingType.LIST, HeaderUnmarshaller.LIST)

            .payloadUnmarshaller(MarshallingType.STRING, XmlPayloadUnmarshaller.STRING)
            .payloadUnmarshaller(MarshallingType.INTEGER, XmlPayloadUnmarshaller.INTEGER)
            .payloadUnmarshaller(MarshallingType.LONG, XmlPayloadUnmarshaller.LONG)
            .payloadUnmarshaller(MarshallingType.SHORT, XmlPayloadUnmarshaller.SHORT)
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
