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

import java.util.Map;


import com.amazonaws.SdkClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.restxml.model.*;
import com.amazonaws.transform.Marshaller;

import com.amazonaws.util.StringUtils;

/**
 * MembersInQueryParamsRequest Marshaller
 */


public class MembersInQueryParamsRequestMarshaller implements Marshaller<Request<MembersInQueryParamsRequest>, MembersInQueryParamsRequest> {

    public Request<MembersInQueryParamsRequest> marshall(MembersInQueryParamsRequest membersInQueryParamsRequest) {

        if (membersInQueryParamsRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<MembersInQueryParamsRequest> request = new DefaultRequest<MembersInQueryParamsRequest>(membersInQueryParamsRequest, "AmazonProtocolRestXml");

        request.setHttpMethod(HttpMethodName.GET);

        String uriResourcePath = "/2016-03-11/membersInQueryParams?StaticQueryParam=foo";

        uriResourcePath = com.amazonaws.util.UriResourcePathUtils.addStaticQueryParamtersToRequest(request, uriResourcePath);

        request.setResourcePath(uriResourcePath);

        if (membersInQueryParamsRequest.getStringQueryParam() != null) {
            request.addParameter("String", StringUtils.fromString(membersInQueryParamsRequest.getStringQueryParam()));
        }

        if (membersInQueryParamsRequest.getBooleanQueryParam() != null) {
            request.addParameter("Boolean", StringUtils.fromBoolean(membersInQueryParamsRequest.getBooleanQueryParam()));
        }

        if (membersInQueryParamsRequest.getIntegerQueryParam() != null) {
            request.addParameter("Integer", StringUtils.fromInteger(membersInQueryParamsRequest.getIntegerQueryParam()));
        }

        if (membersInQueryParamsRequest.getLongQueryParam() != null) {
            request.addParameter("Long", StringUtils.fromLong(membersInQueryParamsRequest.getLongQueryParam()));
        }

        if (membersInQueryParamsRequest.getFloatQueryParam() != null) {
            request.addParameter("Float", StringUtils.fromFloat(membersInQueryParamsRequest.getFloatQueryParam()));
        }

        if (membersInQueryParamsRequest.getDoubleQueryParam() != null) {
            request.addParameter("Double", StringUtils.fromDouble(membersInQueryParamsRequest.getDoubleQueryParam()));
        }

        if (membersInQueryParamsRequest.getTimestampQueryParam() != null) {
            request.addParameter("Timestamp", StringUtils.fromDate(membersInQueryParamsRequest.getTimestampQueryParam()));
        }

        if (membersInQueryParamsRequest.getListOfStrings() != null && !(membersInQueryParamsRequest.getListOfStrings().isEmpty())) {
            for (String value : membersInQueryParamsRequest.getListOfStrings()) {
                request.addParameter("item", StringUtils.fromString(value));
            }
        }

        Map<String, String> mapOfStringToString = membersInQueryParamsRequest.getMapOfStringToString();
        if (mapOfStringToString != null) {
            for (Map.Entry<String, String> entry : mapOfStringToString.entrySet()) {
                if (entry != null && entry.getValue() != null) {
                    request.addParameter(StringUtils.fromString(entry.getKey()), StringUtils.fromString(entry.getValue()));
                }
            }
        }

        return request;
    }

}
