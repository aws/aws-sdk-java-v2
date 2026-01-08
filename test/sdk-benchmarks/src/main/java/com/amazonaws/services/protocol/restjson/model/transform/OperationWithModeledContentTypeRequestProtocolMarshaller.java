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
package com.amazonaws.services.protocol.restjson.model.transform;



import com.amazonaws.SdkClientException;
import com.amazonaws.Request;

import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.restjson.model.*;
import com.amazonaws.transform.Marshaller;

import com.amazonaws.protocol.*;
import com.amazonaws.protocol.Protocol;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * OperationWithModeledContentTypeRequest Marshaller
 */

@SdkInternalApi
public class OperationWithModeledContentTypeRequestProtocolMarshaller implements
        Marshaller<Request<OperationWithModeledContentTypeRequest>, OperationWithModeledContentTypeRequest> {

    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().protocol(Protocol.REST_JSON)
            .requestUri("/2016-03-11/operationWithModeledContentType").httpMethodName(HttpMethodName.POST).hasExplicitPayloadMember(false)
            .hasPayloadMembers(false).serviceName("AmazonProtocolRestJson").build();

    private final com.amazonaws.protocol.json.SdkJsonProtocolFactory protocolFactory;

    public OperationWithModeledContentTypeRequestProtocolMarshaller(com.amazonaws.protocol.json.SdkJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public Request<OperationWithModeledContentTypeRequest> marshall(OperationWithModeledContentTypeRequest operationWithModeledContentTypeRequest) {

        if (operationWithModeledContentTypeRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            final ProtocolRequestMarshaller<OperationWithModeledContentTypeRequest> protocolMarshaller = protocolFactory.createProtocolMarshaller(
                    SDK_OPERATION_BINDING, operationWithModeledContentTypeRequest);

            protocolMarshaller.startMarshalling();
            OperationWithModeledContentTypeRequestMarshaller.getInstance().marshall(operationWithModeledContentTypeRequest, protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
