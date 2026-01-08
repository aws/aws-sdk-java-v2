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
package com.amazonaws.services.protocol.ec2.model.transform;

import java.util.List;



import com.amazonaws.SdkClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.ec2.model.*;
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

        Request<AllTypesRequest> request = new DefaultRequest<AllTypesRequest>(allTypesRequest, "AmazonProtocolEc2");
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

        SimpleStruct simpleStructMember = allTypesRequest.getSimpleStructMember();
        if (simpleStructMember != null) {

            if (simpleStructMember.getStringMember() != null) {
                request.addParameter("simpleStructMember.StringMember", StringUtils.fromString(simpleStructMember.getStringMember()));
            }
        }

        List<String> allTypesRequestSimpleListList = allTypesRequest.getSimpleList();
        if (allTypesRequestSimpleListList != null) {
            int simpleListListIndex = 1;

            for (String allTypesRequestSimpleListListValue : allTypesRequestSimpleListList) {
                if (allTypesRequestSimpleListListValue != null) {
                    request.addParameter("simpleList." + simpleListListIndex, StringUtils.fromString(allTypesRequestSimpleListListValue));
                }
                simpleListListIndex++;
            }
        }

        List<SimpleStruct> allTypesRequestListOfStructsList = allTypesRequest.getListOfStructs();
        if (allTypesRequestListOfStructsList != null) {
            int listOfStructsListIndex = 1;

            for (SimpleStruct allTypesRequestListOfStructsListValue : allTypesRequestListOfStructsList) {

                if (allTypesRequestListOfStructsListValue.getStringMember() != null) {
                    request.addParameter("listOfStructs." + listOfStructsListIndex + ".StringMember",
                            StringUtils.fromString(allTypesRequestListOfStructsListValue.getStringMember()));
                }
                listOfStructsListIndex++;
            }
        }

        if (allTypesRequest.getTimestampMember() != null) {
            request.addParameter("timestampMember", StringUtils.fromDate(allTypesRequest.getTimestampMember()));
        }

        StructWithTimestamp structWithNestedTimestampMember = allTypesRequest.getStructWithNestedTimestampMember();
        if (structWithNestedTimestampMember != null) {

            if (structWithNestedTimestampMember.getNestedTimestamp() != null) {
                request.addParameter("structWithNestedTimestampMember.NestedTimestamp",
                        StringUtils.fromDate(structWithNestedTimestampMember.getNestedTimestamp()));
            }
        }

        if (allTypesRequest.getBlobArg() != null) {
            request.addParameter("blobArg", StringUtils.fromByteBuffer(allTypesRequest.getBlobArg()));
        }

        return request;
    }

}
