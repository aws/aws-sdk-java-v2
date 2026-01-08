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
package com.amazonaws.services.protocol.query.model.transform;

import javax.xml.stream.events.XMLEvent;


import com.amazonaws.services.protocol.query.model.*;
import com.amazonaws.transform.Unmarshaller;

import com.amazonaws.transform.StaxUnmarshallerContext;
import com.amazonaws.transform.SimpleTypeStaxUnmarshallers.*;

/**
 * SimpleStruct StAX Unmarshaller
 */


public class SimpleStructStaxUnmarshaller implements Unmarshaller<SimpleStruct, StaxUnmarshallerContext> {

    public SimpleStruct unmarshall(StaxUnmarshallerContext context) throws Exception {
        SimpleStruct simpleStruct = new SimpleStruct();
        int originalDepth = context.getCurrentDepth();
        int targetDepth = originalDepth + 1;

        if (context.isStartOfDocument())
            targetDepth += 1;

        while (true) {
            XMLEvent xmlEvent = context.nextEvent();
            if (xmlEvent.isEndDocument())
                return simpleStruct;

            if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {

                if (context.testExpression("StringMember", targetDepth)) {
                    simpleStruct.setStringMember(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }
            } else if (xmlEvent.isEndElement()) {
                if (context.getCurrentDepth() < originalDepth) {
                    return simpleStruct;
                }
            }
        }
    }

    private static SimpleStructStaxUnmarshaller instance;

    public static SimpleStructStaxUnmarshaller getInstance() {
        if (instance == null)
            instance = new SimpleStructStaxUnmarshaller();
        return instance;
    }
}
