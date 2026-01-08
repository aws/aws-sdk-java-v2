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
 * StreamingOutputOperationRequest Marshaller
 */

@SdkInternalApi
public class StreamingOutputOperationRequestProtocolMarshaller implements Marshaller<Request<StreamingOutputOperationRequest>, StreamingOutputOperationRequest> {

    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().protocol(Protocol.REST_JSON)
            .requestUri("/2016-03-11/streamingOutputOperation").httpMethodName(HttpMethodName.POST).hasExplicitPayloadMember(false).hasPayloadMembers(false)
            .serviceName("AmazonProtocolRestJson").build();

    private final com.amazonaws.protocol.json.SdkJsonProtocolFactory protocolFactory;

    public StreamingOutputOperationRequestProtocolMarshaller(com.amazonaws.protocol.json.SdkJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public Request<StreamingOutputOperationRequest> marshall(StreamingOutputOperationRequest streamingOutputOperationRequest) {

        if (streamingOutputOperationRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            final ProtocolRequestMarshaller<StreamingOutputOperationRequest> protocolMarshaller = protocolFactory.createProtocolMarshaller(
                    SDK_OPERATION_BINDING, streamingOutputOperationRequest);

            protocolMarshaller.startMarshalling();
            StreamingOutputOperationRequestMarshaller.getInstance().marshall(streamingOutputOperationRequest, protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
