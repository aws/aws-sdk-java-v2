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

package software.amazon.awssdk.retry.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import utils.model.EmptyAmazonWebServiceRequest;

public class RetryPolicyContextTest {

    @Test
    public void totalRequests_IsOneMoreThanRetriesAttempted() {
        assertEquals(4, RetryPolicyContexts.withRetriesAttempted(3).totalRequests());
    }

    @Test
    public void nullHttpStatusCodeAllowed() {
        assertNull(RetryPolicyContexts.withStatusCode(null).httpStatusCode());
    }

    @Test
    public void nullExceptionAllowed() {
        assertNull(RetryPolicyContexts.withException(null).exception());
    }

    @Test
    public void buildFully() {
        final EmptyAmazonWebServiceRequest origRequest = new EmptyAmazonWebServiceRequest();
        final SdkHttpFullRequest request = SdkHttpFullRequest.builder().build();
        final SdkClientException exception = new SdkClientException("boom");
        final RetryPolicyContext context = RetryPolicyContext.builder()
                                                             .retriesAttempted(3)
                                                             .httpStatusCode(400)
                                                             .request(request)
                                                             .exception(exception)
                                                             .originalRequest(origRequest)
                                                             .build();


        assertEquals(3, context.retriesAttempted());
        assertEquals(Integer.valueOf(400), context.httpStatusCode());
        assertEquals(request, context.request());
        assertEquals(exception, context.exception());
        assertEquals(origRequest, context.originalRequest());
    }

}
