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
import java.nio.ByteBuffer;


import com.amazonaws.services.protocol.restjson.model.*;
import com.amazonaws.transform.SimpleTypeJsonUnmarshallers.*;
import com.amazonaws.transform.*;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * StructWithNestedBlobType JSON Unmarshaller
 */

public class StructWithNestedBlobTypeJsonUnmarshaller implements Unmarshaller<StructWithNestedBlobType, JsonUnmarshallerContext> {

    public StructWithNestedBlobType unmarshall(JsonUnmarshallerContext context) throws Exception {
        StructWithNestedBlobType structWithNestedBlobType = new StructWithNestedBlobType();

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
                if (context.testExpression("NestedBlob", targetDepth)) {
                    context.nextToken();
                    structWithNestedBlobType.setNestedBlob(context.getUnmarshaller(ByteBuffer.class).unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return structWithNestedBlobType;
    }

    private static StructWithNestedBlobTypeJsonUnmarshaller instance;

    public static StructWithNestedBlobTypeJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new StructWithNestedBlobTypeJsonUnmarshaller();
        return instance;
    }
}
