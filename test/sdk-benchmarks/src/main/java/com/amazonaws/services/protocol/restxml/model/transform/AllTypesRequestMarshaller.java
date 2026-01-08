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

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.amazonaws.SdkClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.restxml.model.*;
import com.amazonaws.transform.Marshaller;

import com.amazonaws.util.StringInputStream;

import com.amazonaws.util.XMLWriter;

/**
 * AllTypesRequest Marshaller
 */


public class AllTypesRequestMarshaller implements Marshaller<Request<AllTypesRequest>, AllTypesRequest> {

    public Request<AllTypesRequest> marshall(AllTypesRequest allTypesRequest) {

        if (allTypesRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<AllTypesRequest> request = new DefaultRequest<AllTypesRequest>(allTypesRequest, "AmazonProtocolRestXml");

        request.setHttpMethod(HttpMethodName.POST);

        String uriResourcePath = "/2016-03-11/allTypes";

        request.setResourcePath(uriResourcePath);

        try {
            StringWriter stringWriter = new StringWriter();
            XMLWriter xmlWriter = new XMLWriter(stringWriter, "https://restxml/");

            xmlWriter.startElement("AllTypesRequest");
            if (allTypesRequest != null) {

                if (allTypesRequest.getStringMember() != null) {
                    xmlWriter.startElement("stringMember").value(allTypesRequest.getStringMember()).endElement();
                }

                if (allTypesRequest.getIntegerMember() != null) {
                    xmlWriter.startElement("integerMember").value(allTypesRequest.getIntegerMember()).endElement();
                }

                if (allTypesRequest.getBooleanMember() != null) {
                    xmlWriter.startElement("booleanMember").value(allTypesRequest.getBooleanMember()).endElement();
                }

                if (allTypesRequest.getFloatMember() != null) {
                    xmlWriter.startElement("floatMember").value(allTypesRequest.getFloatMember()).endElement();
                }

                if (allTypesRequest.getDoubleMember() != null) {
                    xmlWriter.startElement("doubleMember").value(allTypesRequest.getDoubleMember()).endElement();
                }

                if (allTypesRequest.getLongMember() != null) {
                    xmlWriter.startElement("longMember").value(allTypesRequest.getLongMember()).endElement();
                }

                {
                    SimpleStruct simpleStructMember = allTypesRequest.getSimpleStructMember();
                    if (simpleStructMember != null) {
                        xmlWriter.startElement("simpleStructMember");

                        if (simpleStructMember.getStringMember() != null) {
                            xmlWriter.startElement("StringMember").value(simpleStructMember.getStringMember()).endElement();
                        }
                        xmlWriter.endElement();
                    }
                }

                List<String> allTypesRequestSimpleListList = allTypesRequest.getSimpleList();
                if (allTypesRequestSimpleListList != null) {
                    xmlWriter.startElement("simpleList");

                    for (String allTypesRequestSimpleListListValue : allTypesRequestSimpleListList) {
                        xmlWriter.startElement("member");
                        xmlWriter.value(allTypesRequestSimpleListListValue);
                        xmlWriter.endElement();
                    }
                    xmlWriter.endElement();
                }

                List<SimpleStruct> allTypesRequestListOfStructsList = allTypesRequest.getListOfStructs();
                if (allTypesRequestListOfStructsList != null) {
                    xmlWriter.startElement("listOfStructs");

                    for (SimpleStruct allTypesRequestListOfStructsListValue : allTypesRequestListOfStructsList) {
                        xmlWriter.startElement("member");

                        if (allTypesRequestListOfStructsListValue.getStringMember() != null) {
                            xmlWriter.startElement("StringMember").value(allTypesRequestListOfStructsListValue.getStringMember()).endElement();
                        }
                        xmlWriter.endElement();
                    }
                    xmlWriter.endElement();
                }

                HashMap<String, String> allTypesRequestMapOfStringToStringMap = (HashMap<String, String>) allTypesRequest
                        .getMapOfStringToString();
                if (allTypesRequestMapOfStringToStringMap != null) {
                    xmlWriter.startElement("mapOfStringToString");

                    for (Map.Entry<String, String> allTypesRequestMapOfStringToStringMapValue : allTypesRequestMapOfStringToStringMap.entrySet()) {
                        if (allTypesRequestMapOfStringToStringMapValue == null) {
                            continue;
                        }
                        xmlWriter.startElement("entry");
                        xmlWriter.startElement("key");
                        xmlWriter.value(allTypesRequestMapOfStringToStringMapValue.getKey());
                        xmlWriter.endElement();
                        xmlWriter.startElement("value");
                        xmlWriter.value(allTypesRequestMapOfStringToStringMapValue.getValue());
                        xmlWriter.endElement();
                        xmlWriter.endElement();
                    }
                    xmlWriter.endElement();
                }

                if (allTypesRequest.getTimestampMember() != null) {
                    xmlWriter.startElement("timestampMember").value(allTypesRequest.getTimestampMember()).endElement();
                }

                {
                    StructWithTimestamp structWithNestedTimestampMember = allTypesRequest.getStructWithNestedTimestampMember();
                    if (structWithNestedTimestampMember != null) {
                        xmlWriter.startElement("structWithNestedTimestampMember");

                        if (structWithNestedTimestampMember.getNestedTimestamp() != null) {
                            xmlWriter.startElement("NestedTimestamp").value(structWithNestedTimestampMember.getNestedTimestamp()).endElement();
                        }
                        xmlWriter.endElement();
                    }
                }

                if (allTypesRequest.getBlobArg() != null) {
                    xmlWriter.startElement("blobArg").value(allTypesRequest.getBlobArg()).endElement();
                }

                HashMap<String, java.nio.ByteBuffer> allTypesRequestBlobMapMap = (HashMap<String, java.nio.ByteBuffer>) allTypesRequest
                        .getBlobMap();
                if (allTypesRequestBlobMapMap != null) {
                    xmlWriter.startElement("blobMap");

                    for (Map.Entry<String, java.nio.ByteBuffer> allTypesRequestBlobMapMapValue : allTypesRequestBlobMapMap.entrySet()) {
                        if (allTypesRequestBlobMapMapValue == null) {
                            continue;
                        }
                        xmlWriter.startElement("entry");
                        xmlWriter.startElement("key");
                        xmlWriter.value(allTypesRequestBlobMapMapValue.getKey());
                        xmlWriter.endElement();
                        xmlWriter.startElement("value");
                        xmlWriter.value(allTypesRequestBlobMapMapValue.getValue());
                        xmlWriter.endElement();
                        xmlWriter.endElement();
                    }
                    xmlWriter.endElement();
                }

                List<java.nio.ByteBuffer> allTypesRequestListOfBlobsList = allTypesRequest.getListOfBlobs();
                if (allTypesRequestListOfBlobsList != null) {
                    xmlWriter.startElement("listOfBlobs");

                    for (java.nio.ByteBuffer allTypesRequestListOfBlobsListValue : allTypesRequestListOfBlobsList) {
                        xmlWriter.startElement("member");
                        xmlWriter.value(allTypesRequestListOfBlobsListValue);
                        xmlWriter.endElement();
                    }
                    xmlWriter.endElement();
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
