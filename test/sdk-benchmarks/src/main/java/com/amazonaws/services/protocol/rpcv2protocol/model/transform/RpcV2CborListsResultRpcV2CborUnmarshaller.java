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
 * RpcV2CborListsResult CBOR Unmarshaller
 */

public class RpcV2CborListsResultRpcV2CborUnmarshaller implements Unmarshaller<RpcV2CborListsResult, RpcV2CborUnmarshallerContext> {

    public RpcV2CborListsResult unmarshall(RpcV2CborUnmarshallerContext context) throws Exception {
        RpcV2CborListsResult rpcV2CborListsResult = new RpcV2CborListsResult();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return rpcV2CborListsResult;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("stringList", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setStringList(new ListUnmarshaller<String>(context.getUnmarshaller(String.class))

                    .unmarshall(context));
                }
                if (context.testExpression("stringSet", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setStringSet(new ListUnmarshaller<String>(context.getUnmarshaller(String.class))

                    .unmarshall(context));
                }
                if (context.testExpression("integerList", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setIntegerList(new ListUnmarshaller<Integer>(context.getUnmarshaller(Integer.class))

                    .unmarshall(context));
                }
                if (context.testExpression("booleanList", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setBooleanList(new ListUnmarshaller<Boolean>(context.getUnmarshaller(Boolean.class))

                    .unmarshall(context));
                }
                if (context.testExpression("timestampList", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setTimestampList(new ListUnmarshaller<java.util.Date>(DateCborUnmarshaller.getInstance())

                    .unmarshall(context));
                }
                if (context.testExpression("enumList", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setEnumList(new ListUnmarshaller<String>(context.getUnmarshaller(String.class))

                    .unmarshall(context));
                }
                if (context.testExpression("intEnumList", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setIntEnumList(new ListUnmarshaller<Integer>(context.getUnmarshaller(Integer.class))

                    .unmarshall(context));
                }
                if (context.testExpression("nestedStringList", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setNestedStringList(new ListUnmarshaller<java.util.List<String>>(new ListUnmarshaller<String>(context
                            .getUnmarshaller(String.class))

                    )

                    .unmarshall(context));
                }
                if (context.testExpression("structureList", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setStructureList(new ListUnmarshaller<StructureListMember>(StructureListMemberRpcV2CborUnmarshaller.getInstance())

                    .unmarshall(context));
                }
                if (context.testExpression("blobList", targetDepth)) {
                    context.nextToken();
                    rpcV2CborListsResult.setBlobList(new ListUnmarshaller<ByteBuffer>(context.getUnmarshaller(ByteBuffer.class))

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

        return rpcV2CborListsResult;
    }

    private static RpcV2CborListsResultRpcV2CborUnmarshaller instance;

    public static RpcV2CborListsResultRpcV2CborUnmarshaller getInstance() {
        if (instance == null)
            instance = new RpcV2CborListsResultRpcV2CborUnmarshaller();
        return instance;
    }
}
