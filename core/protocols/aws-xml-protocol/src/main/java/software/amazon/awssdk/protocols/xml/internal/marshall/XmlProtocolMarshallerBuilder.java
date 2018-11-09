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

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;

/**
 * Builder to create an appropriate implementation of {@link ProtocolMarshaller} for Xml based services.
 */
@SdkInternalApi
public class XmlProtocolMarshallerBuilder  {

    private URI endpoint;
    private XmlGenerator xmlGenerator;
    private OperationInfo operationInfo;
    private String rootElement;

    public static XmlProtocolMarshallerBuilder builder() {
        return new XmlProtocolMarshallerBuilder();
    }

    public XmlProtocolMarshaller build() {
        return new XmlProtocolMarshaller(endpoint, xmlGenerator, operationInfo, rootElement);
    }

    public XmlProtocolMarshallerBuilder endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public XmlProtocolMarshallerBuilder xmlGenerator(XmlGenerator xmlGenerator) {
        this.xmlGenerator = xmlGenerator;
        return this;
    }

    public XmlProtocolMarshallerBuilder operationInfo(OperationInfo operationInfo) {
        this.operationInfo = operationInfo;
        return this;
    }

    /**
     * Some services like Route53 specifies the location for the request shape. This should be the root of the
     * generated xml document.
     *
     * Other services Cloudfront, s3 don't specify location param for the request shape. For them, this value will be null.
     */
    public XmlProtocolMarshallerBuilder rootElement(String rootElement) {
        this.rootElement = rootElement;
        return this;
    }
}
