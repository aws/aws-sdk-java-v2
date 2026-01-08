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

import java.util.List;


import com.amazonaws.SdkClientException;
import com.amazonaws.services.protocol.rpcv2protocol.model.*;

import com.amazonaws.protocol.*;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * RpcV2CborListsRequestMarshaller
 */

@SdkInternalApi
public class RpcV2CborListsRequestMarshaller {

    private static final MarshallingInfo<List> STRINGLIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("stringList").build();
    private static final MarshallingInfo<List> STRINGSET_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("stringSet").build();
    private static final MarshallingInfo<List> INTEGERLIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("integerList").build();
    private static final MarshallingInfo<List> BOOLEANLIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("booleanList").build();
    private static final MarshallingInfo<List> TIMESTAMPLIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("timestampList").build();
    private static final MarshallingInfo<List> ENUMLIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("enumList").build();
    private static final MarshallingInfo<List> INTENUMLIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("intEnumList").build();
    private static final MarshallingInfo<List> NESTEDSTRINGLIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("nestedStringList").build();
    private static final MarshallingInfo<List> STRUCTURELIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("structureList").build();
    private static final MarshallingInfo<List> BLOBLIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("blobList").build();

    private static final RpcV2CborListsRequestMarshaller instance = new RpcV2CborListsRequestMarshaller();

    public static RpcV2CborListsRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(RpcV2CborListsRequest rpcV2CborListsRequest, ProtocolMarshaller protocolMarshaller) {

        if (rpcV2CborListsRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(rpcV2CborListsRequest.getStringList(), STRINGLIST_BINDING);
            protocolMarshaller.marshall(rpcV2CborListsRequest.getStringSet(), STRINGSET_BINDING);
            protocolMarshaller.marshall(rpcV2CborListsRequest.getIntegerList(), INTEGERLIST_BINDING);
            protocolMarshaller.marshall(rpcV2CborListsRequest.getBooleanList(), BOOLEANLIST_BINDING);
            protocolMarshaller.marshall(rpcV2CborListsRequest.getTimestampList(), TIMESTAMPLIST_BINDING);
            protocolMarshaller.marshall(rpcV2CborListsRequest.getEnumList(), ENUMLIST_BINDING);
            protocolMarshaller.marshall(rpcV2CborListsRequest.getIntEnumList(), INTENUMLIST_BINDING);
            protocolMarshaller.marshall(rpcV2CborListsRequest.getNestedStringList(), NESTEDSTRINGLIST_BINDING);
            protocolMarshaller.marshall(rpcV2CborListsRequest.getStructureList(), STRUCTURELIST_BINDING);
            protocolMarshaller.marshall(rpcV2CborListsRequest.getBlobList(), BLOBLIST_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request: " + e.getMessage(), e);
        }
    }

}
