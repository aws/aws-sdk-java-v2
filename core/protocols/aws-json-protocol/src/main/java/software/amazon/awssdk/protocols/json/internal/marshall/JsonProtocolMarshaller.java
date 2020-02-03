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

package software.amazon.awssdk.protocols.json.internal.marshall;

import static software.amazon.awssdk.core.internal.util.Mimetype.MIMETYPE_EVENT_STREAM;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import java.io.ByteArrayInputStream;
import java.net.URI;
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
import software.amazon.awssdk.protocols.core.ValueToStringConverter.ValueToString;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;

/**
 * Implementation of {@link ProtocolMarshaller} for JSON based services. This includes JSON-RPC and REST-JSON.
 */
@SdkInternalApi
public class JsonProtocolMarshaller implements ProtocolMarshaller<SdkHttpFullRequest> {

    public static final ValueToString<Instant> INSTANT_VALUE_TO_STRING =
        InstantToString.create(getDefaultTimestampFormats());

    private static final JsonMarshallerRegistry MARSHALLER_REGISTRY = createMarshallerRegistry();

    private final URI endpoint;
    private final StructuredJsonGenerator jsonGenerator;
    private final SdkHttpFullRequest.Builder request;
    private final String contentType;
    private final boolean hasExplicitPayloadMember;
    private final boolean hasStreamingInput;

    private final JsonMarshallerContext marshallerContext;
    private final boolean hasEventStreamingInput;
    private final boolean hasEvent;

    JsonProtocolMarshaller(URI endpoint,
                           StructuredJsonGenerator jsonGenerator,
                           String contentType,
                           OperationInfo operationInfo) {
        this.endpoint = endpoint;
        this.jsonGenerator = jsonGenerator;
        this.contentType = contentType;
        this.hasExplicitPayloadMember = operationInfo.hasExplicitPayloadMember();
        this.hasStreamingInput = operationInfo.hasStreamingInput();
        this.hasEventStreamingInput = operationInfo.hasEventStreamingInput();
        this.hasEvent = operationInfo.hasEvent();
        this.request = fillBasicRequestParams(operationInfo);
        this.marshallerContext = JsonMarshallerContext.builder()
                                                      .jsonGenerator(jsonGenerator)
                                                      .marshallerRegistry(MARSHALLER_REGISTRY)
                                                      .protocolHandler(this)
                                                      .request(request)
                                                      .build();
    }

