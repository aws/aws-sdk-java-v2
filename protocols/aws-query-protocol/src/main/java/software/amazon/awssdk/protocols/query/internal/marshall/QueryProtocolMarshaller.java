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

package software.amazon.awssdk.protocols.query.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.util.UriResourcePathUtils;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;

/**
 * Implementation of {@link ProtocolMarshaller} for AWS Query services.
 *
 * @param <OrigRequestT> Type of the original request object.
 */
@SdkInternalApi
public final class QueryProtocolMarshaller<OrigRequestT>
    implements ProtocolMarshaller<Request<OrigRequestT>> {

    private static final QueryMarshallerRegistry AWS_QUERY_MARSHALLER_REGISTRY = commonRegistry()
        .marshaller(MarshallingType.LIST, ListQueryMarshaller.awsQuery())
        .build();

    private static final QueryMarshallerRegistry EC2_QUERY_MARSHALLER_REGISTRY = commonRegistry()
        .marshaller(MarshallingType.LIST, ListQueryMarshaller.ec2Query())
        .build();

    private final Request<OrigRequestT> request;
    private final QueryMarshallerRegistry registry;

    private QueryProtocolMarshaller(Builder<OrigRequestT> builder) {
        this.request = fillBasicRequestParams(builder.operationInfo, builder.originalRequest);
        this.registry = builder.isEc2 ? EC2_QUERY_MARSHALLER_REGISTRY : AWS_QUERY_MARSHALLER_REGISTRY;
    }

    private Request<OrigRequestT> fillBasicRequestParams(OperationInfo operationInfo, OrigRequestT originalRequest) {
        Request<OrigRequestT> request = new DefaultRequest<>(originalRequest, operationInfo.serviceName());
        request.setHttpMethod(operationInfo.httpMethodName());
        // AWS/Query doesn't have a resource path so we just use empty string
        request.setResourcePath(UriResourcePathUtils.addStaticQueryParametersToRequest(request, ""));
        request.addParameter("Action", operationInfo.operationIdentifier());
        request.addParameter("Version", operationInfo.apiVersion());
        return request;
    }

    @Override
    public Request<OrigRequestT> marshall(SdkPojo pojo) {
        QueryMarshallerContext context = QueryMarshallerContext.builder()
                                                               .request(request)
                                                               .protocolHandler(this)
                                                               .marshallerRegistry(registry)
                                                               .build();
        doMarshall(null, context, pojo);
        return request;
    }

    private void doMarshall(String path, QueryMarshallerContext context, SdkPojo pojo) {
        for (SdkField<?> sdkField : pojo.sdkFields()) {
            Object val = sdkField.getValueOrDefault(pojo);
            QueryMarshaller<Object> marshaller = registry.getMarshaller(sdkField.marshallingType(), val);
            marshaller.marshall(context, resolvePath(path, sdkField), val, (SdkField<Object>) sdkField);
        }
    }

    private static String resolvePath(String path, SdkField<?> sdkField) {
        return path == null ? sdkField.locationName() : path + "." + sdkField.locationName();
    }

    public static <T> Builder<T> builder(T origRequest) {
        return new Builder<>(origRequest);
    }

    private static QueryMarshallerRegistry.Builder commonRegistry() {
        return QueryMarshallerRegistry
            .builder()
            .marshaller(MarshallingType.STRING, SimpleTypeQueryMarshaller.STRING)
            .marshaller(MarshallingType.INTEGER, SimpleTypeQueryMarshaller.INTEGER)
            .marshaller(MarshallingType.FLOAT, SimpleTypeQueryMarshaller.FLOAT)
            .marshaller(MarshallingType.BOOLEAN, SimpleTypeQueryMarshaller.BOOLEAN)
            .marshaller(MarshallingType.DOUBLE, SimpleTypeQueryMarshaller.DOUBLE)
            .marshaller(MarshallingType.LONG, SimpleTypeQueryMarshaller.LONG)
            .marshaller(MarshallingType.INSTANT, SimpleTypeQueryMarshaller.INSTANT)
            .marshaller(MarshallingType.SDK_BYTES, SimpleTypeQueryMarshaller.SDK_BYTES)
            .marshaller(MarshallingType.NULL, SimpleTypeQueryMarshaller.NULL)
            .marshaller(MarshallingType.MAP, new MapQueryMarshaller())
            .marshaller(MarshallingType.SDK_POJO, (context, path, val, sdkField) ->
                context.protocolHandler().doMarshall(path, context, val));
    }

    public static final class Builder<T> {

        private final T originalRequest;
        private OperationInfo operationInfo;
        private boolean isEc2;

        private Builder(T originalRequest) {
            this.originalRequest = originalRequest;
        }

        public Builder<T> operationInfo(OperationInfo operationInfo) {
            this.operationInfo = operationInfo;
            return this;
        }

        public Builder<T> isEc2(boolean ec2) {
            isEc2 = ec2;
            return this;
        }

        public QueryProtocolMarshaller<T> build() {
            return new QueryProtocolMarshaller<>(this);
        }
    }

}
