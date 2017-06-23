/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.handlers;

import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Interface that allows hooking into the lifecycle of a request and in certain cases, modifying
 * the outbound request or inbound response.
 * TODO: This shouldn't be coupled to AWS-specific concepts
 */
public abstract class RequestHandler implements IRequestHandler {

    @Override
    public AmazonWebServiceRequest beforeMarshalling(AmazonWebServiceRequest request) {
        return request;
    }

    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {
        return request;
    }

    @Override
    public HttpResponse beforeUnmarshalling(SdkHttpFullRequest request, HttpResponse httpResponse) {
        return httpResponse;
    }

    @Override
    public void afterResponse(SdkHttpFullRequest request, Response<?> response) {
    }

    @Override
    public void afterError(SdkHttpFullRequest request, Response<?> response, Exception e) {
    }
}
