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
 * Ec2TypesRequest Marshaller
 */


public class Ec2TypesRequestMarshaller implements Marshaller<Request<Ec2TypesRequest>, Ec2TypesRequest> {

    public Request<Ec2TypesRequest> marshall(Ec2TypesRequest ec2TypesRequest) {

        if (ec2TypesRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<Ec2TypesRequest> request = new DefaultRequest<Ec2TypesRequest>(ec2TypesRequest, "AmazonProtocolEc2");
        request.addParameter("Action", "Ec2Types");
        request.addParameter("Version", "2016-03-11");
        request.setHttpMethod(HttpMethodName.POST);

        List<String> ec2TypesRequestFlattenedListOfStringsList = ec2TypesRequest.getFlattenedListOfStrings();
        if (ec2TypesRequestFlattenedListOfStringsList != null) {
            int flattenedListOfStringsListIndex = 1;

            for (String ec2TypesRequestFlattenedListOfStringsListValue : ec2TypesRequestFlattenedListOfStringsList) {
                if (ec2TypesRequestFlattenedListOfStringsListValue != null) {
                    request.addParameter("FlattenedListOfStrings." + flattenedListOfStringsListIndex,
                            StringUtils.fromString(ec2TypesRequestFlattenedListOfStringsListValue));
                }
                flattenedListOfStringsListIndex++;
            }
        }

        List<SimpleStruct> ec2TypesRequestFlattenedListOfStructsList = ec2TypesRequest.getFlattenedListOfStructs();
        if (ec2TypesRequestFlattenedListOfStructsList != null) {
            int flattenedListOfStructsListIndex = 1;

            for (SimpleStruct ec2TypesRequestFlattenedListOfStructsListValue : ec2TypesRequestFlattenedListOfStructsList) {

                if (ec2TypesRequestFlattenedListOfStructsListValue.getStringMember() != null) {
                    request.addParameter("FlattenedListOfStructs." + flattenedListOfStructsListIndex + ".StringMember",
                            StringUtils.fromString(ec2TypesRequestFlattenedListOfStructsListValue.getStringMember()));
                }
                flattenedListOfStructsListIndex++;
            }
        }

        List<String> ec2TypesRequestFlattenedListWithLocationList = ec2TypesRequest.getFlattenedListWithLocation();
        if (ec2TypesRequestFlattenedListWithLocationList != null) {
            int flattenedListWithLocationListIndex = 1;

            for (String ec2TypesRequestFlattenedListWithLocationListValue : ec2TypesRequestFlattenedListWithLocationList) {
                if (ec2TypesRequestFlattenedListWithLocationListValue != null) {
                    request.addParameter("ListMemberName." + flattenedListWithLocationListIndex,
                            StringUtils.fromString(ec2TypesRequestFlattenedListWithLocationListValue));
                }
                flattenedListWithLocationListIndex++;
            }
        }

        if (ec2TypesRequest.getStringMemberWithLocation() != null) {
            request.addParameter("SomeLocation", StringUtils.fromString(ec2TypesRequest.getStringMemberWithLocation()));
        }

        if (ec2TypesRequest.getStringMemberWithQueryName() != null) {
            request.addParameter("someQueryName", StringUtils.fromString(ec2TypesRequest.getStringMemberWithQueryName()));
        }

        if (ec2TypesRequest.getStringMemberWithLocationAndQueryName() != null) {
            request.addParameter("someQueryName", StringUtils.fromString(ec2TypesRequest.getStringMemberWithLocationAndQueryName()));
        }

        List<String> ec2TypesRequestListMemberWithLocationAndQueryNameList = ec2TypesRequest.getListMemberWithLocationAndQueryName();
        if (ec2TypesRequestListMemberWithLocationAndQueryNameList != null) {
            int listMemberWithLocationAndQueryNameListIndex = 1;

            for (String ec2TypesRequestListMemberWithLocationAndQueryNameListValue : ec2TypesRequestListMemberWithLocationAndQueryNameList) {
                if (ec2TypesRequestListMemberWithLocationAndQueryNameListValue != null) {
                    request.addParameter("listQueryName." + listMemberWithLocationAndQueryNameListIndex,
                            StringUtils.fromString(ec2TypesRequestListMemberWithLocationAndQueryNameListValue));
                }
                listMemberWithLocationAndQueryNameListIndex++;
            }
        }

        List<String> ec2TypesRequestListMemberWithOnlyMemberLocationList = ec2TypesRequest.getListMemberWithOnlyMemberLocation();
        if (ec2TypesRequestListMemberWithOnlyMemberLocationList != null) {
            int listMemberWithOnlyMemberLocationListIndex = 1;

            for (String ec2TypesRequestListMemberWithOnlyMemberLocationListValue : ec2TypesRequestListMemberWithOnlyMemberLocationList) {
                if (ec2TypesRequestListMemberWithOnlyMemberLocationListValue != null) {
                    request.addParameter("Item." + listMemberWithOnlyMemberLocationListIndex,
                            StringUtils.fromString(ec2TypesRequestListMemberWithOnlyMemberLocationListValue));
                }
                listMemberWithOnlyMemberLocationListIndex++;
            }
        }

        return request;
    }

}
