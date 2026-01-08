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
package com.amazonaws.services.protocol.ec2.model.transform;



import com.amazonaws.SdkClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.ec2.model.*;
import com.amazonaws.transform.Marshaller;

import com.amazonaws.util.IdempotentUtils;

/**
 * IdempotentOperationRequest Marshaller
 */


public class IdempotentOperationRequestMarshaller implements Marshaller<Request<IdempotentOperationRequest>, IdempotentOperationRequest> {

    public Request<IdempotentOperationRequest> marshall(IdempotentOperationRequest idempotentOperationRequest) {

        if (idempotentOperationRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<IdempotentOperationRequest> request = new DefaultRequest<IdempotentOperationRequest>(idempotentOperationRequest, "AmazonProtocolEc2");
        request.addParameter("Action", "IdempotentOperation");
        request.addParameter("Version", "2016-03-11");
        request.setHttpMethod(HttpMethodName.POST);

        request.addParameter("IdempotencyToken", IdempotentUtils.resolveString(idempotentOperationRequest.getIdempotencyToken()));

        return request;
    }

}
