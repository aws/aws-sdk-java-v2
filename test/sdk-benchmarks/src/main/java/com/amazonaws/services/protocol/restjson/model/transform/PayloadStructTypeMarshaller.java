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
 * PayloadStructTypeMarshaller
 */

@SdkInternalApi
public class PayloadStructTypeMarshaller {

    private static final MarshallingInfo<String> PAYLOADMEMBERONE_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("PayloadMemberOne").build();
    private static final MarshallingInfo<String> PAYLOADMEMBERTWO_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("PayloadMemberTwo").build();

    private static final PayloadStructTypeMarshaller instance = new PayloadStructTypeMarshaller();

    public static PayloadStructTypeMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(PayloadStructType payloadStructType, ProtocolMarshaller protocolMarshaller) {

        if (payloadStructType == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(payloadStructType.getPayloadMemberOne(), PAYLOADMEMBERONE_BINDING);
            protocolMarshaller.marshall(payloadStructType.getPayloadMemberTwo(), PAYLOADMEMBERTWO_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
