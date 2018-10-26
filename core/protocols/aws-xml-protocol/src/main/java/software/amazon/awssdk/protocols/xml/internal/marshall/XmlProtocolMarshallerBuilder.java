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

package software.amazon.awssdk.protocols.xml.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;

/**
 * Builder to create an appropriate implementation of {@link ProtocolMarshaller} for Xml based services.
 *
 * @param <T> Type of the original request object.
 */
@SdkInternalApi
public class XmlProtocolMarshallerBuilder<T extends SdkRequest>  {

    private XmlGenerator xmlGenerator;
    private OperationInfo operationInfo;
    private T originalRequest;
    private String rootElement;

    public static <T extends SdkRequest> XmlProtocolMarshallerBuilder<T> builder() {
        return new XmlProtocolMarshallerBuilder<T>();
    }

    public XmlProtocolMarshaller<T> build() {
        return new XmlProtocolMarshaller<T>(xmlGenerator, originalRequest, operationInfo, rootElement);
    }

    public XmlProtocolMarshallerBuilder<T> xmlGenerator(XmlGenerator xmlGenerator) {
        this.xmlGenerator = xmlGenerator;
        return this;
    }

    public XmlProtocolMarshallerBuilder<T> originalRequest(T originalRequest) {
        this.originalRequest = originalRequest;
        return this;
    }

    public XmlProtocolMarshallerBuilder<T> operationInfo(OperationInfo operationInfo) {
        this.operationInfo = operationInfo;
        return this;
    }

    /**
     * Some services like Route53 specifies the location for the request shape. This should be the root of the
     * generated xml document.
     *
     * Other services Cloudfront, s3 don't specify location param for the request shape. For them, this value will be null.
     */
    public XmlProtocolMarshallerBuilder<T> rootElement(String rootElement) {
        this.rootElement = rootElement;
        return this;
    }
}
