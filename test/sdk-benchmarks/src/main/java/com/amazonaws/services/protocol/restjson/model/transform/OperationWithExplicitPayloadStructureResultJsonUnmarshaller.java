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

import java.math.*;



import com.amazonaws.services.protocol.restjson.model.*;
import com.amazonaws.transform.SimpleTypeJsonUnmarshallers.*;
import com.amazonaws.transform.*;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * OperationWithExplicitPayloadStructureResult JSON Unmarshaller
 */

public class OperationWithExplicitPayloadStructureResultJsonUnmarshaller implements
        Unmarshaller<OperationWithExplicitPayloadStructureResult, JsonUnmarshallerContext> {

    public OperationWithExplicitPayloadStructureResult unmarshall(JsonUnmarshallerContext context) throws Exception {
        OperationWithExplicitPayloadStructureResult operationWithExplicitPayloadStructureResult = new OperationWithExplicitPayloadStructureResult();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return operationWithExplicitPayloadStructureResult;
        }

        while (true) {
            if (token == null)
                break;

            operationWithExplicitPayloadStructureResult.setPayloadMember(SimpleStructJsonUnmarshaller.getInstance().unmarshall(context));
            token = context.nextToken();
        }

        return operationWithExplicitPayloadStructureResult;
    }

    private static OperationWithExplicitPayloadStructureResultJsonUnmarshaller instance;

    public static OperationWithExplicitPayloadStructureResultJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new OperationWithExplicitPayloadStructureResultJsonUnmarshaller();
        return instance;
    }
}
