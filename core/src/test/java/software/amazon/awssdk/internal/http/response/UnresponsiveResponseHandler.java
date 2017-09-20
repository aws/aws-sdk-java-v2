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

package software.amazon.awssdk.internal.http.response;

import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.interceptor.ExecutionAttributes;

/**
 * Error Response Handler implementation that hangs forever
 */
public class UnresponsiveResponseHandler implements HttpResponseHandler<AmazonWebServiceResponse<Object>> {

    @Override
    public AmazonWebServiceResponse<Object> handle(HttpResponse response,
                                                   ExecutionAttributes executionAttributes) throws Exception {
        Thread.sleep(Long.MAX_VALUE);
        return null;
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return false;
    }
}
