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

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.InstantToString;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.core.ProtocolUtils;
import software.amazon.awssdk.protocols.core.ValueToStringConverter;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Implementation of {@link ProtocolMarshaller} for REST-XML services. This is currently only Cloudfront, Route53,
 * and S3.
 */
@SdkInternalApi
public final class XmlProtocolMarshaller implements ProtocolMarshaller<SdkHttpFullRequest> {

    public static final ValueToStringConverter.ValueToString<Instant> INSTANT_VALUE_TO_STRING =
        InstantToString.create(getDefaultTimestampFormats());

    private static final XmlMarshallerRegistry MARSHALLER_REGISTRY = createMarshallerRegistry();

    private final URI endpoint;
    private final SdkHttpFullRequest.Builder request;
    private final String rootElement;
    private final XmlMarshallerContext marshallerContext;

    private XmlProtocolMarshaller(Builder builder) {
        this.endpoint = builder.endpoint;
        this.request = ProtocolUtils.createSdkHttpRequest(builder.operationInfo, this.endpoint);
        this.rootElement = builder.operationInfo.addtionalMetadata(AwsXmlProtocolFactory.ROOT_MARSHALL_LOCATION_ATTRIBUTE);
        this.marshallerContext = XmlMarshallerContext.builder()
                                                     .xmlGenerator(builder.xmlGenerator)
                                                     .marshallerRegistry(MARSHALLER_REGISTRY)
                                                     .protocolMarshaller(this)
                                                     .request(request)
                                                     .build();
    }

    @Override
    public SdkHttpFullRequest marshall(SdkPojo pojo) {
        if (rootElement != null) {
            marshallerContext.xmlGenerator().startElement(rootElement);
        }

        doMarshall(pojo);

        if (rootElement != null) {
            marshallerContext.xmlGenerator().endElement();
        }

        return finishMarshalling(pojo);
    }

    void doMarshall(SdkPojo pojo) {
        for (SdkField<?> field : pojo.sdkFields()) {
            Object val = field.getValueOrDefault(pojo);

            if (isBinary(field, val)) {
                request.contentStreamProvider(((SdkBytes) val)::asInputStream);
                setContentTypeHeaderIfNeeded("binary/octet-stream");

            } else if (isExplicitPayloadMember(field) && val instanceof String) {
                byte[] content = ((String) val).getBytes(StandardCharsets.UTF_8);
                request.contentStreamProvider(() -> new ByteArrayInputStream(content));
                request.putHeader(CONTENT_LENGTH, Integer.toString(content.length));

            } else {
                MARSHALLER_REGISTRY.getMarshaller(field.location(), field.marshallingType(), val)
                                   .marshall(val, marshallerContext, field.locationName(), (SdkField<Object>) field);
            }
        }
    }

    private SdkHttpFullRequest finishMarshalling(SdkPojo pojo) {
        // Content may already be set if the payload is binary data.
        if (hasPayloadMembers(pojo) && request.contentStreamProvider() == null
            && marshallerContext.xmlGenerator() != null) {
            String content = marshallerContext.xmlGenerator().stringWriter().getBuffer().toString();

            if (!content.isEmpty()) {
                request.contentStreamProvider(() -> new StringInputStream(content));
                request.putHeader("Content-Length", Integer.toString(content.getBytes(StandardCharsets.UTF_8).length));
                setContentTypeHeaderIfNeeded("application/xml");
            }
        }

        return request.build();
    }

    private boolean isBinary(SdkField<?> field, Object val) {
        return isExplicitPayloadMember(field) && val instanceof SdkBytes;
    }

    private boolean isExplicitPayloadMember(SdkField<?> field) {
        return field.containsTrait(PayloadTrait.class);
    }

    private boolean hasPayloadMembers(SdkPojo sdkPojo) {
        return sdkPojo.sdkFields().stream()
                      .anyMatch(f -> f.location() == MarshallLocation.PAYLOAD);
    }

    private void setContentTypeHeaderIfNeeded(String contentType) {
        if (contentType != null && !request.headers().containsKey(CONTENT_TYPE)) {
            request.putHeader(CONTENT_TYPE, contentType);
        }
    }

