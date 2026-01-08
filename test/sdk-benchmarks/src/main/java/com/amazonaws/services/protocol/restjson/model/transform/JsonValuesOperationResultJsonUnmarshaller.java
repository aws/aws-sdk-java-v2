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
 * JsonValuesOperationResult JSON Unmarshaller
 */

public class JsonValuesOperationResultJsonUnmarshaller implements Unmarshaller<JsonValuesOperationResult, JsonUnmarshallerContext> {

    public JsonValuesOperationResult unmarshall(JsonUnmarshallerContext context) throws Exception {
        JsonValuesOperationResult jsonValuesOperationResult = new JsonValuesOperationResult();

        if (context.isStartOfDocument()) {
            if (context.getHeader("Encoded-Header") != null) {
                context.setCurrentHeader("Encoded-Header");
                jsonValuesOperationResult.setJsonValueHeaderMember(context.getUnmarshaller(String.class, JsonUnmarshallerContext.UnmarshallerType.JSON_VALUE)
                        .unmarshall(context));
            }
        }

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return jsonValuesOperationResult;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("JsonValueMember", targetDepth)) {
                    context.nextToken();
                    jsonValuesOperationResult.setJsonValueMember(context.getUnmarshaller(String.class, JsonUnmarshallerContext.UnmarshallerType.JSON_VALUE)
                            .unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return jsonValuesOperationResult;
    }

    private static JsonValuesOperationResultJsonUnmarshaller instance;

    public static JsonValuesOperationResultJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new JsonValuesOperationResultJsonUnmarshaller();
        return instance;
    }
}
