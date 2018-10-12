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

package software.amazon.awssdk.core.internal.protocol.query;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.SdkField;
import software.amazon.awssdk.core.protocol.SdkPojo;
import software.amazon.awssdk.core.protocol.traits.DefaultValueTrait;
import software.amazon.awssdk.core.protocol.traits.ListTrait;
import software.amazon.awssdk.core.protocol.traits.MapTrait;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.UriResourcePathUtils;

/**
 * Implementation of {@link ProtocolMarshaller} for AWS Query services.
 *
 * @param <OrigRequestT> Type of the original request object.
 */
@SdkProtectedApi
public final class QueryProtocolMarshaller<OrigRequestT>
    implements ProtocolMarshaller<Request<OrigRequestT>> {

    private static final QueryMarshallerRegistry MARSHALLER_REGISTRY = QueryMarshallerRegistry
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
        .marshaller(MarshallingType.MAP, new MapMarshaller())
        .marshaller(MarshallingType.LIST, new ListMarshaller())
        .marshaller(MarshallingType.SDK_POJO, (req, path, val, sdkField) -> doMarshall(path, req, val))
        .build();

    private final Request<OrigRequestT> request;

    public QueryProtocolMarshaller(OperationInfo operationInfo, OrigRequestT originalRequest) {
        this.request = fillBasicRequestParams(operationInfo, originalRequest);
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
        doMarshall(null, request, pojo);
        return request;
    }

    private static void doMarshall(String path, Request<?> request, SdkPojo pojo) {
        for (SdkField<?> sdkField : pojo.sdkFields()) {
            Object val = resolveValue(sdkField.get(pojo), sdkField);
            QueryMarshaller<Object> marshaller = getMarshaller(sdkField, val);
            marshaller.marshall(request, resolvePath(path, sdkField), val, (SdkField<Object>) sdkField);
        }
    }

    private static Object resolveValue(Object val, SdkField<?> sdkField) {
        DefaultValueTrait trait = sdkField.getTrait(DefaultValueTrait.class);
        return trait == null ? val : trait.resolveValue(val);
    }

    private static QueryMarshaller<Object> getMarshaller(SdkField<?> sdkField, Object val) {
        QueryMarshaller<Object> marshaller = MARSHALLER_REGISTRY.getMarshaller(sdkField.marshallingType(), val);
        if (marshaller == null) {
            throw SdkClientException.create("No marshaller found for type -" + sdkField.marshallingType());
        }
        return marshaller;
    }

    private static String resolvePath(String path, SdkField<?> sdkField) {
        return path == null ? sdkField.locationName() : path + "." + sdkField.locationName();
    }

    private static class MapMarshaller implements QueryMarshaller<Map<String, ?>> {

        @Override
        public void marshall(Request<?> request, String path, Map<String, ?> val, SdkField<Map<String, ?>> sdkField) {
            MapTrait mapTrait = sdkField.getTrait(MapTrait.class);
            AtomicInteger entryNum = new AtomicInteger(1);
            val.forEach((key, value) -> {

                String mapKeyPath = resolveMapPath(path, mapTrait, entryNum, mapTrait.keyLocationName());

                request.addParameter(mapKeyPath, key);

                String mapValuePath = resolveMapPath(path, mapTrait, entryNum, mapTrait.valueLocationName());

                getMarshaller(mapTrait.valueFieldInfo(), val)
                    .marshall(request, mapValuePath, value, mapTrait.valueFieldInfo());
                entryNum.incrementAndGet();
            });
        }

        private static String resolveMapPath(String path, MapTrait mapTrait, AtomicInteger entryNum, String s) {
            return mapTrait.isFlattened() ?
                   String.format("%s.%d.%s", path, entryNum.get(), s) :
                   String.format("%s.entry.%d.%s", path, entryNum.get(), s);
        }
    }

    /**
     * Marshaller for list types.
     */
    private static class ListMarshaller implements QueryMarshaller<List<?>> {

        @Override
        public void marshall(Request<?> request, String path, List<?> val, SdkField<List<?>> sdkField) {
            // Explicitly empty lists are marshalled as a query param with empty value in AWS/Query
            if (val.isEmpty() && !(val instanceof SdkAutoConstructList)) {
                request.addParameter(path, "");
                return;
            }
            for (int i = 0; i < val.size(); i++) {
                ListTrait listTrait = sdkField.getTrait(ListTrait.class);
                String listPath = resolveListPath(path, i, listTrait);
                getMarshaller(listTrait.memberFieldInfo(), val)
                    .marshall(request, listPath, val.get(i), listTrait.memberFieldInfo());
            }
        }

        private String resolveListPath(String path, int i, ListTrait listTrait) {
            return listTrait.isFlattened() ?
                   String.format("%s.%d", path, i + 1) :
                   String.format("%s.%s.%d", path, listTrait.memberFieldInfo().locationName(), i + 1);
        }
    }
}
