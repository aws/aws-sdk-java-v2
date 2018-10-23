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

package software.amazon.awssdk.protocols.json.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;

/**
 * Builder to create an appropriate implementation of {@link ProtocolMarshaller} for JSON based services.
 *
 * @param <T> Type of the original request object.
 */
@SdkInternalApi
public class JsonProtocolMarshallerBuilder<T> {

    private StructuredJsonGenerator jsonGenerator;
    private String contentType;
    private OperationInfo operationInfo;
    private boolean sendExplicitNullForPayload;
    private T originalRequest;

    public static <T> JsonProtocolMarshallerBuilder<T> standard() {
        return new JsonProtocolMarshallerBuilder<>();
    }

    public JsonProtocolMarshallerBuilder<T> jsonGenerator(StructuredJsonGenerator jsonGenerator) {
        this.jsonGenerator = jsonGenerator;
        return this;
    }

    public JsonProtocolMarshallerBuilder<T> contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public JsonProtocolMarshallerBuilder<T> operationInfo(OperationInfo operationInfo) {
        this.operationInfo = operationInfo;
        return this;
    }

    /**
     * @param sendExplicitNullForPayload True if an explicit JSON null should be sent as the body when the
     *                                   payload member is null. See {@link NullAsEmptyBodyProtocolRequestMarshaller}.
     */
    public JsonProtocolMarshallerBuilder<T> sendExplicitNullForPayload(boolean sendExplicitNullForPayload) {
        this.sendExplicitNullForPayload = sendExplicitNullForPayload;
        return this;
    }

    public JsonProtocolMarshallerBuilder<T> originalRequest(T originalRequest) {
        this.originalRequest = originalRequest;
        return this;
    }

    public ProtocolMarshaller<Request<T>> build() {
        ProtocolMarshaller<Request<T>> protocolMarshaller = new JsonProtocolMarshaller<>(jsonGenerator,
                                                                                         contentType,
                                                                                         operationInfo,
                                                                                         originalRequest);
        return sendExplicitNullForPayload ? protocolMarshaller
                                          : new NullAsEmptyBodyProtocolRequestMarshaller<>(protocolMarshaller);
    }
}
