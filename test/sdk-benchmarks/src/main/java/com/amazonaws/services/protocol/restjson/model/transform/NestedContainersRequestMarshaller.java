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
 * NestedContainersRequestMarshaller
 */

@SdkInternalApi
public class NestedContainersRequestMarshaller {

    private static final MarshallingInfo<List> LISTOFLISTSOFSTRINGS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfListsOfStrings").build();
    private static final MarshallingInfo<List> LISTOFLISTSOFSTRUCTS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfListsOfStructs").build();
    private static final MarshallingInfo<List> LISTOFLISTSOFALLTYPESSTRUCTS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfListsOfAllTypesStructs").build();
    private static final MarshallingInfo<List> LISTOFLISTOFLISTSOFSTRINGS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfListOfListsOfStrings").build();
    private static final MarshallingInfo<Map> MAPOFSTRINGTOLISTOFLISTSOFSTRINGS_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfStringToListOfListsOfStrings").build();
    private static final MarshallingInfo<String> STRINGMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("StringMember").build();

    private static final NestedContainersRequestMarshaller instance = new NestedContainersRequestMarshaller();

    public static NestedContainersRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(NestedContainersRequest nestedContainersRequest, ProtocolMarshaller protocolMarshaller) {

        if (nestedContainersRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(nestedContainersRequest.getListOfListsOfStrings(), LISTOFLISTSOFSTRINGS_BINDING);
            protocolMarshaller.marshall(nestedContainersRequest.getListOfListsOfStructs(), LISTOFLISTSOFSTRUCTS_BINDING);
            protocolMarshaller.marshall(nestedContainersRequest.getListOfListsOfAllTypesStructs(), LISTOFLISTSOFALLTYPESSTRUCTS_BINDING);
            protocolMarshaller.marshall(nestedContainersRequest.getListOfListOfListsOfStrings(), LISTOFLISTOFLISTSOFSTRINGS_BINDING);
            protocolMarshaller.marshall(nestedContainersRequest.getMapOfStringToListOfListsOfStrings(), MAPOFSTRINGTOLISTOFLISTSOFSTRINGS_BINDING);
            protocolMarshaller.marshall(nestedContainersRequest.getStringMember(), STRINGMEMBER_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
