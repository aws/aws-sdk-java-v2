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
package com.amazonaws.services.protocol.query.model.transform;

import java.util.List;
import java.util.Map;


import com.amazonaws.SdkClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.query.model.*;
import com.amazonaws.transform.Marshaller;
import com.amazonaws.util.StringUtils;

/**
 * AllTypesRequest Marshaller
 */


public class AllTypesRequestMarshaller implements Marshaller<Request<AllTypesRequest>, AllTypesRequest> {

    public Request<AllTypesRequest> marshall(AllTypesRequest allTypesRequest) {

        if (allTypesRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<AllTypesRequest> request = new DefaultRequest<AllTypesRequest>(allTypesRequest, "AmazonProtocolQuery");
        request.addParameter("Action", "AllTypes");
        request.addParameter("Version", "2016-03-11");
        request.setHttpMethod(HttpMethodName.POST);

        if (allTypesRequest.getStringMember() != null) {
            request.addParameter("stringMember", StringUtils.fromString(allTypesRequest.getStringMember()));
        }

        if (allTypesRequest.getIntegerMember() != null) {
            request.addParameter("integerMember", StringUtils.fromInteger(allTypesRequest.getIntegerMember()));
        }

        if (allTypesRequest.getBooleanMember() != null) {
            request.addParameter("booleanMember", StringUtils.fromBoolean(allTypesRequest.getBooleanMember()));
        }

        if (allTypesRequest.getFloatMember() != null) {
            request.addParameter("floatMember", StringUtils.fromFloat(allTypesRequest.getFloatMember()));
        }

        if (allTypesRequest.getDoubleMember() != null) {
            request.addParameter("doubleMember", StringUtils.fromDouble(allTypesRequest.getDoubleMember()));
        }

        if (allTypesRequest.getLongMember() != null) {
            request.addParameter("longMember", StringUtils.fromLong(allTypesRequest.getLongMember()));
        }

        {
            SimpleStruct simpleStructMember = allTypesRequest.getSimpleStructMember();
            if (simpleStructMember != null) {

                if (simpleStructMember.getStringMember() != null) {
                    request.addParameter("simpleStructMember.StringMember", StringUtils.fromString(simpleStructMember.getStringMember()));
                }
            }
        }

        if (allTypesRequest.getSimpleList() != null) {
            List<String> simpleListList = allTypesRequest.getSimpleList();
            if (simpleListList.isEmpty()) {
                request.addParameter("simpleList", "");
            } else {
                int simpleListListIndex = 1;

                for (String simpleListListValue : simpleListList) {
                    if (simpleListListValue != null) {
                        request.addParameter("simpleList.member." + simpleListListIndex, StringUtils.fromString(simpleListListValue));
                    }
                    simpleListListIndex++;
                }
            }
        }

        if (allTypesRequest.getListOfStructs() != null) {
            List<SimpleStruct> listOfStructsList = allTypesRequest.getListOfStructs();
            if (listOfStructsList.isEmpty()) {
                request.addParameter("listOfStructs", "");
            } else {
                int listOfStructsListIndex = 1;

                for (SimpleStruct listOfStructsListValue : listOfStructsList) {
                    if (listOfStructsListValue != null) {

                        if (listOfStructsListValue.getStringMember() != null) {
                            request.addParameter("listOfStructs.member." + listOfStructsListIndex + ".StringMember",
                                    StringUtils.fromString(listOfStructsListValue.getStringMember()));
                        }
                    }
                    listOfStructsListIndex++;
                }
            }
        }

        Map<String, String> mapOfStringToString = allTypesRequest.getMapOfStringToString();
        if (mapOfStringToString != null) {
            int mapOfStringToStringListIndex = 1;
            for (Map.Entry<String, String> entry : mapOfStringToString.entrySet()) {
                if (entry != null && entry.getKey() != null) {
                    request.addParameter("mapOfStringToString.entry." + mapOfStringToStringListIndex + ".key", StringUtils.fromString(entry.getKey()));
                }
                if (entry != null && entry.getValue() != null) {
                    request.addParameter("mapOfStringToString.entry." + mapOfStringToStringListIndex + ".value", StringUtils.fromString(entry.getValue()));
                }
                mapOfStringToStringListIndex++;
            }
        }

        if (allTypesRequest.getTimestampMember() != null) {
            request.addParameter("timestampMember", StringUtils.fromDate(allTypesRequest.getTimestampMember()));
        }

        {
            StructWithTimestamp structWithNestedTimestampMember = allTypesRequest.getStructWithNestedTimestampMember();
            if (structWithNestedTimestampMember != null) {

                if (structWithNestedTimestampMember.getNestedTimestamp() != null) {
                    request.addParameter("structWithNestedTimestampMember.NestedTimestamp",
                            StringUtils.fromDate(structWithNestedTimestampMember.getNestedTimestamp()));
                }
            }
        }

        if (allTypesRequest.getBlobArg() != null) {
            request.addParameter("blobArg", StringUtils.fromByteBuffer(allTypesRequest.getBlobArg()));
        }

        Map<String, java.nio.ByteBuffer> blobMap = allTypesRequest.getBlobMap();
        if (blobMap != null) {
            int blobMapListIndex = 1;
            for (Map.Entry<String, java.nio.ByteBuffer> entry : blobMap.entrySet()) {
                if (entry != null && entry.getKey() != null) {
                    request.addParameter("blobMap.entry." + blobMapListIndex + ".key", StringUtils.fromString(entry.getKey()));
                }
                if (entry != null && entry.getValue() != null) {
                    request.addParameter("blobMap.entry." + blobMapListIndex + ".value", StringUtils.fromByteBuffer(entry.getValue()));
                }
                blobMapListIndex++;
            }
        }

        if (allTypesRequest.getListOfBlobs() != null) {
            List<java.nio.ByteBuffer> listOfBlobsList = allTypesRequest.getListOfBlobs();
            if (listOfBlobsList.isEmpty()) {
                request.addParameter("listOfBlobs", "");
            } else {
                int listOfBlobsListIndex = 1;

                for (java.nio.ByteBuffer listOfBlobsListValue : listOfBlobsList) {
                    if (listOfBlobsListValue != null) {
                        request.addParameter("listOfBlobs.member." + listOfBlobsListIndex, StringUtils.fromByteBuffer(listOfBlobsListValue));
                    }
                    listOfBlobsListIndex++;
                }
            }
        }

        return request;
    }

}
