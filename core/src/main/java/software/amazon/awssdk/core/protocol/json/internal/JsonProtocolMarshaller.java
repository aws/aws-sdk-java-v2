/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.protocol.json.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.AmazonWebServiceRequest;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.protocol.MarshallingInfo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;
import software.amazon.awssdk.core.util.UriResourcePathUtils;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Implementation of {@link ProtocolMarshaller} for JSON based services. This includes JSON-RPC and REST-JSON.
 *
 * @param <OrigRequestT> Type of the original request object.
 */
@SdkInternalApi
public class JsonProtocolMarshaller<OrigRequestT> implements ProtocolRequestMarshaller<OrigRequestT> {

    private static final MarshallerRegistry MARSHALLER_REGISTRY = createMarshallerRegistry();

    private final StructuredJsonGenerator jsonGenerator;
    private final Request<OrigRequestT> request;
    private final String contentType;
    private final boolean hasExplicitPayloadMember;

    private final JsonMarshallerContext marshallerContext;

    public JsonProtocolMarshaller(StructuredJsonGenerator jsonGenerator,
                                  String contentType,
                                  OperationInfo operationInfo,
                                  OrigRequestT originalRequest) {
        this.jsonGenerator = jsonGenerator;
        this.contentType = contentType;
        this.hasExplicitPayloadMember = operationInfo.hasExplicitPayloadMember();
        this.request = fillBasicRequestParams(operationInfo, originalRequest);
        this.marshallerContext = JsonMarshallerContext.builder()
                .jsonGenerator(jsonGenerator)
                .marshallerRegistry(MARSHALLER_REGISTRY)
                .protocolHandler(this)
                .request(request)
                .build();
    }

    private Request<OrigRequestT> fillBasicRequestParams(OperationInfo operationInfo, OrigRequestT originalRequest) {
        Request<OrigRequestT> request = createRequest(operationInfo, originalRequest);
        request.setHttpMethod(operationInfo.httpMethodName());
        request.setResourcePath(UriResourcePathUtils.addStaticQueryParametersToRequest(request, operationInfo.requestUri()));
        if (operationInfo.operationIdentifier() != null) {
            request.addHeader("X-Amz-Target", operationInfo.operationIdentifier());
        }
        return request;
    }

    private DefaultRequest<OrigRequestT> createRequest(OperationInfo operationInfo, OrigRequestT originalRequest) {
        if (originalRequest instanceof AmazonWebServiceRequest) {
            return new DefaultRequest<>((AmazonWebServiceRequest) originalRequest, operationInfo.serviceName());
        } else {
            return new DefaultRequest<>(operationInfo.serviceName());
        }
    }

