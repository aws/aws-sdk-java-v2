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
import java.math.*;
import java.nio.ByteBuffer;


import com.amazonaws.services.protocol.restjson.model.*;
import com.amazonaws.transform.SimpleTypeJsonUnmarshallers.*;
import com.amazonaws.transform.*;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * AllTypesStructure JSON Unmarshaller
 */

public class AllTypesStructureJsonUnmarshaller implements Unmarshaller<AllTypesStructure, JsonUnmarshallerContext> {

    public AllTypesStructure unmarshall(JsonUnmarshallerContext context) throws Exception {
        AllTypesStructure allTypesStructure = new AllTypesStructure();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return null;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("StringMember", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setStringMember(context.getUnmarshaller(String.class).unmarshall(context));
                }
                if (context.testExpression("IntegerMember", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setIntegerMember(context.getUnmarshaller(Integer.class).unmarshall(context));
                }
                if (context.testExpression("BooleanMember", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setBooleanMember(context.getUnmarshaller(Boolean.class).unmarshall(context));
                }
                if (context.testExpression("FloatMember", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setFloatMember(context.getUnmarshaller(Float.class).unmarshall(context));
                }
                if (context.testExpression("DoubleMember", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setDoubleMember(context.getUnmarshaller(Double.class).unmarshall(context));
                }
                if (context.testExpression("LongMember", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setLongMember(context.getUnmarshaller(Long.class).unmarshall(context));
                }
                if (context.testExpression("SimpleList", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setSimpleList(new ListUnmarshaller<String>(context.getUnmarshaller(String.class))

                    .unmarshall(context));
                }
                if (context.testExpression("ListOfMaps", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setListOfMaps(new ListUnmarshaller<Map<String, String>>(new MapUnmarshaller<String, String>(context
                            .getUnmarshaller(String.class), context.getUnmarshaller(String.class)))

                    .unmarshall(context));
                }
                if (context.testExpression("ListOfStructs", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setListOfStructs(new ListUnmarshaller<SimpleStruct>(SimpleStructJsonUnmarshaller.getInstance())

                    .unmarshall(context));
                }
                if (context.testExpression("MapOfStringToIntegerList", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setMapOfStringToIntegerList(new MapUnmarshaller<String, java.util.List<Integer>>(context.getUnmarshaller(String.class),
                            new ListUnmarshaller<Integer>(context.getUnmarshaller(Integer.class))

                    ).unmarshall(context));
                }
                if (context.testExpression("MapOfStringToString", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setMapOfStringToString(new MapUnmarshaller<String, String>(context.getUnmarshaller(String.class), context
                            .getUnmarshaller(String.class)).unmarshall(context));
                }
                if (context.testExpression("MapOfStringToStruct", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setMapOfStringToStruct(new MapUnmarshaller<String, SimpleStruct>(context.getUnmarshaller(String.class),
                            SimpleStructJsonUnmarshaller.getInstance()).unmarshall(context));
                }
                if (context.testExpression("TimestampMember", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setTimestampMember(DateJsonUnmarshallerFactory.getInstance("unixTimestamp").unmarshall(context));
                }
                if (context.testExpression("StructWithNestedTimestampMember", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setStructWithNestedTimestampMember(StructWithTimestampJsonUnmarshaller.getInstance().unmarshall(context));
                }
                if (context.testExpression("BlobArg", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setBlobArg(context.getUnmarshaller(ByteBuffer.class).unmarshall(context));
                }
                if (context.testExpression("StructWithNestedBlob", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setStructWithNestedBlob(StructWithNestedBlobTypeJsonUnmarshaller.getInstance().unmarshall(context));
                }
                if (context.testExpression("BlobMap", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setBlobMap(new MapUnmarshaller<String, ByteBuffer>(context.getUnmarshaller(String.class), context
                            .getUnmarshaller(ByteBuffer.class)).unmarshall(context));
                }
                if (context.testExpression("ListOfBlobs", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setListOfBlobs(new ListUnmarshaller<ByteBuffer>(context.getUnmarshaller(ByteBuffer.class))

                    .unmarshall(context));
                }
                if (context.testExpression("RecursiveStruct", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setRecursiveStruct(RecursiveStructTypeJsonUnmarshaller.getInstance().unmarshall(context));
                }
                if (context.testExpression("PolymorphicTypeWithSubTypes", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setPolymorphicTypeWithSubTypes(BaseTypeJsonUnmarshaller.getInstance().unmarshall(context));
                }
                if (context.testExpression("PolymorphicTypeWithoutSubTypes", targetDepth)) {
                    context.nextToken();
                    allTypesStructure.setPolymorphicTypeWithoutSubTypes(SubTypeOneJsonUnmarshaller.getInstance().unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return allTypesStructure;
    }

    private static AllTypesStructureJsonUnmarshaller instance;

    public static AllTypesStructureJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new AllTypesStructureJsonUnmarshaller();
        return instance;
    }
}
