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
 * RecursiveStructType JSON Unmarshaller
 */

public class RecursiveStructTypeJsonUnmarshaller implements Unmarshaller<RecursiveStructType, JsonUnmarshallerContext> {

    public RecursiveStructType unmarshall(JsonUnmarshallerContext context) throws Exception {
        RecursiveStructType recursiveStructType = new RecursiveStructType();

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
                if (context.testExpression("NoRecurse", targetDepth)) {
                    context.nextToken();
                    recursiveStructType.setNoRecurse(context.getUnmarshaller(String.class).unmarshall(context));
                }
                if (context.testExpression("RecursiveStruct", targetDepth)) {
                    context.nextToken();
                    recursiveStructType.setRecursiveStruct(RecursiveStructTypeJsonUnmarshaller.getInstance().unmarshall(context));
                }
                if (context.testExpression("RecursiveList", targetDepth)) {
                    context.nextToken();
                    recursiveStructType.setRecursiveList(new ListUnmarshaller<RecursiveStructType>(RecursiveStructTypeJsonUnmarshaller.getInstance())

                    .unmarshall(context));
                }
                if (context.testExpression("RecursiveMap", targetDepth)) {
                    context.nextToken();
                    recursiveStructType.setRecursiveMap(new MapUnmarshaller<String, RecursiveStructType>(context.getUnmarshaller(String.class),
                            RecursiveStructTypeJsonUnmarshaller.getInstance()).unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return recursiveStructType;
    }

    private static RecursiveStructTypeJsonUnmarshaller instance;

    public static RecursiveStructTypeJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new RecursiveStructTypeJsonUnmarshaller();
        return instance;
    }
}