    private static Map<MarshallLocation, TimestampFormatTrait.Format> getDefaultTimestampFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new HashMap<>();
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.ISO_8601);
        formats.put(MarshallLocation.QUERY_PARAM, TimestampFormatTrait.Format.ISO_8601);
        return Collections.unmodifiableMap(formats);
    }

    private static XmlMarshallerRegistry createMarshallerRegistry() {
        return XmlMarshallerRegistry
            .builder()
            .payloadMarshaller(MarshallingType.STRING, XmlPayloadMarshaller.STRING)
            .payloadMarshaller(MarshallingType.INTEGER, XmlPayloadMarshaller.INTEGER)
            .payloadMarshaller(MarshallingType.LONG, XmlPayloadMarshaller.LONG)
            .payloadMarshaller(MarshallingType.FLOAT, XmlPayloadMarshaller.FLOAT)
            .payloadMarshaller(MarshallingType.DOUBLE, XmlPayloadMarshaller.DOUBLE)
            .payloadMarshaller(MarshallingType.BIG_DECIMAL, XmlPayloadMarshaller.BIG_DECIMAL)
            .payloadMarshaller(MarshallingType.BOOLEAN, XmlPayloadMarshaller.BOOLEAN)
            .payloadMarshaller(MarshallingType.INSTANT, XmlPayloadMarshaller.INSTANT)
            .payloadMarshaller(MarshallingType.SDK_BYTES, XmlPayloadMarshaller.SDK_BYTES)
            .payloadMarshaller(MarshallingType.SDK_POJO, XmlPayloadMarshaller.SDK_POJO)
            .payloadMarshaller(MarshallingType.LIST, XmlPayloadMarshaller.LIST)
            .payloadMarshaller(MarshallingType.MAP, XmlPayloadMarshaller.MAP)
            .payloadMarshaller(MarshallingType.NULL, XmlMarshaller.NULL)

            .headerMarshaller(MarshallingType.STRING, HeaderMarshaller.STRING)
            .headerMarshaller(MarshallingType.INTEGER, HeaderMarshaller.INTEGER)
            .headerMarshaller(MarshallingType.LONG, HeaderMarshaller.LONG)
            .headerMarshaller(MarshallingType.DOUBLE, HeaderMarshaller.DOUBLE)
            .headerMarshaller(MarshallingType.FLOAT, HeaderMarshaller.FLOAT)
            .headerMarshaller(MarshallingType.BOOLEAN, HeaderMarshaller.BOOLEAN)
            .headerMarshaller(MarshallingType.INSTANT, HeaderMarshaller.INSTANT)
            .headerMarshaller(MarshallingType.MAP, HeaderMarshaller.MAP)
            .headerMarshaller(MarshallingType.NULL, XmlMarshaller.NULL)

            .queryParamMarshaller(MarshallingType.STRING, QueryParamMarshaller.STRING)
            .queryParamMarshaller(MarshallingType.INTEGER, QueryParamMarshaller.INTEGER)
            .queryParamMarshaller(MarshallingType.LONG, QueryParamMarshaller.LONG)
            .queryParamMarshaller(MarshallingType.DOUBLE, QueryParamMarshaller.DOUBLE)
            .queryParamMarshaller(MarshallingType.FLOAT, QueryParamMarshaller.FLOAT)
            .queryParamMarshaller(MarshallingType.BOOLEAN, QueryParamMarshaller.BOOLEAN)
            .queryParamMarshaller(MarshallingType.INSTANT, QueryParamMarshaller.INSTANT)
            .queryParamMarshaller(MarshallingType.LIST, QueryParamMarshaller.LIST)
            .queryParamMarshaller(MarshallingType.MAP, QueryParamMarshaller.MAP)
            .queryParamMarshaller(MarshallingType.NULL, XmlMarshaller.NULL)

            .pathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshaller.STRING)
            .pathParamMarshaller(MarshallingType.INTEGER, SimpleTypePathMarshaller.INTEGER)
            .pathParamMarshaller(MarshallingType.LONG, SimpleTypePathMarshaller.LONG)
            .pathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshaller.NULL)

            .greedyPathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshaller.GREEDY_STRING)
            .greedyPathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshaller.NULL)
            .build();
    }

    /**
     * @return New {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link XmlProtocolMarshaller}.
     */
    public static final class Builder {

        private URI endpoint;
        private XmlGenerator xmlGenerator;
        private OperationInfo operationInfo;

        private Builder() {
        }

        /**
         * @param endpoint Endpoint to set on the marshalled request.
         * @return This builder for method chaining.
         */
        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * @param xmlGenerator Object to write XML data.
         * @return This builder for method chaining.
         */
        public Builder xmlGenerator(XmlGenerator xmlGenerator) {
            this.xmlGenerator = xmlGenerator;
            return this;
        }

        /**
         * @param operationInfo Metadata about the operation like URI, HTTP method, etc.
         * @return This builder for method chaining.
         */
        public Builder operationInfo(OperationInfo operationInfo) {
            this.operationInfo = operationInfo;
            return this;
        }

        /**
         * @return New instance of {@link XmlProtocolMarshaller}.
         */
        public XmlProtocolMarshaller build() {
            return new XmlProtocolMarshaller(this);
        }
    }
}
