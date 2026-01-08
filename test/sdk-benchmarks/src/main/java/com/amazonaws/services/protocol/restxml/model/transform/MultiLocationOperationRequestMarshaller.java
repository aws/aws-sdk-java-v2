/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.services.protocol.restxml.model.transform;

import static com.amazonaws.util.StringUtils.UTF8;

import java.io.StringWriter;



import com.amazonaws.SdkClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.restxml.model.*;
import com.amazonaws.transform.Marshaller;

import com.amazonaws.util.StringInputStream;
import com.amazonaws.util.StringUtils;

import com.amazonaws.util.XMLWriter;

/**
 * MultiLocationOperationRequest Marshaller
 */


public class MultiLocationOperationRequestMarshaller implements Marshaller<Request<MultiLocationOperationRequest>, MultiLocationOperationRequest> {

    public Request<MultiLocationOperationRequest> marshall(MultiLocationOperationRequest multiLocationOperationRequest) {

        if (multiLocationOperationRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<MultiLocationOperationRequest> request = new DefaultRequest<MultiLocationOperationRequest>(multiLocationOperationRequest,
                "AmazonProtocolRestXml");

        request.setHttpMethod(HttpMethodName.POST);

        if (multiLocationOperationRequest.getStringHeaderMember() != null) {
            request.addHeader("x-amz-header-string", StringUtils.fromString(multiLocationOperationRequest.getStringHeaderMember()));
        }

        if (multiLocationOperationRequest.getTimestampHeaderMember() != null) {
            request.addHeader("x-amz-timearg", StringUtils.fromDate(multiLocationOperationRequest.getTimestampHeaderMember()));
        }

        String uriResourcePath = "/2016-03-11/multiLocationOperation/{PathParam}";

        uriResourcePath = com.amazonaws.transform.PathMarshallers.NON_GREEDY.marshall(uriResourcePath, "PathParam",
                multiLocationOperationRequest.getPathParam());
        request.setResourcePath(uriResourcePath);

        if (multiLocationOperationRequest.getQueryParamOne() != null) {
            request.addParameter("QueryParamOne", StringUtils.fromString(multiLocationOperationRequest.getQueryParamOne()));
        }

        if (multiLocationOperationRequest.getQueryParamTwo() != null) {
            request.addParameter("QueryParamTwo", StringUtils.fromString(multiLocationOperationRequest.getQueryParamTwo()));
        }

        try {
            StringWriter stringWriter = new StringWriter();
            XMLWriter xmlWriter = new XMLWriter(stringWriter, "https://restxml/");

            xmlWriter.startElement("MultiLocationOperationRequest");
            if (multiLocationOperationRequest != null) {

                {
                    PayloadStructType payloadStructParam = multiLocationOperationRequest.getPayloadStructParam();
                    if (payloadStructParam != null) {
                        xmlWriter.startElement("PayloadStructParam");

                        if (payloadStructParam.getPayloadMemberOne() != null) {
                            xmlWriter.startElement("PayloadMemberOne").value(payloadStructParam.getPayloadMemberOne()).endElement();
                        }

                        if (payloadStructParam.getPayloadMemberTwo() != null) {
                            xmlWriter.startElement("PayloadMemberTwo").value(payloadStructParam.getPayloadMemberTwo()).endElement();
                        }
                        xmlWriter.endElement();
                    }
                }
            }
            xmlWriter.endElement();

            request.setContent(new StringInputStream(stringWriter.getBuffer().toString()));
            request.addHeader("Content-Length", Integer.toString(stringWriter.getBuffer().toString().getBytes(UTF8).length));
            if (!request.getHeaders().containsKey("Content-Type")) {
                request.addHeader("Content-Type", "application/xml");
            }
        } catch (Throwable t) {
            throw new SdkClientException("Unable to marshall request to XML: " + t.getMessage(), t);
        }

        return request;
    }

}
