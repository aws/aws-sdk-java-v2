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

import java.util.Map;



import com.amazonaws.SdkClientException;
import com.amazonaws.services.protocol.rpcv2protocol.model.*;

import com.amazonaws.protocol.*;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * RpcV2CborDenseMapsRequestMarshaller
 */

@SdkInternalApi
public class RpcV2CborDenseMapsRequestMarshaller {

    private static final MarshallingInfo<Map> DENSESTRUCTMAP_BINDING = MarshallingInfo.builder(MarshallingType.MAP).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("denseStructMap").build();
    private static final MarshallingInfo<Map> DENSENUMBERMAP_BINDING = MarshallingInfo.builder(MarshallingType.MAP).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("denseNumberMap").build();
    private static final MarshallingInfo<Map> DENSEBOOLEANMAP_BINDING = MarshallingInfo.builder(MarshallingType.MAP).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("denseBooleanMap").build();
    private static final MarshallingInfo<Map> DENSESTRINGMAP_BINDING = MarshallingInfo.builder(MarshallingType.MAP).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("denseStringMap").build();
    private static final MarshallingInfo<Map> DENSESETMAP_BINDING = MarshallingInfo.builder(MarshallingType.MAP).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("denseSetMap").build();

    private static final RpcV2CborDenseMapsRequestMarshaller instance = new RpcV2CborDenseMapsRequestMarshaller();

    public static RpcV2CborDenseMapsRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(RpcV2CborDenseMapsRequest rpcV2CborDenseMapsRequest, ProtocolMarshaller protocolMarshaller) {

        if (rpcV2CborDenseMapsRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(rpcV2CborDenseMapsRequest.getDenseStructMap(), DENSESTRUCTMAP_BINDING);
            protocolMarshaller.marshall(rpcV2CborDenseMapsRequest.getDenseNumberMap(), DENSENUMBERMAP_BINDING);
            protocolMarshaller.marshall(rpcV2CborDenseMapsRequest.getDenseBooleanMap(), DENSEBOOLEANMAP_BINDING);
            protocolMarshaller.marshall(rpcV2CborDenseMapsRequest.getDenseStringMap(), DENSESTRINGMAP_BINDING);
            protocolMarshaller.marshall(rpcV2CborDenseMapsRequest.getDenseSetMap(), DENSESETMAP_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request: " + e.getMessage(), e);
        }
    }

}