    private static JsonMarshallerRegistry createMarshallerRegistry() {
        return JsonMarshallerRegistry
            .builder()
            .payloadMarshaller(MarshallingType.STRING, SimpleTypeJsonMarshaller.STRING)
            .payloadMarshaller(MarshallingType.INTEGER, SimpleTypeJsonMarshaller.INTEGER)
            .payloadMarshaller(MarshallingType.LONG, SimpleTypeJsonMarshaller.LONG)
            .payloadMarshaller(MarshallingType.DOUBLE, SimpleTypeJsonMarshaller.DOUBLE)
            .payloadMarshaller(MarshallingType.FLOAT, SimpleTypeJsonMarshaller.FLOAT)
            .payloadMarshaller(MarshallingType.BIG_DECIMAL, SimpleTypeJsonMarshaller.BIG_DECIMAL)
            .payloadMarshaller(MarshallingType.BOOLEAN, SimpleTypeJsonMarshaller.BOOLEAN)
            .payloadMarshaller(MarshallingType.INSTANT, SimpleTypeJsonMarshaller.INSTANT)
            .payloadMarshaller(MarshallingType.SDK_BYTES, SimpleTypeJsonMarshaller.SDK_BYTES)
            .payloadMarshaller(MarshallingType.SDK_POJO, SimpleTypeJsonMarshaller.SDK_POJO)
            .payloadMarshaller(MarshallingType.LIST, SimpleTypeJsonMarshaller.LIST)
            .payloadMarshaller(MarshallingType.MAP, SimpleTypeJsonMarshaller.MAP)
            .payloadMarshaller(MarshallingType.NULL, SimpleTypeJsonMarshaller.NULL)

            .headerMarshaller(MarshallingType.STRING, HeaderMarshaller.STRING)
            .headerMarshaller(MarshallingType.INTEGER, HeaderMarshaller.INTEGER)
            .headerMarshaller(MarshallingType.LONG, HeaderMarshaller.LONG)
            .headerMarshaller(MarshallingType.DOUBLE, HeaderMarshaller.DOUBLE)
            .headerMarshaller(MarshallingType.FLOAT, HeaderMarshaller.FLOAT)
            .headerMarshaller(MarshallingType.BOOLEAN, HeaderMarshaller.BOOLEAN)
            .headerMarshaller(MarshallingType.INSTANT, HeaderMarshaller.INSTANT)
            .headerMarshaller(MarshallingType.NULL, JsonMarshaller.NULL)

            .queryParamMarshaller(MarshallingType.STRING, QueryParamMarshaller.STRING)
            .queryParamMarshaller(MarshallingType.INTEGER, QueryParamMarshaller.INTEGER)
            .queryParamMarshaller(MarshallingType.LONG, QueryParamMarshaller.LONG)
            .queryParamMarshaller(MarshallingType.DOUBLE, QueryParamMarshaller.DOUBLE)
            .queryParamMarshaller(MarshallingType.FLOAT, QueryParamMarshaller.FLOAT)
            .queryParamMarshaller(MarshallingType.BOOLEAN, QueryParamMarshaller.BOOLEAN)
            .queryParamMarshaller(MarshallingType.INSTANT, QueryParamMarshaller.INSTANT)
            .queryParamMarshaller(MarshallingType.LIST, QueryParamMarshaller.LIST)
            .queryParamMarshaller(MarshallingType.MAP, QueryParamMarshaller.MAP)
            .queryParamMarshaller(MarshallingType.NULL, JsonMarshaller.NULL)

            .pathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshaller.STRING)
            .pathParamMarshaller(MarshallingType.INTEGER, SimpleTypePathMarshaller.INTEGER)
            .pathParamMarshaller(MarshallingType.LONG, SimpleTypePathMarshaller.LONG)
            .pathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshaller.NULL)

            .greedyPathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshaller.GREEDY_STRING)
            .greedyPathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshaller.NULL)
            .build();
    }

    private static Map<MarshallLocation, TimestampFormatTrait.Format> getDefaultTimestampFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new HashMap<>();
        // TODO the default is supposedly rfc822. See JAVA-2949
        // We are using ISO_8601 in v1. Investigate which is the right format
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        formats.put(MarshallLocation.QUERY_PARAM, TimestampFormatTrait.Format.ISO_8601);
        return Collections.unmodifiableMap(formats);
    }

    private SdkHttpFullRequest.Builder fillBasicRequestParams(OperationInfo operationInfo) {
        return ProtocolUtils.createSdkHttpRequest(operationInfo, endpoint)
                            .applyMutation(b -> {
                                if (operationInfo.operationIdentifier() != null) {
                                    b.putHeader("X-Amz-Target", operationInfo.operationIdentifier());
                                }
                            });
    }

    /**
     * If there is not an explicit payload member then we need to start the implicit JSON request object. All
     * members bound to the payload will be added as fields to this object.
     */
    private void startMarshalling() {
        if (!hasExplicitPayloadMember) {
            jsonGenerator.writeStartObject();
        }
    }

    void doMarshall(SdkPojo pojo) {
        for (SdkField<?> field : pojo.sdkFields()) {
            Object val = field.getValueOrDefault(pojo);
            if (isBinary(field, val)) {
                request.contentStreamProvider(((SdkBytes) val)::asInputStream);
            } else {
                if (val != null && field.containsTrait(PayloadTrait.class)) {
                    jsonGenerator.writeStartObject();
                    doMarshall((SdkPojo) val);
                    jsonGenerator.writeEndObject();
                } else {
                    MARSHALLER_REGISTRY.getMarshaller(field.location(), field.marshallingType(), val)
                                       .marshall(val, marshallerContext, field.locationName(), (SdkField<Object>) field);
                }
            }
        }
    }

    private boolean isBinary(SdkField<?> field, Object val) {
        return isExplicitPayloadMember(field) && val instanceof SdkBytes;
    }

    private boolean isExplicitPayloadMember(SdkField<?> field) {
        return field.containsTrait(PayloadTrait.class);
    }

    @Override
    public SdkHttpFullRequest marshall(SdkPojo pojo) {
        startMarshalling();
        doMarshall(pojo);
        return finishMarshalling();
    }

    private SdkHttpFullRequest finishMarshalling() {
        // Content may already be set if the payload is binary data.
        if (request.contentStreamProvider() == null) {
            // End the implicit request object if needed.
            if (!hasExplicitPayloadMember) {
                jsonGenerator.writeEndObject();
            }

            byte[] content = jsonGenerator.getBytes();

            if (content != null) {
                request.contentStreamProvider(() -> new ByteArrayInputStream(content));
                if (content.length > 0) {
                    request.putHeader(CONTENT_LENGTH, Integer.toString(content.length));
                }
            }
        }

        // We skip setting the default content type if the request is streaming as
        // content-type is determined based on the body of the stream
        // TODO: !request.headers().containsKey(CONTENT_TYPE) does not work because request is created from line 77
        // and not from the original request
        if (!request.headers().containsKey(CONTENT_TYPE) && !hasEvent) {
            if (hasEventStreamingInput) {
                request.putHeader(CONTENT_TYPE, MIMETYPE_EVENT_STREAM);
            } else if (contentType != null && !hasStreamingInput && request.contentStreamProvider() != null) {
                request.putHeader(CONTENT_TYPE, contentType);
            }
        }

        return request.build();
    }

}
