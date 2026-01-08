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



import com.amazonaws.SdkClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.restxml.model.*;
import com.amazonaws.transform.Marshaller;

import com.amazonaws.util.StringUtils;

/**
 * MembersInHeadersRequest Marshaller
 */


public class MembersInHeadersRequestMarshaller implements Marshaller<Request<MembersInHeadersRequest>, MembersInHeadersRequest> {

    public Request<MembersInHeadersRequest> marshall(MembersInHeadersRequest membersInHeadersRequest) {

        if (membersInHeadersRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<MembersInHeadersRequest> request = new DefaultRequest<MembersInHeadersRequest>(membersInHeadersRequest, "AmazonProtocolRestXml");

        request.setHttpMethod(HttpMethodName.POST);

        if (membersInHeadersRequest.getStringMember() != null) {
            request.addHeader("x-amz-string", StringUtils.fromString(membersInHeadersRequest.getStringMember()));
        }

        if (membersInHeadersRequest.getBooleanMember() != null) {
            request.addHeader("x-amz-boolean", StringUtils.fromBoolean(membersInHeadersRequest.getBooleanMember()));
        }

        if (membersInHeadersRequest.getIntegerMember() != null) {
            request.addHeader("x-amz-integer", StringUtils.fromInteger(membersInHeadersRequest.getIntegerMember()));
        }

        if (membersInHeadersRequest.getLongMember() != null) {
            request.addHeader("x-amz-long", StringUtils.fromLong(membersInHeadersRequest.getLongMember()));
        }

        if (membersInHeadersRequest.getFloatMember() != null) {
            request.addHeader("x-amz-float", StringUtils.fromFloat(membersInHeadersRequest.getFloatMember()));
        }

        if (membersInHeadersRequest.getDoubleMember() != null) {
            request.addHeader("x-amz-double", StringUtils.fromDouble(membersInHeadersRequest.getDoubleMember()));
        }

        if (membersInHeadersRequest.getTimestampMember() != null) {
            request.addHeader("x-amz-timestamp", StringUtils.fromDate(membersInHeadersRequest.getTimestampMember()));
        }

        String uriResourcePath = "/2016-03-11/membersInHeaders";

        request.setResourcePath(uriResourcePath);

        return request;
    }

}
