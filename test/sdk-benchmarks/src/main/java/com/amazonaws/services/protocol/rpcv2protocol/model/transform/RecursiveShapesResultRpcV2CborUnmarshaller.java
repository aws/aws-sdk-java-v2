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
package com.amazonaws.services.protocol.rpcv2protocol.model.transform;

import java.math.*;



import com.amazonaws.services.protocol.rpcv2protocol.model.*;
import com.amazonaws.transform.rpcv2cbor.SimpleTypeRpcV2CborUnmarshallers.*;
import com.amazonaws.transform.rpcv2cbor.*;
import com.amazonaws.transform.Unmarshaller;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * RecursiveShapesResult CBOR Unmarshaller
 */

public class RecursiveShapesResultRpcV2CborUnmarshaller implements Unmarshaller<RecursiveShapesResult, RpcV2CborUnmarshallerContext> {

    public RecursiveShapesResult unmarshall(RpcV2CborUnmarshallerContext context) throws Exception {
        RecursiveShapesResult recursiveShapesResult = new RecursiveShapesResult();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return recursiveShapesResult;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("nested", targetDepth)) {
                    context.nextToken();
                    recursiveShapesResult.setNested(RecursiveShapesInputOutputNested1RpcV2CborUnmarshaller.getInstance().unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return recursiveShapesResult;
    }

    private static RecursiveShapesResultRpcV2CborUnmarshaller instance;

    public static RecursiveShapesResultRpcV2CborUnmarshaller getInstance() {
        if (instance == null)
            instance = new RecursiveShapesResultRpcV2CborUnmarshaller();
        return instance;
    }
}
