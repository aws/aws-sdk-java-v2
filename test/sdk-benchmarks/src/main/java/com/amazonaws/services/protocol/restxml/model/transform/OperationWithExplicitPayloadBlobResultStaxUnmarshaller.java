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
package com.amazonaws.services.protocol.restxml.model.transform;



import com.amazonaws.services.protocol.restxml.model.*;
import com.amazonaws.transform.Unmarshaller;

import com.amazonaws.transform.StaxUnmarshallerContext;
import com.amazonaws.transform.SimpleTypeStaxUnmarshallers.*;

/**
 * OperationWithExplicitPayloadBlobResult StAX Unmarshaller
 */


public class OperationWithExplicitPayloadBlobResultStaxUnmarshaller implements Unmarshaller<OperationWithExplicitPayloadBlobResult, StaxUnmarshallerContext> {

    public OperationWithExplicitPayloadBlobResult unmarshall(StaxUnmarshallerContext context) throws Exception {
        OperationWithExplicitPayloadBlobResult operationWithExplicitPayloadBlobResult = new OperationWithExplicitPayloadBlobResult();
        int originalDepth = context.getCurrentDepth();
        int targetDepth = originalDepth + 1;

        java.io.InputStream is = context.getHttpResponse().getContent();
        if (is != null) {
            try {
                operationWithExplicitPayloadBlobResult.setPayloadMember(java.nio.ByteBuffer.wrap(com.amazonaws.util.IOUtils.toByteArray(is)));
            } finally {
                com.amazonaws.util.IOUtils.closeQuietly(is, null);
            }
        }
        return operationWithExplicitPayloadBlobResult;
    }

    private static OperationWithExplicitPayloadBlobResultStaxUnmarshaller instance;

    public static OperationWithExplicitPayloadBlobResultStaxUnmarshaller getInstance() {
        if (instance == null)
            instance = new OperationWithExplicitPayloadBlobResultStaxUnmarshaller();
        return instance;
    }
}
