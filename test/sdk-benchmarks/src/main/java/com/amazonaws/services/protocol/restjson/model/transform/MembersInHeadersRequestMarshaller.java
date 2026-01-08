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
 * MembersInHeadersRequestMarshaller
 */

@SdkInternalApi
public class MembersInHeadersRequestMarshaller {

    private static final MarshallingInfo<String> STRINGMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.HEADER).marshallLocationName("x-amz-string").build();
    private static final MarshallingInfo<Boolean> BOOLEANMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.BOOLEAN)
            .marshallLocation(MarshallLocation.HEADER).marshallLocationName("x-amz-boolean").build();
    private static final MarshallingInfo<Integer> INTEGERMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.INTEGER)
            .marshallLocation(MarshallLocation.HEADER).marshallLocationName("x-amz-integer").build();
    private static final MarshallingInfo<Long> LONGMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.LONG).marshallLocation(MarshallLocation.HEADER)
            .marshallLocationName("x-amz-long").build();
    private static final MarshallingInfo<Float> FLOATMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.FLOAT).marshallLocation(MarshallLocation.HEADER)
            .marshallLocationName("x-amz-float").build();
    private static final MarshallingInfo<Double> DOUBLEMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.DOUBLE)
            .marshallLocation(MarshallLocation.HEADER).marshallLocationName("x-amz-double").build();
    private static final MarshallingInfo<java.util.Date> TIMESTAMPMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.DATE)
            .marshallLocation(MarshallLocation.HEADER).marshallLocationName("x-amz-timestamp").timestampFormat("unknown").build();

    private static final MembersInHeadersRequestMarshaller instance = new MembersInHeadersRequestMarshaller();

    public static MembersInHeadersRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(MembersInHeadersRequest membersInHeadersRequest, ProtocolMarshaller protocolMarshaller) {

        if (membersInHeadersRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(membersInHeadersRequest.getStringMember(), STRINGMEMBER_BINDING);
            protocolMarshaller.marshall(membersInHeadersRequest.getBooleanMember(), BOOLEANMEMBER_BINDING);
            protocolMarshaller.marshall(membersInHeadersRequest.getIntegerMember(), INTEGERMEMBER_BINDING);
            protocolMarshaller.marshall(membersInHeadersRequest.getLongMember(), LONGMEMBER_BINDING);
            protocolMarshaller.marshall(membersInHeadersRequest.getFloatMember(), FLOATMEMBER_BINDING);
            protocolMarshaller.marshall(membersInHeadersRequest.getDoubleMember(), DOUBLEMEMBER_BINDING);
            protocolMarshaller.marshall(membersInHeadersRequest.getTimestampMember(), TIMESTAMPMEMBER_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
