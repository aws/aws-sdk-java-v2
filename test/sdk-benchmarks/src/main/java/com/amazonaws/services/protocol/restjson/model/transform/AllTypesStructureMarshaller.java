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
 * AllTypesStructureMarshaller
 */

@SdkInternalApi
public class AllTypesStructureMarshaller {

    private static final MarshallingInfo<String> STRINGMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("StringMember").build();
    private static final MarshallingInfo<Integer> INTEGERMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.INTEGER)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("IntegerMember").build();
    private static final MarshallingInfo<Boolean> BOOLEANMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.BOOLEAN)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("BooleanMember").build();
    private static final MarshallingInfo<Float> FLOATMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.FLOAT).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("FloatMember").build();
    private static final MarshallingInfo<Double> DOUBLEMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.DOUBLE)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("DoubleMember").build();
    private static final MarshallingInfo<Long> LONGMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.LONG).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("LongMember").build();
    private static final MarshallingInfo<List> SIMPLELIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("SimpleList").build();
    private static final MarshallingInfo<List> LISTOFMAPS_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("ListOfMaps").build();
    private static final MarshallingInfo<List> LISTOFSTRUCTS_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("ListOfStructs").build();
    private static final MarshallingInfo<Map> MAPOFSTRINGTOINTEGERLIST_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfStringToIntegerList").build();
    private static final MarshallingInfo<Map> MAPOFSTRINGTOSTRING_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfStringToString").build();
    private static final MarshallingInfo<Map> MAPOFSTRINGTOSTRUCT_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfStringToStruct").build();
    private static final MarshallingInfo<java.util.Date> TIMESTAMPMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.DATE)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("TimestampMember").timestampFormat("unixTimestamp").build();
    private static final MarshallingInfo<StructuredPojo> STRUCTWITHNESTEDTIMESTAMPMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("StructWithNestedTimestampMember").build();
    private static final MarshallingInfo<java.nio.ByteBuffer> BLOBARG_BINDING = MarshallingInfo.builder(MarshallingType.BYTE_BUFFER)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("BlobArg").build();
    private static final MarshallingInfo<StructuredPojo> STRUCTWITHNESTEDBLOB_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("StructWithNestedBlob").build();
    private static final MarshallingInfo<Map> BLOBMAP_BINDING = MarshallingInfo.builder(MarshallingType.MAP).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("BlobMap").build();
    private static final MarshallingInfo<List> LISTOFBLOBS_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("ListOfBlobs").build();
    private static final MarshallingInfo<StructuredPojo> RECURSIVESTRUCT_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("RecursiveStruct").build();
    private static final MarshallingInfo<StructuredPojo> POLYMORPHICTYPEWITHSUBTYPES_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("PolymorphicTypeWithSubTypes").build();
    private static final MarshallingInfo<StructuredPojo> POLYMORPHICTYPEWITHOUTSUBTYPES_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("PolymorphicTypeWithoutSubTypes").build();

    private static final AllTypesStructureMarshaller instance = new AllTypesStructureMarshaller();

    public static AllTypesStructureMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(AllTypesStructure allTypesStructure, ProtocolMarshaller protocolMarshaller) {

        if (allTypesStructure == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(allTypesStructure.getStringMember(), STRINGMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getIntegerMember(), INTEGERMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getBooleanMember(), BOOLEANMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getFloatMember(), FLOATMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getDoubleMember(), DOUBLEMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getLongMember(), LONGMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getSimpleList(), SIMPLELIST_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getListOfMaps(), LISTOFMAPS_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getListOfStructs(), LISTOFSTRUCTS_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getMapOfStringToIntegerList(), MAPOFSTRINGTOINTEGERLIST_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getMapOfStringToString(), MAPOFSTRINGTOSTRING_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getMapOfStringToStruct(), MAPOFSTRINGTOSTRUCT_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getTimestampMember(), TIMESTAMPMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getStructWithNestedTimestampMember(), STRUCTWITHNESTEDTIMESTAMPMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getBlobArg(), BLOBARG_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getStructWithNestedBlob(), STRUCTWITHNESTEDBLOB_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getBlobMap(), BLOBMAP_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getListOfBlobs(), LISTOFBLOBS_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getRecursiveStruct(), RECURSIVESTRUCT_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getPolymorphicTypeWithSubTypes(), POLYMORPHICTYPEWITHSUBTYPES_BINDING);
            protocolMarshaller.marshall(allTypesStructure.getPolymorphicTypeWithoutSubTypes(), POLYMORPHICTYPEWITHOUTSUBTYPES_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
