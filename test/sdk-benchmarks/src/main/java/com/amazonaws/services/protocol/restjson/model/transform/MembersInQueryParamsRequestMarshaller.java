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
 * MembersInQueryParamsRequestMarshaller
 */

@SdkInternalApi
public class MembersInQueryParamsRequestMarshaller {

    private static final MarshallingInfo<String> STRINGQUERYPARAM_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("String").build();
    private static final MarshallingInfo<Boolean> BOOLEANQUERYPARAM_BINDING = MarshallingInfo.builder(MarshallingType.BOOLEAN)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("Boolean").build();
    private static final MarshallingInfo<Integer> INTEGERQUERYPARAM_BINDING = MarshallingInfo.builder(MarshallingType.INTEGER)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("Integer").build();
    private static final MarshallingInfo<Long> LONGQUERYPARAM_BINDING = MarshallingInfo.builder(MarshallingType.LONG)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("Long").build();
    private static final MarshallingInfo<Float> FLOATQUERYPARAM_BINDING = MarshallingInfo.builder(MarshallingType.FLOAT)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("Float").build();
    private static final MarshallingInfo<Double> DOUBLEQUERYPARAM_BINDING = MarshallingInfo.builder(MarshallingType.DOUBLE)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("Double").build();
    private static final MarshallingInfo<java.util.Date> TIMESTAMPQUERYPARAM_BINDING = MarshallingInfo.builder(MarshallingType.DATE)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("Timestamp").timestampFormat("iso8601").build();
    private static final MarshallingInfo<List> LISTOFSTRINGS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("item").build();
    private static final MarshallingInfo<Map> MAPOFSTRINGTOSTRING_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("MapOfStringToString").build();

    private static final MembersInQueryParamsRequestMarshaller instance = new MembersInQueryParamsRequestMarshaller();

    public static MembersInQueryParamsRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(MembersInQueryParamsRequest membersInQueryParamsRequest, ProtocolMarshaller protocolMarshaller) {

        if (membersInQueryParamsRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(membersInQueryParamsRequest.getStringQueryParam(), STRINGQUERYPARAM_BINDING);
            protocolMarshaller.marshall(membersInQueryParamsRequest.getBooleanQueryParam(), BOOLEANQUERYPARAM_BINDING);
            protocolMarshaller.marshall(membersInQueryParamsRequest.getIntegerQueryParam(), INTEGERQUERYPARAM_BINDING);
            protocolMarshaller.marshall(membersInQueryParamsRequest.getLongQueryParam(), LONGQUERYPARAM_BINDING);
            protocolMarshaller.marshall(membersInQueryParamsRequest.getFloatQueryParam(), FLOATQUERYPARAM_BINDING);
            protocolMarshaller.marshall(membersInQueryParamsRequest.getDoubleQueryParam(), DOUBLEQUERYPARAM_BINDING);
            protocolMarshaller.marshall(membersInQueryParamsRequest.getTimestampQueryParam(), TIMESTAMPQUERYPARAM_BINDING);
            protocolMarshaller.marshall(membersInQueryParamsRequest.getListOfStrings(), LISTOFSTRINGS_BINDING);
            protocolMarshaller.marshall(membersInQueryParamsRequest.getMapOfStringToString(), MAPOFSTRINGTOSTRING_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
