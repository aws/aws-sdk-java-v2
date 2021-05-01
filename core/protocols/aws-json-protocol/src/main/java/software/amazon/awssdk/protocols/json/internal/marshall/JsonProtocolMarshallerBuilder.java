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
public final class JsonProtocolMarshallerBuilder {

    private URI endpoint;
    private StructuredJsonGenerator jsonGenerator;
    private String contentType;
    private OperationInfo operationInfo;
    private boolean sendExplicitNullForPayload;

    private JsonProtocolMarshallerBuilder() {
    }

    /**
     * @return New instance of {@link JsonProtocolMarshallerBuilder}.
     */
    public static JsonProtocolMarshallerBuilder create() {
        return new JsonProtocolMarshallerBuilder();
    }

    /**
     * @param endpoint Endpoint to set on the marshalled request.
     * @return This builder for method chaining.
     */
    public JsonProtocolMarshallerBuilder endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Sets the implementation of {@link StructuredJsonGenerator} which allows writing JSON or JSON like (i.e. CBOR and Ion)
     * data formats.
     *
     * @param jsonGenerator Generator to use.
     * @return This builder for method chaining.
     */
    public JsonProtocolMarshallerBuilder jsonGenerator(StructuredJsonGenerator jsonGenerator) {
        this.jsonGenerator = jsonGenerator;
        return this;
    }

    /**
     * @param contentType The content type to set on the marshalled requests.
     * @return This builder for method chaining.
     */
    public JsonProtocolMarshallerBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * @param operationInfo Metadata about the operation like URI, HTTP method, etc.
     * @return This builder for method chaining.
     */
    public JsonProtocolMarshallerBuilder operationInfo(OperationInfo operationInfo) {
        this.operationInfo = operationInfo;
        return this;
    }

    /**
     * @param sendExplicitNullForPayload True if an explicit JSON null should be sent as the body when the
     * payload member is null. See {@link NullAsEmptyBodyProtocolRequestMarshaller}.
     */
    public JsonProtocolMarshallerBuilder sendExplicitNullForPayload(boolean sendExplicitNullForPayload) {
        this.sendExplicitNullForPayload = sendExplicitNullForPayload;
        return this;
    }

    /**
     * @return New instance of {@link ProtocolMarshaller}. If {@link #sendExplicitNullForPayload} is true then the marshaller
     * will be wrapped with {@link NullAsEmptyBodyProtocolRequestMarshaller}.
     */
    public ProtocolMarshaller<SdkHttpFullRequest> build() {
        ProtocolMarshaller<SdkHttpFullRequest> protocolMarshaller = new JsonProtocolMarshaller(endpoint,
                                                                                               jsonGenerator,
                                                                                               contentType,
                                                                                               operationInfo);
        return sendExplicitNullForPayload ? protocolMarshaller
                                          : new NullAsEmptyBodyProtocolRequestMarshaller(protocolMarshaller);
    }
}
