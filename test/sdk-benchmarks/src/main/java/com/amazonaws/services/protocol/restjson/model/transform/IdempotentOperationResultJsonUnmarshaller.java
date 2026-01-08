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

import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * IdempotentOperationResult JSON Unmarshaller
 */

public class IdempotentOperationResultJsonUnmarshaller implements Unmarshaller<IdempotentOperationResult, JsonUnmarshallerContext> {

    public IdempotentOperationResult unmarshall(JsonUnmarshallerContext context) throws Exception {
        IdempotentOperationResult idempotentOperationResult = new IdempotentOperationResult();

        return idempotentOperationResult;
    }

    private static IdempotentOperationResultJsonUnmarshaller instance;

    public static IdempotentOperationResultJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new IdempotentOperationResultJsonUnmarshaller();
        return instance;
    }
}
