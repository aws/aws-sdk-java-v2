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
 * AllTypesResult JSON Unmarshaller
 */

public class AllTypesResultJsonUnmarshaller implements Unmarshaller<AllTypesResult, JsonUnmarshallerContext> {

    public AllTypesResult unmarshall(JsonUnmarshallerContext context) throws Exception {
        AllTypesResult allTypesResult = new AllTypesResult();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return allTypesResult;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("StringMember", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setStringMember(context.getUnmarshaller(String.class).unmarshall(context));
                }
                if (context.testExpression("IntegerMember", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setIntegerMember(context.getUnmarshaller(Integer.class).unmarshall(context));
                }
                if (context.testExpression("BooleanMember", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setBooleanMember(context.getUnmarshaller(Boolean.class).unmarshall(context));
                }
                if (context.testExpression("FloatMember", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setFloatMember(context.getUnmarshaller(Float.class).unmarshall(context));
                }
                if (context.testExpression("DoubleMember", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setDoubleMember(context.getUnmarshaller(Double.class).unmarshall(context));
                }
                if (context.testExpression("LongMember", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setLongMember(context.getUnmarshaller(Long.class).unmarshall(context));
                }
                if (context.testExpression("SimpleList", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setSimpleList(new ListUnmarshaller<String>(context.getUnmarshaller(String.class))

                    .unmarshall(context));
                }
                if (context.testExpression("ListOfMaps", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setListOfMaps(new ListUnmarshaller<Map<String, String>>(new MapUnmarshaller<String, String>(context
                            .getUnmarshaller(String.class), context.getUnmarshaller(String.class)))

                    .unmarshall(context));
                }
                if (context.testExpression("ListOfStructs", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setListOfStructs(new ListUnmarshaller<SimpleStruct>(SimpleStructJsonUnmarshaller.getInstance())

                    .unmarshall(context));
                }
                if (context.testExpression("MapOfStringToIntegerList", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setMapOfStringToIntegerList(new MapUnmarshaller<String, java.util.List<Integer>>(context.getUnmarshaller(String.class),
                            new ListUnmarshaller<Integer>(context.getUnmarshaller(Integer.class))

                    ).unmarshall(context));
                }
                if (context.testExpression("MapOfStringToString", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setMapOfStringToString(new MapUnmarshaller<String, String>(context.getUnmarshaller(String.class), context
                            .getUnmarshaller(String.class)).unmarshall(context));
                }
                if (context.testExpression("MapOfStringToStruct", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setMapOfStringToStruct(new MapUnmarshaller<String, SimpleStruct>(context.getUnmarshaller(String.class),
                            SimpleStructJsonUnmarshaller.getInstance()).unmarshall(context));
                }
                if (context.testExpression("TimestampMember", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setTimestampMember(DateJsonUnmarshallerFactory.getInstance("unixTimestamp").unmarshall(context));
                }
                if (context.testExpression("StructWithNestedTimestampMember", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setStructWithNestedTimestampMember(StructWithTimestampJsonUnmarshaller.getInstance().unmarshall(context));
                }
                if (context.testExpression("BlobArg", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setBlobArg(context.getUnmarshaller(ByteBuffer.class).unmarshall(context));
                }
                if (context.testExpression("StructWithNestedBlob", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setStructWithNestedBlob(StructWithNestedBlobTypeJsonUnmarshaller.getInstance().unmarshall(context));
                }
                if (context.testExpression("BlobMap", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setBlobMap(new MapUnmarshaller<String, ByteBuffer>(context.getUnmarshaller(String.class), context
                            .getUnmarshaller(ByteBuffer.class)).unmarshall(context));
                }
                if (context.testExpression("ListOfBlobs", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setListOfBlobs(new ListUnmarshaller<ByteBuffer>(context.getUnmarshaller(ByteBuffer.class))

                    .unmarshall(context));
                }
                if (context.testExpression("RecursiveStruct", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setRecursiveStruct(RecursiveStructTypeJsonUnmarshaller.getInstance().unmarshall(context));
                }
                if (context.testExpression("PolymorphicTypeWithSubTypes", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setPolymorphicTypeWithSubTypes(BaseTypeJsonUnmarshaller.getInstance().unmarshall(context));
                }
                if (context.testExpression("PolymorphicTypeWithoutSubTypes", targetDepth)) {
                    context.nextToken();
                    allTypesResult.setPolymorphicTypeWithoutSubTypes(SubTypeOneJsonUnmarshaller.getInstance().unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return allTypesResult;
    }

    private static AllTypesResultJsonUnmarshaller instance;

    public static AllTypesResultJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new AllTypesResultJsonUnmarshaller();
        return instance;
    }
}
