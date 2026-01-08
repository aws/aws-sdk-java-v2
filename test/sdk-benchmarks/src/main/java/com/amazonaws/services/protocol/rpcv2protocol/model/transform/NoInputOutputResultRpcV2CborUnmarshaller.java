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

import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * NoInputOutputResult CBOR Unmarshaller
 */

public class NoInputOutputResultRpcV2CborUnmarshaller implements Unmarshaller<NoInputOutputResult, RpcV2CborUnmarshallerContext> {

    public NoInputOutputResult unmarshall(RpcV2CborUnmarshallerContext context) throws Exception {
        NoInputOutputResult noInputOutputResult = new NoInputOutputResult();

        return noInputOutputResult;
    }

    private static NoInputOutputResultRpcV2CborUnmarshaller instance;

    public static NoInputOutputResultRpcV2CborUnmarshaller getInstance() {
        if (instance == null)
            instance = new NoInputOutputResultRpcV2CborUnmarshaller();
        return instance;
    }
}
