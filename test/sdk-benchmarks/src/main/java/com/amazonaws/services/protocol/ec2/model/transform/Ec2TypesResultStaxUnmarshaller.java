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
package com.amazonaws.services.protocol.ec2.model.transform;

import java.util.ArrayList;

import javax.xml.stream.events.XMLEvent;


import com.amazonaws.services.protocol.ec2.model.*;
import com.amazonaws.transform.Unmarshaller;

import com.amazonaws.transform.StaxUnmarshallerContext;
import com.amazonaws.transform.SimpleTypeStaxUnmarshallers.*;

/**
 * Ec2TypesResult StAX Unmarshaller
 */


public class Ec2TypesResultStaxUnmarshaller implements Unmarshaller<Ec2TypesResult, StaxUnmarshallerContext> {

    public Ec2TypesResult unmarshall(StaxUnmarshallerContext context) throws Exception {
        Ec2TypesResult ec2TypesResult = new Ec2TypesResult();
        int originalDepth = context.getCurrentDepth();
        int targetDepth = originalDepth + 1;

        if (context.isStartOfDocument())
            targetDepth += 1;

        while (true) {
            XMLEvent xmlEvent = context.nextEvent();
            if (xmlEvent.isEndDocument())
                return ec2TypesResult;

            if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {

                if (context.testExpression("FlattenedListOfStrings", targetDepth)) {
                    ec2TypesResult.withFlattenedListOfStrings(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("FlattenedListOfStructs", targetDepth)) {
                    ec2TypesResult.withFlattenedListOfStructs(SimpleStructStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("item", targetDepth)) {
                    ec2TypesResult.withFlattenedListWithLocation(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("someLocation", targetDepth)) {
                    ec2TypesResult.setStringMemberWithLocation(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("StringMemberWithQueryName", targetDepth)) {
                    ec2TypesResult.setStringMemberWithQueryName(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("StringMemberWithLocationAndQueryName", targetDepth)) {
                    ec2TypesResult.setStringMemberWithLocationAndQueryName(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("someLocation", targetDepth)) {
                    ec2TypesResult.withListMemberWithLocationAndQueryName(new ArrayList<String>());
                    continue;
                }

                if (context.testExpression("someLocation/member", targetDepth)) {
                    ec2TypesResult.withListMemberWithLocationAndQueryName(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

                if (context.testExpression("item", targetDepth)) {
                    ec2TypesResult.withListMemberWithOnlyMemberLocation(new ArrayList<String>());
                    continue;
                }

                if (context.testExpression("item/member", targetDepth)) {
                    ec2TypesResult.withListMemberWithOnlyMemberLocation(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }

            } else if (xmlEvent.isEndElement()) {
                if (context.getCurrentDepth() < originalDepth) {
                    return ec2TypesResult;
                }
            }
        }
    }

    private static Ec2TypesResultStaxUnmarshaller instance;

    public static Ec2TypesResultStaxUnmarshaller getInstance() {
        if (instance == null)
            instance = new Ec2TypesResultStaxUnmarshaller();
        return instance;
    }
}
