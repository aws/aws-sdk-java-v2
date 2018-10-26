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

package software.amazon.awssdk.protocols.xml;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlGenerator;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.xml.internal.unmarshall.AwsXmlResponseHandler;
import software.amazon.awssdk.protocols.xml.internal.unmarshall.XmlOperationMetadata;
import software.amazon.awssdk.protocols.xml.internal.unmarshall.XmlProtocolUnmarshaller;

/**
 * Factory to generate the various protocol handlers and generators
 * to be used for communicating with xml services.
 */
@SdkProtectedApi
public final class AwsXmlProtocolFactory {

    private AwsXmlProtocolFactory() {
    }

    /**
     * Creates an instance of {@link XmlProtocolMarshaller} to be used for marshalling the requess.
     *
     * @param operationInfo Info required to marshall the request
     * @param origRequest The original request to marshall
     * @param rootElement The root of the xml document if present. See {@link XmlProtocolMarshallerBuilder#rootElement(String)}.
     * @param xmlNameSpaceUri The XML namespace to include in the xmlns attribute of the root element.
     */
    public <T extends AwsRequest> ProtocolMarshaller<Request<T>> createProtocolMarshaller(OperationInfo operationInfo,
                                                                                          T origRequest,
                                                                                          String rootElement,
                                                                                          String xmlNameSpaceUri) {
        return XmlProtocolMarshallerBuilder.<T>builder()
            .xmlGenerator(createGenerator(operationInfo, xmlNameSpaceUri))
            .originalRequest(origRequest)
            .operationInfo(operationInfo)
            .rootElement(rootElement)
            .build();
    }

    public <T extends AwsResponse> HttpResponseHandler<T> createResponseHandler(Supplier<SdkPojo> pojoSupplier,
                                                                                XmlOperationMetadata staxOperationMetadata) {
        return new AwsXmlResponseHandler<>(
            new XmlProtocolUnmarshaller<>(staxOperationMetadata.useRootElement()), r -> pojoSupplier.get(),
            staxOperationMetadata.isHasStreamingSuccessResponse());
    }

    private XmlGenerator createGenerator(OperationInfo operationInfo, String xmlNameSpaceUri) {
        return operationInfo.hasPayloadMembers() ? XmlGenerator.create(xmlNameSpaceUri) : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AwsXmlProtocolFactory}.
     */
    public static final class Builder {

        public AwsXmlProtocolFactory build() {
            return new AwsXmlProtocolFactory();
        }
    }
}