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
 * SimpleScalarPropertiesRequestMarshaller
 */

@SdkInternalApi
public class SimpleScalarPropertiesRequestMarshaller {

    private static final MarshallingInfo<Boolean> TRUEBOOLEANVALUE_BINDING = MarshallingInfo.builder(MarshallingType.BOOLEAN)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("trueBooleanValue").build();
    private static final MarshallingInfo<Boolean> FALSEBOOLEANVALUE_BINDING = MarshallingInfo.builder(MarshallingType.BOOLEAN)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("falseBooleanValue").build();
    private static final MarshallingInfo<Integer> BYTEVALUE_BINDING = MarshallingInfo.builder(MarshallingType.INTEGER)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("byteValue").build();
    private static final MarshallingInfo<Double> DOUBLEVALUE_BINDING = MarshallingInfo.builder(MarshallingType.DOUBLE)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("doubleValue").build();
    private static final MarshallingInfo<Float> FLOATVALUE_BINDING = MarshallingInfo.builder(MarshallingType.FLOAT).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("floatValue").build();
    private static final MarshallingInfo<Integer> INTEGERVALUE_BINDING = MarshallingInfo.builder(MarshallingType.INTEGER)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("integerValue").build();
    private static final MarshallingInfo<Long> LONGVALUE_BINDING = MarshallingInfo.builder(MarshallingType.LONG).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("longValue").build();
    private static final MarshallingInfo<Integer> SHORTVALUE_BINDING = MarshallingInfo.builder(MarshallingType.INTEGER)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("shortValue").build();
    private static final MarshallingInfo<String> STRINGVALUE_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("stringValue").build();
    private static final MarshallingInfo<java.nio.ByteBuffer> BLOBVALUE_BINDING = MarshallingInfo.builder(MarshallingType.BYTE_BUFFER)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("blobValue").build();

    private static final SimpleScalarPropertiesRequestMarshaller instance = new SimpleScalarPropertiesRequestMarshaller();

    public static SimpleScalarPropertiesRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(SimpleScalarPropertiesRequest simpleScalarPropertiesRequest, ProtocolMarshaller protocolMarshaller) {

        if (simpleScalarPropertiesRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getTrueBooleanValue(), TRUEBOOLEANVALUE_BINDING);
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getFalseBooleanValue(), FALSEBOOLEANVALUE_BINDING);
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getByteValue(), BYTEVALUE_BINDING);
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getDoubleValue(), DOUBLEVALUE_BINDING);
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getFloatValue(), FLOATVALUE_BINDING);
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getIntegerValue(), INTEGERVALUE_BINDING);
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getLongValue(), LONGVALUE_BINDING);
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getShortValue(), SHORTVALUE_BINDING);
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getStringValue(), STRINGVALUE_BINDING);
            protocolMarshaller.marshall(simpleScalarPropertiesRequest.getBlobValue(), BLOBVALUE_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request: " + e.getMessage(), e);
        }
    }

}
