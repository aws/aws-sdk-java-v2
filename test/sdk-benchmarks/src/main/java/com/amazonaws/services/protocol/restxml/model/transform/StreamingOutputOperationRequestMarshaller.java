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



import com.amazonaws.SdkClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.restxml.model.*;
import com.amazonaws.transform.Marshaller;

/**
 * StreamingOutputOperationRequest Marshaller
 */


public class StreamingOutputOperationRequestMarshaller implements Marshaller<Request<StreamingOutputOperationRequest>, StreamingOutputOperationRequest> {

    public Request<StreamingOutputOperationRequest> marshall(StreamingOutputOperationRequest streamingOutputOperationRequest) {

        if (streamingOutputOperationRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<StreamingOutputOperationRequest> request = new DefaultRequest<StreamingOutputOperationRequest>(streamingOutputOperationRequest,
                "AmazonProtocolRestXml");

        request.setHttpMethod(HttpMethodName.POST);

        String uriResourcePath = "/2016-03-11/streamingOutputOperation";

        request.setResourcePath(uriResourcePath);

        return request;
    }

}
