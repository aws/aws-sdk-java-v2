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
package com.amazonaws.services.protocol.rpcv2protocol.model.transform;



import com.amazonaws.SdkClientException;
import com.amazonaws.Request;

import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.rpcv2protocol.model.*;
import com.amazonaws.transform.Marshaller;

import com.amazonaws.protocol.*;
import com.amazonaws.protocol.Protocol;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * Float16Request Marshaller
 */

@SdkInternalApi
public class Float16RequestProtocolMarshaller implements Marshaller<Request<Float16Request>, Float16Request> {

    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().protocol(Protocol.RPCV2_CBOR)
            .requestUri("service/RpcV2Protocol/operation/Float16").httpMethodName(HttpMethodName.POST).hasExplicitPayloadMember(false).hasPayloadMembers(false)
            .operationIdentifier("Float16").serviceName("RpcV2Protocol").build();

    private final com.amazonaws.protocol.rpcv2cbor.SdkRpcV2CborProtocolFactory protocolFactory;

    public Float16RequestProtocolMarshaller(com.amazonaws.protocol.rpcv2cbor.SdkRpcV2CborProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public Request<Float16Request> marshall(Float16Request float16Request) {

        if (float16Request == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            final ProtocolRequestMarshaller<Float16Request> protocolMarshaller = protocolFactory
                    .createProtocolMarshaller(SDK_OPERATION_BINDING, float16Request);

            protocolMarshaller.startMarshalling();
            Float16RequestMarshaller.getInstance().marshall(float16Request, protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request: " + e.getMessage(), e);
        }
    }

}
