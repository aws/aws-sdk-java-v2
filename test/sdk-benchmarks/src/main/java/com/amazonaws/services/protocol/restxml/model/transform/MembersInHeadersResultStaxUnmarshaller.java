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

import javax.xml.stream.events.XMLEvent;


import com.amazonaws.services.protocol.restxml.model.*;
import com.amazonaws.transform.Unmarshaller;

import com.amazonaws.transform.StaxUnmarshallerContext;
import com.amazonaws.transform.SimpleTypeStaxUnmarshallers.*;

/**
 * MembersInHeadersResult StAX Unmarshaller
 */


public class MembersInHeadersResultStaxUnmarshaller implements Unmarshaller<MembersInHeadersResult, StaxUnmarshallerContext> {

    public MembersInHeadersResult unmarshall(StaxUnmarshallerContext context) throws Exception {
        MembersInHeadersResult membersInHeadersResult = new MembersInHeadersResult();
        int originalDepth = context.getCurrentDepth();
        int targetDepth = originalDepth + 1;

        if (context.isStartOfDocument())
            targetDepth += 1;

        if (context.isStartOfDocument()) {
            context.setCurrentHeader("x-amz-string");
            membersInHeadersResult.setStringMember(StringStaxUnmarshaller.getInstance().unmarshall(context));

            context.setCurrentHeader("x-amz-boolean");
            membersInHeadersResult.setBooleanMember(BooleanStaxUnmarshaller.getInstance().unmarshall(context));

            context.setCurrentHeader("x-amz-integer");
            membersInHeadersResult.setIntegerMember(IntegerStaxUnmarshaller.getInstance().unmarshall(context));

            context.setCurrentHeader("x-amz-long");
            membersInHeadersResult.setLongMember(LongStaxUnmarshaller.getInstance().unmarshall(context));

            context.setCurrentHeader("x-amz-float");
            membersInHeadersResult.setFloatMember(FloatStaxUnmarshaller.getInstance().unmarshall(context));

            context.setCurrentHeader("x-amz-double");
            membersInHeadersResult.setDoubleMember(DoubleStaxUnmarshaller.getInstance().unmarshall(context));

            context.setCurrentHeader("x-amz-timestamp");
            membersInHeadersResult.setTimestampMember(DateStaxUnmarshallerFactory.getInstance("rfc822").unmarshall(context));

        }

        while (true) {
            XMLEvent xmlEvent = context.nextEvent();
            if (xmlEvent.isEndDocument())
                return membersInHeadersResult;

            if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {

            } else if (xmlEvent.isEndElement()) {
                if (context.getCurrentDepth() < originalDepth) {
                    return membersInHeadersResult;
                }
            }
        }
    }

    private static MembersInHeadersResultStaxUnmarshaller instance;

    public static MembersInHeadersResultStaxUnmarshaller getInstance() {
        if (instance == null)
            instance = new MembersInHeadersResultStaxUnmarshaller();
        return instance;
    }
}
