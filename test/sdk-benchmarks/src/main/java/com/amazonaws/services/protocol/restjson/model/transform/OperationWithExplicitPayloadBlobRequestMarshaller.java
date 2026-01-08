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
import com.amazonaws.services.protocol.restjson.model.*;

import com.amazonaws.protocol.*;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * OperationWithExplicitPayloadBlobRequestMarshaller
 */

@SdkInternalApi
public class OperationWithExplicitPayloadBlobRequestMarshaller {

    private static final MarshallingInfo<java.nio.ByteBuffer> PAYLOADMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.BYTE_BUFFER)
            .marshallLocation(MarshallLocation.PAYLOAD).isExplicitPayloadMember(true).isBinary(true).build();

    private static final OperationWithExplicitPayloadBlobRequestMarshaller instance = new OperationWithExplicitPayloadBlobRequestMarshaller();

    public static OperationWithExplicitPayloadBlobRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(OperationWithExplicitPayloadBlobRequest operationWithExplicitPayloadBlobRequest, ProtocolMarshaller protocolMarshaller) {

        if (operationWithExplicitPayloadBlobRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(operationWithExplicitPayloadBlobRequest.getPayloadMember(), PAYLOADMEMBER_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
