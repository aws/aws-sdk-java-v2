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
 * ComplexNestedErrorData CBOR Unmarshaller
 */

public class ComplexNestedErrorDataRpcV2CborUnmarshaller implements Unmarshaller<ComplexNestedErrorData, RpcV2CborUnmarshallerContext> {

    public ComplexNestedErrorData unmarshall(RpcV2CborUnmarshallerContext context) throws Exception {
        ComplexNestedErrorData complexNestedErrorData = new ComplexNestedErrorData();

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
                if (context.testExpression("Foo", targetDepth)) {
                    context.nextToken();
                    complexNestedErrorData.setFoo(context.getUnmarshaller(String.class).unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return complexNestedErrorData;
    }

    private static ComplexNestedErrorDataRpcV2CborUnmarshaller instance;

    public static ComplexNestedErrorDataRpcV2CborUnmarshaller getInstance() {
        if (instance == null)
            instance = new ComplexNestedErrorDataRpcV2CborUnmarshaller();
        return instance;
    }
}
