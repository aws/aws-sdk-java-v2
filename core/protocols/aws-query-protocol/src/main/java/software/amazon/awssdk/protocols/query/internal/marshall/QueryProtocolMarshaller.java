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

package software.amazon.awssdk.protocols.query.internal.marshall;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.core.ProtocolUtils;

/**
 * Implementation of {@link ProtocolMarshaller} for AWS Query services.
 */
@SdkInternalApi
public final class QueryProtocolMarshaller
    implements ProtocolMarshaller<SdkHttpFullRequest> {

    private static final QueryMarshallerRegistry AWS_QUERY_MARSHALLER_REGISTRY = commonRegistry()
        .marshaller(MarshallingType.LIST, ListQueryMarshaller.awsQuery())
        .build();

    private static final QueryMarshallerRegistry EC2_QUERY_MARSHALLER_REGISTRY = commonRegistry()
        .marshaller(MarshallingType.LIST, ListQueryMarshaller.ec2Query())
        .build();

    private final SdkHttpFullRequest.Builder request;
    private final QueryMarshallerRegistry registry;
    private final URI endpoint;

    private QueryProtocolMarshaller(Builder builder) {
        this.endpoint = builder.endpoint;
        this.request = fillBasicRequestParams(builder.operationInfo);
        this.registry = builder.isEc2 ? EC2_QUERY_MARSHALLER_REGISTRY : AWS_QUERY_MARSHALLER_REGISTRY;
    }

    private SdkHttpFullRequest.Builder fillBasicRequestParams(OperationInfo operationInfo) {
        return ProtocolUtils.createSdkHttpRequest(operationInfo, endpoint)
                            .encodedPath("")
                            .putRawQueryParameter("Action", operationInfo.operationIdentifier())
                            .putRawQueryParameter("Version", operationInfo.apiVersion());
    }

    @Override
    public SdkHttpFullRequest marshall(SdkPojo pojo) {
        QueryMarshallerContext context = QueryMarshallerContext.builder()
                                                               .request(request)
                                                               .protocolHandler(this)
                                                               .marshallerRegistry(registry)
                                                               .build();
        doMarshall(null, context, pojo);
        return request.build();
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

    /**
     * @return New {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link QueryProtocolMarshaller}.
     */
    public static final class Builder {

        private OperationInfo operationInfo;
        private boolean isEc2;
        private URI endpoint;

        /**
         * @param operationInfo Metadata about the operation like URI, HTTP method, etc.
         * @return This builder for method chaining.
         */
        public Builder operationInfo(OperationInfo operationInfo) {
            this.operationInfo = operationInfo;
            return this;
        }

        /**
         * @param ec2 True if the service is EC2. EC2 has some slightly different behavior so we wire things up
         * a bit differently for it.
         * @return This builder for method chaining.
         */
        public Builder isEc2(boolean ec2) {
            isEc2 = ec2;
            return this;
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
         * @return New instance of {@link QueryProtocolMarshaller}.
         */
        public QueryProtocolMarshaller build() {
            return new QueryProtocolMarshaller(this);
        }
    }

}
