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
 * IdempotentOperationResult StAX Unmarshaller
 */


public class IdempotentOperationResultStaxUnmarshaller implements Unmarshaller<IdempotentOperationResult, StaxUnmarshallerContext> {

    public IdempotentOperationResult unmarshall(StaxUnmarshallerContext context) throws Exception {
        IdempotentOperationResult idempotentOperationResult = new IdempotentOperationResult();
        int originalDepth = context.getCurrentDepth();
        int targetDepth = originalDepth + 1;

        if (context.isStartOfDocument())
            targetDepth += 2;

        while (true) {
            XMLEvent xmlEvent = context.nextEvent();
            if (xmlEvent.isEndDocument())
                return idempotentOperationResult;

            if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {

                if (context.testExpression("IdempotencyToken", targetDepth)) {
                    idempotentOperationResult.setIdempotencyToken(StringStaxUnmarshaller.getInstance().unmarshall(context));
                    continue;
                }
            } else if (xmlEvent.isEndElement()) {
                if (context.getCurrentDepth() < originalDepth) {
                    return idempotentOperationResult;
                }
            }
        }
    }

    private static IdempotentOperationResultStaxUnmarshaller instance;

    public static IdempotentOperationResultStaxUnmarshaller getInstance() {
        if (instance == null)
            instance = new IdempotentOperationResultStaxUnmarshaller();
        return instance;
    }
}
