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
 * GetOperationWithBodyResult JSON Unmarshaller
 */

public class GetOperationWithBodyResultJsonUnmarshaller implements Unmarshaller<GetOperationWithBodyResult, JsonUnmarshallerContext> {

    public GetOperationWithBodyResult unmarshall(JsonUnmarshallerContext context) throws Exception {
        GetOperationWithBodyResult getOperationWithBodyResult = new GetOperationWithBodyResult();

        return getOperationWithBodyResult;
    }

    private static GetOperationWithBodyResultJsonUnmarshaller instance;

    public static GetOperationWithBodyResultJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new GetOperationWithBodyResultJsonUnmarshaller();
        return instance;
    }
}
