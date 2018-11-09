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

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;

/**
 * Builder to create an appropriate implementation of {@link ProtocolMarshaller} for JSON based services.
 */
@SdkInternalApi
public class JsonProtocolMarshallerBuilder {

    private URI endpoint;
    private StructuredJsonGenerator jsonGenerator;
    private String contentType;
    private OperationInfo operationInfo;
    private boolean sendExplicitNullForPayload;

    public static JsonProtocolMarshallerBuilder standard() {
        return new JsonProtocolMarshallerBuilder();
    }

    public JsonProtocolMarshallerBuilder endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public JsonProtocolMarshallerBuilder jsonGenerator(StructuredJsonGenerator jsonGenerator) {
        this.jsonGenerator = jsonGenerator;
        return this;
    }

    public JsonProtocolMarshallerBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public JsonProtocolMarshallerBuilder operationInfo(OperationInfo operationInfo) {
        this.operationInfo = operationInfo;
        return this;
    }

    /**
     * @param sendExplicitNullForPayload True if an explicit JSON null should be sent as the body when the
     *                                   payload member is null. See {@link NullAsEmptyBodyProtocolRequestMarshaller}.
     */
    public JsonProtocolMarshallerBuilder sendExplicitNullForPayload(boolean sendExplicitNullForPayload) {
        this.sendExplicitNullForPayload = sendExplicitNullForPayload;
        return this;
    }

    public ProtocolMarshaller<SdkHttpFullRequest> build() {
        ProtocolMarshaller<SdkHttpFullRequest> protocolMarshaller = new JsonProtocolMarshaller(
            endpoint,
            jsonGenerator,
            contentType,
            operationInfo);
        return sendExplicitNullForPayload ? protocolMarshaller
                                          : new NullAsEmptyBodyProtocolRequestMarshaller(protocolMarshaller);
    }
}