    private static MarshallerRegistry createMarshallerRegistry() {
        return MarshallerRegistry.builder()
                .payloadMarshaller(MarshallingType.STRING, SimpleTypeJsonMarshallers.STRING)
                .payloadMarshaller(MarshallingType.INTEGER, SimpleTypeJsonMarshallers.INTEGER)
                .payloadMarshaller(MarshallingType.LONG, SimpleTypeJsonMarshallers.LONG)
                .payloadMarshaller(MarshallingType.DOUBLE, SimpleTypeJsonMarshallers.DOUBLE)
                .payloadMarshaller(MarshallingType.FLOAT, SimpleTypeJsonMarshallers.FLOAT)
                .payloadMarshaller(MarshallingType.BIG_DECIMAL, SimpleTypeJsonMarshallers.BIG_DECIMAL)
                .payloadMarshaller(MarshallingType.BOOLEAN, SimpleTypeJsonMarshallers.BOOLEAN)
                .payloadMarshaller(MarshallingType.INSTANT, SimpleTypeJsonMarshallers.INSTANT)
                .payloadMarshaller(MarshallingType.BYTE_BUFFER, SimpleTypeJsonMarshallers.BYTE_BUFFER)
                .payloadMarshaller(MarshallingType.STRUCTURED, SimpleTypeJsonMarshallers.STRUCTURED)
                .payloadMarshaller(MarshallingType.LIST, SimpleTypeJsonMarshallers.LIST)
                .payloadMarshaller(MarshallingType.MAP, SimpleTypeJsonMarshallers.MAP)
                .payloadMarshaller(MarshallingType.NULL, SimpleTypeJsonMarshallers.NULL)

                .headerMarshaller(MarshallingType.STRING, HeaderMarshallers.STRING)
                .headerMarshaller(MarshallingType.INTEGER, HeaderMarshallers.INTEGER)
                .headerMarshaller(MarshallingType.LONG, HeaderMarshallers.LONG)
                .headerMarshaller(MarshallingType.DOUBLE, HeaderMarshallers.DOUBLE)
                .headerMarshaller(MarshallingType.FLOAT, HeaderMarshallers.FLOAT)
                .headerMarshaller(MarshallingType.BOOLEAN, HeaderMarshallers.BOOLEAN)
                .headerMarshaller(MarshallingType.INSTANT, HeaderMarshallers.INSTANT)
                .headerMarshaller(MarshallingType.NULL, JsonMarshaller.NULL)

                .queryParamMarshaller(MarshallingType.STRING, QueryParamMarshallers.STRING)
                .queryParamMarshaller(MarshallingType.INTEGER, QueryParamMarshallers.INTEGER)
                .queryParamMarshaller(MarshallingType.LONG, QueryParamMarshallers.LONG)
                .queryParamMarshaller(MarshallingType.DOUBLE, QueryParamMarshallers.DOUBLE)
                .queryParamMarshaller(MarshallingType.FLOAT, QueryParamMarshallers.FLOAT)
                .queryParamMarshaller(MarshallingType.BOOLEAN, QueryParamMarshallers.BOOLEAN)
                .queryParamMarshaller(MarshallingType.INSTANT, QueryParamMarshallers.INSTANT)
                .queryParamMarshaller(MarshallingType.LIST, QueryParamMarshallers.LIST)
                .queryParamMarshaller(MarshallingType.MAP, QueryParamMarshallers.MAP)
                .queryParamMarshaller(MarshallingType.NULL, JsonMarshaller.NULL)

                .pathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshallers.STRING)
                .pathParamMarshaller(MarshallingType.INTEGER, SimpleTypePathMarshallers.INTEGER)
                .pathParamMarshaller(MarshallingType.LONG, SimpleTypePathMarshallers.LONG)
                .pathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshallers.NULL)

                .greedyPathParamMarshaller(MarshallingType.STRING, SimpleTypePathMarshallers.GREEDY_STRING)
                .greedyPathParamMarshaller(MarshallingType.NULL, SimpleTypePathMarshallers.NULL)
                .build();
    }

    /**
     * If there is not an explicit payload member then we need to start the implicit JSON request object. All
     * members bound to the payload will be added as fields to this object.
     */
    @Override
    public void startMarshalling() {
        if (!hasExplicitPayloadMember) {
            jsonGenerator.writeStartObject();
        }
    }

    @Override
    public <V> void marshall(V val, MarshallingInfo<V> marshallingInfo) {
        doMarshall(resolveValue(val, marshallingInfo), marshallingInfo);
    }

    /**
     * @return The original value if non-null, or if value is null and a default value {@link java.util.function.Supplier}
     *     is present return the default value. Otherwise return null.
     */
    private <V> V resolveValue(V val, MarshallingInfo<V> marshallingInfo) {
        return val == null && marshallingInfo.defaultValueSupplier() != null ? marshallingInfo.defaultValueSupplier().get() : val;
    }

    private <V> void doMarshall(V val, MarshallingInfo<V> marshallingInfo) {
        if (marshallingInfo.isBinary()) {
            marshallBinaryPayload(val);
        } else {
            MARSHALLER_REGISTRY.getMarshaller(marshallingInfo.marshallLocation(), marshallingInfo.marshallingType(), val)
                    .marshall(val, marshallerContext, marshallingInfo.marshallLocationName());
        }
    }

    /**
     * Binary data should be placed as is, directly into the content.
     */
    private void marshallBinaryPayload(Object val) {
        if (val instanceof ByteBuffer) {
            request.setContent(BinaryUtils.toStream((ByteBuffer) val));
        } else if (val instanceof InputStream) {
            request.setContent((InputStream) val);
        }
    }

    @Override
    public Request<OrigRequestT> finishMarshalling() {
        // Content may already be set if the payload is binary data.
        if (request.getContent() == null) {
            // End the implicit request object if needed.
            if (!hasExplicitPayloadMember) {
                jsonGenerator.writeEndObject();
            }

            byte[] content = jsonGenerator.getBytes();
            request.setContent(new ByteArrayInputStream(content));
            if (content.length > 0) {
                request.addHeader("Content-Length", Integer.toString(content.length));
            }
        }
        if (!request.getHeaders().containsKey("Content-Type")) {
            request.addHeader("Content-Type", contentType);
        }
        return request;
    }

}
