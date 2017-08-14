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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.interceptor.ExecutionAttributes;

public class NullResponseHandler implements HttpResponseHandler<AmazonWebServiceResponse<Object>> {

    private boolean needsConnectionLeftOpen;

    public static void assertIsUnmarshallingException(AmazonClientException e) {
        assertThat(e.getCause(), instanceOf(RuntimeException.class));
        RuntimeException re = (RuntimeException) e.getCause();
        assertThat(re.getMessage(), containsString("Unable to unmarshall response"));
    }

    @Override
    public AmazonWebServiceResponse<Object> handle(HttpResponse response,
                                                   ExecutionAttributes executionAttributes) throws Exception {
        return null;
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return needsConnectionLeftOpen;
    }

    /**
     * Enable streaming
     * @return Object for method chaining
     */
    public NullResponseHandler leaveConnectionOpen() {
        this.needsConnectionLeftOpen = true;
        return this;
    }
}
