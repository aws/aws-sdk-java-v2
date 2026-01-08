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

import java.util.Map;
import java.util.List;


import com.amazonaws.SdkClientException;
import com.amazonaws.services.protocol.restjson.model.*;

import com.amazonaws.protocol.*;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * RecursiveStructTypeMarshaller
 */

@SdkInternalApi
public class RecursiveStructTypeMarshaller {

    private static final MarshallingInfo<String> NORECURSE_BINDING = MarshallingInfo.builder(MarshallingType.STRING).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("NoRecurse").build();
    private static final MarshallingInfo<StructuredPojo> RECURSIVESTRUCT_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("RecursiveStruct").build();
    private static final MarshallingInfo<List> RECURSIVELIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("RecursiveList").build();
    private static final MarshallingInfo<Map> RECURSIVEMAP_BINDING = MarshallingInfo.builder(MarshallingType.MAP).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("RecursiveMap").build();

    private static final RecursiveStructTypeMarshaller instance = new RecursiveStructTypeMarshaller();

    public static RecursiveStructTypeMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(RecursiveStructType recursiveStructType, ProtocolMarshaller protocolMarshaller) {

        if (recursiveStructType == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(recursiveStructType.getNoRecurse(), NORECURSE_BINDING);
            protocolMarshaller.marshall(recursiveStructType.getRecursiveStruct(), RECURSIVESTRUCT_BINDING);
            protocolMarshaller.marshall(recursiveStructType.getRecursiveList(), RECURSIVELIST_BINDING);
            protocolMarshaller.marshall(recursiveStructType.getRecursiveMap(), RECURSIVEMAP_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
