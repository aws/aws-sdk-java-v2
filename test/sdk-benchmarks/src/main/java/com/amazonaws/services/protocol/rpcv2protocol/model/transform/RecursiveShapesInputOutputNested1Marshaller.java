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
import com.amazonaws.services.protocol.rpcv2protocol.model.*;

import com.amazonaws.protocol.*;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * RecursiveShapesInputOutputNested1Marshaller
 */

@SdkInternalApi
public class RecursiveShapesInputOutputNested1Marshaller {

    private static final MarshallingInfo<String> FOO_BINDING = MarshallingInfo.builder(MarshallingType.STRING).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("foo").build();
    private static final MarshallingInfo<StructuredPojo> NESTED_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("nested").build();

    private static final RecursiveShapesInputOutputNested1Marshaller instance = new RecursiveShapesInputOutputNested1Marshaller();

    public static RecursiveShapesInputOutputNested1Marshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(RecursiveShapesInputOutputNested1 recursiveShapesInputOutputNested1, ProtocolMarshaller protocolMarshaller) {

        if (recursiveShapesInputOutputNested1 == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(recursiveShapesInputOutputNested1.getFoo(), FOO_BINDING);
            protocolMarshaller.marshall(recursiveShapesInputOutputNested1.getNested(), NESTED_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request: " + e.getMessage(), e);
        }
    }

}
