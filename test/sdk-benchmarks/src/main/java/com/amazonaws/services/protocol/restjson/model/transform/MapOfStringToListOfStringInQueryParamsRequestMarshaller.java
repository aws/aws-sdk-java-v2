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



import com.amazonaws.SdkClientException;
import com.amazonaws.services.protocol.restjson.model.*;

import com.amazonaws.protocol.*;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * MapOfStringToListOfStringInQueryParamsRequestMarshaller
 */

@SdkInternalApi
public class MapOfStringToListOfStringInQueryParamsRequestMarshaller {

    private static final MarshallingInfo<Map> MAPOFSTRINGTOLISTOFSTRINGS_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
            .marshallLocation(MarshallLocation.QUERY_PARAM).marshallLocationName("MapOfStringToListOfStrings").build();

    private static final MapOfStringToListOfStringInQueryParamsRequestMarshaller instance = new MapOfStringToListOfStringInQueryParamsRequestMarshaller();

    public static MapOfStringToListOfStringInQueryParamsRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(MapOfStringToListOfStringInQueryParamsRequest mapOfStringToListOfStringInQueryParamsRequest, ProtocolMarshaller protocolMarshaller) {

        if (mapOfStringToListOfStringInQueryParamsRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(mapOfStringToListOfStringInQueryParamsRequest.getMapOfStringToListOfStrings(), MAPOFSTRINGTOLISTOFSTRINGS_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
