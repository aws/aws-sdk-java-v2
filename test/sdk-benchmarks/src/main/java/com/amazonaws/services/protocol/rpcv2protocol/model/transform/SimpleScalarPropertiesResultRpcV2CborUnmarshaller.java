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
import java.nio.ByteBuffer;


import com.amazonaws.services.protocol.rpcv2protocol.model.*;
import com.amazonaws.transform.rpcv2cbor.SimpleTypeRpcV2CborUnmarshallers.*;
import com.amazonaws.transform.rpcv2cbor.*;
import com.amazonaws.transform.Unmarshaller;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * SimpleScalarPropertiesResult CBOR Unmarshaller
 */

public class SimpleScalarPropertiesResultRpcV2CborUnmarshaller implements Unmarshaller<SimpleScalarPropertiesResult, RpcV2CborUnmarshallerContext> {

    public SimpleScalarPropertiesResult unmarshall(RpcV2CborUnmarshallerContext context) throws Exception {
        SimpleScalarPropertiesResult simpleScalarPropertiesResult = new SimpleScalarPropertiesResult();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return simpleScalarPropertiesResult;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("trueBooleanValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setTrueBooleanValue(context.getUnmarshaller(Boolean.class).unmarshall(context));
                }
                if (context.testExpression("falseBooleanValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setFalseBooleanValue(context.getUnmarshaller(Boolean.class).unmarshall(context));
                }
                if (context.testExpression("byteValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setByteValue(context.getUnmarshaller(Integer.class).unmarshall(context));
                }
                if (context.testExpression("doubleValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setDoubleValue(context.getUnmarshaller(Double.class).unmarshall(context));
                }
                if (context.testExpression("floatValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setFloatValue(context.getUnmarshaller(Float.class).unmarshall(context));
                }
                if (context.testExpression("integerValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setIntegerValue(context.getUnmarshaller(Integer.class).unmarshall(context));
                }
                if (context.testExpression("longValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setLongValue(context.getUnmarshaller(Long.class).unmarshall(context));
                }
                if (context.testExpression("shortValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setShortValue(context.getUnmarshaller(Integer.class).unmarshall(context));
                }
                if (context.testExpression("stringValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setStringValue(context.getUnmarshaller(String.class).unmarshall(context));
                }
                if (context.testExpression("blobValue", targetDepth)) {
                    context.nextToken();
                    simpleScalarPropertiesResult.setBlobValue(context.getUnmarshaller(ByteBuffer.class).unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return simpleScalarPropertiesResult;
    }

    private static SimpleScalarPropertiesResultRpcV2CborUnmarshaller instance;

    public static SimpleScalarPropertiesResultRpcV2CborUnmarshaller getInstance() {
        if (instance == null)
            instance = new SimpleScalarPropertiesResultRpcV2CborUnmarshaller();
        return instance;
    }
}
