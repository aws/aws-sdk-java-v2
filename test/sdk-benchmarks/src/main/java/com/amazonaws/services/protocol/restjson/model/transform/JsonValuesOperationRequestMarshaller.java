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
 * JsonValuesOperationRequestMarshaller
 */

@SdkInternalApi
public class JsonValuesOperationRequestMarshaller {

    private static final MarshallingInfo<String> JSONVALUEHEADERMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.JSON_VALUE)
            .marshallLocation(MarshallLocation.HEADER).marshallLocationName("Encoded-Header").build();
    private static final MarshallingInfo<String> JSONVALUEMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.JSON_VALUE)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("JsonValueMember").build();

    private static final JsonValuesOperationRequestMarshaller instance = new JsonValuesOperationRequestMarshaller();

    public static JsonValuesOperationRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(JsonValuesOperationRequest jsonValuesOperationRequest, ProtocolMarshaller protocolMarshaller) {

        if (jsonValuesOperationRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(jsonValuesOperationRequest.getJsonValueHeaderMember(), JSONVALUEHEADERMEMBER_BINDING);
            protocolMarshaller.marshall(jsonValuesOperationRequest.getJsonValueMember(), JSONVALUEMEMBER_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
