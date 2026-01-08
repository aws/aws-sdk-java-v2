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
 * RestXmlTypesResult StAX Unmarshaller
 */


public class RestXmlTypesResultStaxUnmarshaller implements Unmarshaller<RestXmlTypesResult, StaxUnmarshallerContext> {

    private static class FlattenedMapMapEntryUnmarshaller implements Unmarshaller<Entry<String, String>, StaxUnmarshallerContext> {

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

        private static FlattenedMapMapEntryUnmarshaller instance;

        public static FlattenedMapMapEntryUnmarshaller getInstance() {
            if (instance == null)
                instance = new FlattenedMapMapEntryUnmarshaller();
            return instance;
        }

    }

    private static class FlattenedMapWithLocationMapEntryUnmarshaller implements Unmarshaller<Entry<String, String>, StaxUnmarshallerContext> {

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
                    if (context.testExpression("thekey", targetDepth)) {
                        entry.setKey(StringStaxUnmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                    if (context.testExpression("thevalue", targetDepth)) {
                        entry.setValue(StringStaxUnmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                } else if (xmlEvent.isEndElement()) {
                    if (context.getCurrentDepth() < originalDepth)
                        return entry;
                }
            }
        }

        private static FlattenedMapWithLocationMapEntryUnmarshaller instance;

        public static FlattenedMapWithLocationMapEntryUnmarshaller getInstance() {
            if (instance == null)
                instance = new FlattenedMapWithLocationMapEntryUnmarshaller();
            return instance;
        }

    }

    private static class NonFlattenedMapWithLocationMapEntryUnmarshaller implements Unmarshaller<Entry<String, String>, StaxUnmarshallerContext> {

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
                    if (context.testExpression("thekey", targetDepth)) {
                        entry.setKey(StringStaxUnmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                    if (context.testExpression("thevalue", targetDepth)) {
                        entry.setValue(StringStaxUnmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                } else if (xmlEvent.isEndElement()) {
                    if (context.getCurrentDepth() < originalDepth)
                        return entry;
                }
            }
        }

        private static NonFlattenedMapWithLocationMapEntryUnmarshaller instance;

        public static NonFlattenedMapWithLocationMapEntryUnmarshaller getInstance() {
            if (instance == null)
                instance = new NonFlattenedMapWithLocationMapEntryUnmarshaller();
            return instance;
        }

    }

    private static class MapOfStringToStringInQueryMapEntryUnmarshaller implements Unmarshaller<Entry<String, String>, StaxUnmarshallerContext> {

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

        private static MapOfStringToStringInQueryMapEntryUnmarshaller instance;

        public static MapOfStringToStringInQueryMapEntryUnmarshaller getInstance() {
            if (instance == null)
                instance = new MapOfStringToStringInQueryMapEntryUnmarshaller();
            return instance;
        }

    }

    public RestXmlTypesResult unmarshall(StaxUnmarshallerContext context) throws Exception {
        RestXmlTypesResult restXmlTypesResult = new RestXmlTypesResult();
        int originalDepth = context.getCurrentDepth();
        int targetDepth = originalDepth + 1;

        if (context.isStartOfDocument())
            targetDepth += 1;

        if (context.isStartOfDocument()) {
            context.setCurrentHeader("x-amz-timearg");
            restXmlTypesResult.setTimestampMemberInHeader(DateStaxUnmarshallerFactory.getInstance("rfc822").unmarshall(context));

        }

        while (true) {
            XMLEvent xmlEvent = context.nextEvent();
            if (xmlEvent.isEndDocument())
                return restXmlTypesResult;

            if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {

                if (context.testExpression("FlattenedListOfStrings", targetDepth)) {
                    restXmlTypesResult.withFlattenedListOfStrings(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("NonFlattenedListWithLocation", targetDepth)) {
                    restXmlTypesResult.withNonFlattenedListWithLocation(new ArrayList<String>());
                    continue;
                }

                if (context.testExpression("NonFlattenedListWithLocation/item", targetDepth)) {
                    restXmlTypesResult.withNonFlattenedListWithLocation(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("FlattenedListOfStructs", targetDepth)) {
                    restXmlTypesResult.withFlattenedListOfStructs(SimpleStructStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("item", targetDepth)) {
                    restXmlTypesResult.withFlattenedListWithLocation(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("FlattenedMap", targetDepth)) {
                    Entry<String, String> entry = FlattenedMapMapEntryUnmarshaller.getInstance().unmarshall(context);
                    restXmlTypesResult.addFlattenedMapEntry(entry.getKey(), entry.getValue());
                    continue;
                }

                if (context.testExpression("flatmap", targetDepth)) {
                    Entry<String, String> entry = FlattenedMapWithLocationMapEntryUnmarshaller.getInstance().unmarshall(context);
                    restXmlTypesResult.addFlattenedMapWithLocationEntry(entry.getKey(), entry.getValue());
                    continue;
                }

                if (context.testExpression("themap/entry", targetDepth)) {
                    Entry<String, String> entry = NonFlattenedMapWithLocationMapEntryUnmarshaller.getInstance().unmarshall(context);
                    restXmlTypesResult.addNonFlattenedMapWithLocationEntry(entry.getKey(), entry.getValue());
                    continue;
                }

                if (context.testExpression("stringMemberInQuery", targetDepth)) {
                    restXmlTypesResult.setStringMemberInQuery(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("listOfStrings", targetDepth)) {
                    restXmlTypesResult.withListOfStringsInQuery(new ArrayList<String>());
                    continue;
                }

                if (context.testExpression("listOfStrings/member", targetDepth)) {
                    restXmlTypesResult.withListOfStringsInQuery(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("MapOfStringToStringInQuery/entry", targetDepth)) {
                    Entry<String, String> entry = MapOfStringToStringInQueryMapEntryUnmarshaller.getInstance().unmarshall(context);
                    restXmlTypesResult.addMapOfStringToStringInQueryEntry(entry.getKey(), entry.getValue());
                    continue;
                }

            } else if (xmlEvent.isEndElement()) {
                if (context.getCurrentDepth() < originalDepth) {
                    return restXmlTypesResult;
                }
            }
        }
    }

    private static RestXmlTypesResultStaxUnmarshaller instance;

    public static RestXmlTypesResultStaxUnmarshaller getInstance() {
        if (instance == null)
            instance = new RestXmlTypesResultStaxUnmarshaller();
        return instance;
    }
}
