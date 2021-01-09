/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.http.response;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;

public class NullResponseHandler implements HttpResponseHandler<SdkResponse> {

    public static void assertIsUnmarshallingException(SdkClientException e) {
        assertThat(e.getCause(), instanceOf(RuntimeException.class));
        RuntimeException re = (RuntimeException) e.getCause();
        assertThat(re.getMessage(), containsString("Unable to unmarshall response"));
    }

    @Override
    public SdkResponse handle(SdkHttpFullResponse response,
                                                   ExecutionAttributes executionAttributes) throws Exception {
        return null;
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return false;
    }
}
