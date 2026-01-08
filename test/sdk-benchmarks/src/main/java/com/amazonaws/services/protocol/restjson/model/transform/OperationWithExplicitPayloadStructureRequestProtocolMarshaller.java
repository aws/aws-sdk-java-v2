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
 * OperationWithExplicitPayloadStructureRequest Marshaller
 */

@SdkInternalApi
public class OperationWithExplicitPayloadStructureRequestProtocolMarshaller implements
        Marshaller<Request<OperationWithExplicitPayloadStructureRequest>, OperationWithExplicitPayloadStructureRequest> {

    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().protocol(Protocol.REST_JSON)
            .requestUri("/2016-03-11/operationWithExplicitPayloadStructure").httpMethodName(HttpMethodName.POST).hasExplicitPayloadMember(true)
            .hasPayloadMembers(true).serviceName("AmazonProtocolRestJson").build();

    private final com.amazonaws.protocol.json.SdkJsonProtocolFactory protocolFactory;

    public OperationWithExplicitPayloadStructureRequestProtocolMarshaller(com.amazonaws.protocol.json.SdkJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public Request<OperationWithExplicitPayloadStructureRequest> marshall(
            OperationWithExplicitPayloadStructureRequest operationWithExplicitPayloadStructureRequest) {

        if (operationWithExplicitPayloadStructureRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            final ProtocolRequestMarshaller<OperationWithExplicitPayloadStructureRequest> protocolMarshaller = protocolFactory.createProtocolMarshaller(
                    SDK_OPERATION_BINDING, operationWithExplicitPayloadStructureRequest);

            protocolMarshaller.startMarshalling();
            OperationWithExplicitPayloadStructureRequestMarshaller.getInstance().marshall(operationWithExplicitPayloadStructureRequest, protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
