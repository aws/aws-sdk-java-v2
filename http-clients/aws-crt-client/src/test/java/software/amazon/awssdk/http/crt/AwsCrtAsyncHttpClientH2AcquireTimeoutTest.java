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


package software.amazon.awssdk.http.crt;

import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.http.SdkAsyncHttpClientH2AcquireTimeoutTestSuite;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

public class AwsCrtAsyncHttpClientH2AcquireTimeoutTest extends SdkAsyncHttpClientH2AcquireTimeoutTestSuite {

    @Override
    protected SdkAsyncHttpClient createSdkAsyncHttpClient(AttributeMap config) {
        return AwsCrtAsyncHttpClient.builder().buildWithDefaults(config);
    }

    @Override
    protected String expectedAcquireTimeoutMessagePrefix() {
        return "Acquire operation took longer than the configured maximum time.";
    }

    @Override
    protected Class<? extends Throwable> expectedAcquireTimeoutCause() {
        return CrtRuntimeException.class;
    }
}
