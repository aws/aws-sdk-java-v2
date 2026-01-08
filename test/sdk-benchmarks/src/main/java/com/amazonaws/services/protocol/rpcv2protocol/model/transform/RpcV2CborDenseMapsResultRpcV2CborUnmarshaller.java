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
 * RpcV2CborDenseMapsResult CBOR Unmarshaller
 */

public class RpcV2CborDenseMapsResultRpcV2CborUnmarshaller implements Unmarshaller<RpcV2CborDenseMapsResult, RpcV2CborUnmarshallerContext> {

    public RpcV2CborDenseMapsResult unmarshall(RpcV2CborUnmarshallerContext context) throws Exception {
        RpcV2CborDenseMapsResult rpcV2CborDenseMapsResult = new RpcV2CborDenseMapsResult();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return rpcV2CborDenseMapsResult;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("denseStructMap", targetDepth)) {
                    context.nextToken();
                    rpcV2CborDenseMapsResult.setDenseStructMap(new MapUnmarshaller<String, GreetingStruct>(context.getUnmarshaller(String.class),
                            GreetingStructRpcV2CborUnmarshaller.getInstance()).unmarshall(context));
                }
                if (context.testExpression("denseNumberMap", targetDepth)) {
                    context.nextToken();
                    rpcV2CborDenseMapsResult.setDenseNumberMap(new MapUnmarshaller<String, Integer>(context.getUnmarshaller(String.class), context
                            .getUnmarshaller(Integer.class)).unmarshall(context));
                }
                if (context.testExpression("denseBooleanMap", targetDepth)) {
                    context.nextToken();
                    rpcV2CborDenseMapsResult.setDenseBooleanMap(new MapUnmarshaller<String, Boolean>(context.getUnmarshaller(String.class), context
                            .getUnmarshaller(Boolean.class)).unmarshall(context));
                }
                if (context.testExpression("denseStringMap", targetDepth)) {
                    context.nextToken();
                    rpcV2CborDenseMapsResult.setDenseStringMap(new MapUnmarshaller<String, String>(context.getUnmarshaller(String.class), context
                            .getUnmarshaller(String.class)).unmarshall(context));
                }
                if (context.testExpression("denseSetMap", targetDepth)) {
                    context.nextToken();
                    rpcV2CborDenseMapsResult.setDenseSetMap(new MapUnmarshaller<String, java.util.List<String>>(context.getUnmarshaller(String.class),
                            new ListUnmarshaller<String>(context.getUnmarshaller(String.class))

                    ).unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return rpcV2CborDenseMapsResult;
    }

    private static RpcV2CborDenseMapsResultRpcV2CborUnmarshaller instance;

    public static RpcV2CborDenseMapsResultRpcV2CborUnmarshaller getInstance() {
        if (instance == null)
            instance = new RpcV2CborDenseMapsResultRpcV2CborUnmarshaller();
        return instance;
    }
}
