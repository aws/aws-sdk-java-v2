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
 * MultiLocationOperationRequestMarshaller
 */

@SdkInternalApi
public class MultiLocationOperationRequestMarshaller {

    private static final MarshallingInfo<String> PATHPARAM_BINDING = MarshallingInfo.builder(MarshallingType.STRING).marshallLocation(MarshallLocation.PATH)
            .marshallLocationName("PathParam").build();
    private static final MarshallingInfo<String> QUERYPARAMONE_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("QueryParamOne").build();
    private static final MarshallingInfo<String> QUERYPARAMTWO_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("QueryParamTwo").build();
    private static final MarshallingInfo<String> STRINGHEADERMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.HEADER).marshallLocationName("x-amz-header-string").build();
    private static final MarshallingInfo<java.util.Date> TIMESTAMPHEADERMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.DATE)
            .marshallLocation(MarshallLocation.HEADER).marshallLocationName("x-amz-timearg").timestampFormat("unknown").build();
    private static final MarshallingInfo<StructuredPojo> PAYLOADSTRUCTPARAM_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("PayloadStructParam").build();

    private static final MultiLocationOperationRequestMarshaller instance = new MultiLocationOperationRequestMarshaller();

    public static MultiLocationOperationRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(MultiLocationOperationRequest multiLocationOperationRequest, ProtocolMarshaller protocolMarshaller) {

        if (multiLocationOperationRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(multiLocationOperationRequest.getPathParam(), PATHPARAM_BINDING);
            protocolMarshaller.marshall(multiLocationOperationRequest.getQueryParamOne(), QUERYPARAMONE_BINDING);
            protocolMarshaller.marshall(multiLocationOperationRequest.getQueryParamTwo(), QUERYPARAMTWO_BINDING);
            protocolMarshaller.marshall(multiLocationOperationRequest.getStringHeaderMember(), STRINGHEADERMEMBER_BINDING);
            protocolMarshaller.marshall(multiLocationOperationRequest.getTimestampHeaderMember(), TIMESTAMPHEADERMEMBER_BINDING);
            protocolMarshaller.marshall(multiLocationOperationRequest.getPayloadStructParam(), PAYLOADSTRUCTPARAM_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
