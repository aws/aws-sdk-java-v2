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
package com.amazonaws.services.protocol.restxml.model.transform;

import java.util.Map;

import java.util.ArrayList;
import java.util.Map.Entry;

import javax.xml.stream.events.XMLEvent;


import com.amazonaws.services.protocol.restxml.model.*;
import com.amazonaws.transform.Unmarshaller;
import com.amazonaws.transform.MapEntry;
import com.amazonaws.transform.StaxUnmarshallerContext;
import com.amazonaws.transform.SimpleTypeStaxUnmarshallers.*;

/**
 * AllTypesResult StAX Unmarshaller
 */


public class AllTypesResultStaxUnmarshaller implements Unmarshaller<AllTypesResult, StaxUnmarshallerContext> {

    private static class MapOfStringToStringMapEntryUnmarshaller implements Unmarshaller<Entry<String, String>, StaxUnmarshallerContext> {

        @Override
        public Entry<String, String> unmarshall(StaxUnmarshallerContext context) throws Exception {
            int originalDepth = context.getCurrentDepth();
            int targetDepth = originalDepth + 1;

            MapEntry<String, String> entry = new MapEntry<String, String>();

            while (true) {
                XMLEvent xmlEvent = context.nextEvent();
                if (xmlEvent.isEndDocument())
                    return entry;

                if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {
                    if (context.testExpression("key", targetDepth)) {
                        entry.setKey(StringStaxUnmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                    if (context.testExpression("value", targetDepth)) {
                        entry.setValue(StringStaxUnmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                } else if (xmlEvent.isEndElement()) {
                    if (context.getCurrentDepth() < originalDepth)
                        return entry;
                }
            }
        }

        private static MapOfStringToStringMapEntryUnmarshaller instance;

        public static MapOfStringToStringMapEntryUnmarshaller getInstance() {
            if (instance == null)
                instance = new MapOfStringToStringMapEntryUnmarshaller();
            return instance;
        }

    }

    private static class BlobMapMapEntryUnmarshaller implements Unmarshaller<Entry<String, java.nio.ByteBuffer>, StaxUnmarshallerContext> {

        @Override
        public Entry<String, java.nio.ByteBuffer> unmarshall(StaxUnmarshallerContext context) throws Exception {
            int originalDepth = context.getCurrentDepth();
            int targetDepth = originalDepth + 1;

            MapEntry<String, java.nio.ByteBuffer> entry = new MapEntry<String, java.nio.ByteBuffer>();

            while (true) {
                XMLEvent xmlEvent = context.nextEvent();
                if (xmlEvent.isEndDocument())
                    return entry;

                if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {
                    if (context.testExpression("key", targetDepth)) {
                        entry.setKey(StringStaxUnmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                    if (context.testExpression("value", targetDepth)) {
                        entry.setValue(ByteBufferStaxUnmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                } else if (xmlEvent.isEndElement()) {
                    if (context.getCurrentDepth() < originalDepth)
                        return entry;
                }
            }
        }

        private static BlobMapMapEntryUnmarshaller instance;

        public static BlobMapMapEntryUnmarshaller getInstance() {
            if (instance == null)
                instance = new BlobMapMapEntryUnmarshaller();
            return instance;
        }

    }

    public AllTypesResult unmarshall(StaxUnmarshallerContext context) throws Exception {
        AllTypesResult allTypesResult = new AllTypesResult();
        int originalDepth = context.getCurrentDepth();
        int targetDepth = originalDepth + 1;

        if (context.isStartOfDocument())
            targetDepth += 1;

        while (true) {
            XMLEvent xmlEvent = context.nextEvent();
            if (xmlEvent.isEndDocument())
                return allTypesResult;

            if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {

                if (context.testExpression("stringMember", targetDepth)) {
                    allTypesResult.setStringMember(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("integerMember", targetDepth)) {
                    allTypesResult.setIntegerMember(IntegerStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("booleanMember", targetDepth)) {
                    allTypesResult.setBooleanMember(BooleanStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("floatMember", targetDepth)) {
                    allTypesResult.setFloatMember(FloatStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("doubleMember", targetDepth)) {
                    allTypesResult.setDoubleMember(DoubleStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("longMember", targetDepth)) {
                    allTypesResult.setLongMember(LongStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("simpleStructMember", targetDepth)) {
                    allTypesResult.setSimpleStructMember(SimpleStructStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("simpleList", targetDepth)) {
                    allTypesResult.withSimpleList(new ArrayList<String>());
                    continue;
                }

                if (context.testExpression("simpleList/member", targetDepth)) {
                    allTypesResult.withSimpleList(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("listOfStructs", targetDepth)) {
                    allTypesResult.withListOfStructs(new ArrayList<SimpleStruct>());
                    continue;
                }

                if (context.testExpression("listOfStructs/member", targetDepth)) {
                    allTypesResult.withListOfStructs(SimpleStructStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("mapOfStringToString/entry", targetDepth)) {
                    Entry<String, String> entry = MapOfStringToStringMapEntryUnmarshaller.getInstance().unmarshall(context);
                    allTypesResult.addMapOfStringToStringEntry(entry.getKey(), entry.getValue());
                    continue;
                }

                if (context.testExpression("timestampMember", targetDepth)) {
                    allTypesResult.setTimestampMember(DateStaxUnmarshallerFactory.getInstance("iso8601").unmarshall(context));
                    continue;
                }

                if (context.testExpression("structWithNestedTimestampMember", targetDepth)) {
                    allTypesResult.setStructWithNestedTimestampMember(StructWithTimestampStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("blobArg", targetDepth)) {
                    allTypesResult.setBlobArg(ByteBufferStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("blobMap/entry", targetDepth)) {
                    Entry<String, java.nio.ByteBuffer> entry = BlobMapMapEntryUnmarshaller.getInstance().unmarshall(context);
                    allTypesResult.addBlobMapEntry(entry.getKey(), entry.getValue());
                    continue;
                }

                if (context.testExpression("listOfBlobs", targetDepth)) {
                    allTypesResult.withListOfBlobs(new ArrayList<java.nio.ByteBuffer>());
                    continue;
                }

                if (context.testExpression("listOfBlobs/member", targetDepth)) {
                    allTypesResult.withListOfBlobs(ByteBufferStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

            } else if (xmlEvent.isEndElement()) {
                if (context.getCurrentDepth() < originalDepth) {
                    return allTypesResult;
                }
            }
        }
    }

    private static AllTypesResultStaxUnmarshaller instance;

    public static AllTypesResultStaxUnmarshaller getInstance() {
        if (instance == null)
            instance = new AllTypesResultStaxUnmarshaller();
        return instance;
    }
}
