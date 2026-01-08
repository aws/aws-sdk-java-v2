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
import com.amazonaws.util.StringUtils;

import com.amazonaws.util.XMLWriter;

/**
 * RestXmlTypesRequest Marshaller
 */


public class RestXmlTypesRequestMarshaller implements Marshaller<Request<RestXmlTypesRequest>, RestXmlTypesRequest> {

    public Request<RestXmlTypesRequest> marshall(RestXmlTypesRequest restXmlTypesRequest) {

        if (restXmlTypesRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<RestXmlTypesRequest> request = new DefaultRequest<RestXmlTypesRequest>(restXmlTypesRequest, "AmazonProtocolRestXml");

        request.setHttpMethod(HttpMethodName.POST);

        if (restXmlTypesRequest.getTimestampMemberInHeader() != null) {
            request.addHeader("x-amz-timearg", StringUtils.fromDate(restXmlTypesRequest.getTimestampMemberInHeader()));
        }

        String uriResourcePath = "/2016-03-11/restXmlTypes";

        request.setResourcePath(uriResourcePath);

        if (restXmlTypesRequest.getStringMemberInQuery() != null) {
            request.addParameter("stringMemberInQuery", StringUtils.fromString(restXmlTypesRequest.getStringMemberInQuery()));
        }

        if (restXmlTypesRequest.getListOfStringsInQuery() != null && !(restXmlTypesRequest.getListOfStringsInQuery().isEmpty())) {
            for (String value : restXmlTypesRequest.getListOfStringsInQuery()) {
                request.addParameter("listOfStrings", StringUtils.fromString(value));
            }
        }

        Map<String, String> mapOfStringToStringInQuery = restXmlTypesRequest.getMapOfStringToStringInQuery();
        if (mapOfStringToStringInQuery != null) {
            for (Map.Entry<String, String> entry : mapOfStringToStringInQuery.entrySet()) {
                if (entry != null && entry.getValue() != null) {
                    request.addParameter(StringUtils.fromString(entry.getKey()), StringUtils.fromString(entry.getValue()));
                }
            }
        }

        try {
            StringWriter stringWriter = new StringWriter();
            XMLWriter xmlWriter = new XMLWriter(stringWriter, "https://restxml/");

            xmlWriter.startElement("RestXmlTypesRequest");
            if (restXmlTypesRequest != null) {

                List<String> restXmlTypesRequestFlattenedListOfStringsList = restXmlTypesRequest.getFlattenedListOfStrings();
                if (restXmlTypesRequestFlattenedListOfStringsList != null) {
                    for (String restXmlTypesRequestFlattenedListOfStringsListValue : restXmlTypesRequestFlattenedListOfStringsList) {
                        xmlWriter.startElement("FlattenedListOfStrings");
                        xmlWriter.value(restXmlTypesRequestFlattenedListOfStringsListValue);
                        xmlWriter.endElement();
                    }
                }

                List<String> restXmlTypesRequestNonFlattenedListWithLocationList = restXmlTypesRequest.getNonFlattenedListWithLocation();
                if (restXmlTypesRequestNonFlattenedListWithLocationList != null) {
                    xmlWriter.startElement("NonFlattenedListWithLocation");

                    for (String restXmlTypesRequestNonFlattenedListWithLocationListValue : restXmlTypesRequestNonFlattenedListWithLocationList) {
                        xmlWriter.startElement("item");
                        xmlWriter.value(restXmlTypesRequestNonFlattenedListWithLocationListValue);
                        xmlWriter.endElement();
                    }
                    xmlWriter.endElement();
                }

                List<SimpleStruct> restXmlTypesRequestFlattenedListOfStructsList = restXmlTypesRequest.getFlattenedListOfStructs();
                if (restXmlTypesRequestFlattenedListOfStructsList != null) {
                    for (SimpleStruct restXmlTypesRequestFlattenedListOfStructsListValue : restXmlTypesRequestFlattenedListOfStructsList) {
                        xmlWriter.startElement("FlattenedListOfStructs");

                        if (restXmlTypesRequestFlattenedListOfStructsListValue.getStringMember() != null) {
                            xmlWriter.startElement("StringMember").value(restXmlTypesRequestFlattenedListOfStructsListValue.getStringMember()).endElement();
                        }
                        xmlWriter.endElement();
                    }
                }

                List<String> restXmlTypesRequestFlattenedListWithLocationList = restXmlTypesRequest.getFlattenedListWithLocation();
                if (restXmlTypesRequestFlattenedListWithLocationList != null) {
                    for (String restXmlTypesRequestFlattenedListWithLocationListValue : restXmlTypesRequestFlattenedListWithLocationList) {
                        xmlWriter.startElement("item");
                        xmlWriter.value(restXmlTypesRequestFlattenedListWithLocationListValue);
                        xmlWriter.endElement();
                    }
                }

                HashMap<String, String> restXmlTypesRequestFlattenedMapMap = (HashMap<String, String>) restXmlTypesRequest
                        .getFlattenedMap();
                if (restXmlTypesRequestFlattenedMapMap != null) {
                    xmlWriter.startElement("FlattenedMap");

                    for (Map.Entry<String, String> restXmlTypesRequestFlattenedMapMapValue : restXmlTypesRequestFlattenedMapMap.entrySet()) {
                        if (restXmlTypesRequestFlattenedMapMapValue == null) {
                            continue;
                        }
                        xmlWriter.startElement("entry");
                        xmlWriter.startElement("key");
                        xmlWriter.value(restXmlTypesRequestFlattenedMapMapValue.getKey());
                        xmlWriter.endElement();
                        xmlWriter.startElement("value");
                        xmlWriter.value(restXmlTypesRequestFlattenedMapMapValue.getValue());
                        xmlWriter.endElement();
                        xmlWriter.endElement();
                    }
                    xmlWriter.endElement();
                }

                HashMap<String, String> restXmlTypesRequestFlattenedMapWithLocationMap = (HashMap<String, String>) restXmlTypesRequest
                        .getFlattenedMapWithLocation();
                if (restXmlTypesRequestFlattenedMapWithLocationMap != null) {
                    xmlWriter.startElement("flatmap");

                    for (Map.Entry<String, String> restXmlTypesRequestFlattenedMapWithLocationMapValue : restXmlTypesRequestFlattenedMapWithLocationMap
                            .entrySet()) {
                        if (restXmlTypesRequestFlattenedMapWithLocationMapValue == null) {
                            continue;
                        }
                        xmlWriter.startElement("entry");
                        xmlWriter.startElement("thekey");
                        xmlWriter.value(restXmlTypesRequestFlattenedMapWithLocationMapValue.getKey());
                        xmlWriter.endElement();
                        xmlWriter.startElement("thevalue");
                        xmlWriter.value(restXmlTypesRequestFlattenedMapWithLocationMapValue.getValue());
                        xmlWriter.endElement();
                        xmlWriter.endElement();
                    }
                    xmlWriter.endElement();
                }

                HashMap<String, String> restXmlTypesRequestNonFlattenedMapWithLocationMap = (HashMap<String, String>) restXmlTypesRequest
                        .getNonFlattenedMapWithLocation();
                if (restXmlTypesRequestNonFlattenedMapWithLocationMap != null) {
                    xmlWriter.startElement("themap");

                    for (Map.Entry<String, String> restXmlTypesRequestNonFlattenedMapWithLocationMapValue : restXmlTypesRequestNonFlattenedMapWithLocationMap
                            .entrySet()) {
                        if (restXmlTypesRequestNonFlattenedMapWithLocationMapValue == null) {
                            continue;
                        }
                        xmlWriter.startElement("entry");
                        xmlWriter.startElement("thekey");
                        xmlWriter.value(restXmlTypesRequestNonFlattenedMapWithLocationMapValue.getKey());
                        xmlWriter.endElement();
                        xmlWriter.startElement("thevalue");
                        xmlWriter.value(restXmlTypesRequestNonFlattenedMapWithLocationMapValue.getValue());
                        xmlWriter.endElement();
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
