/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.asserts.marshalling;

import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.util.ValidationUtils;

public class HttpMethodAssertion extends MarshallingAssertion {

    private final HttpMethodName expectedMethodName;

    public HttpMethodAssertion(HttpMethodName method) {
        this.expectedMethodName = ValidationUtils.assertNotNull(method, "method");
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        assertEquals(expectedMethodName.name(), actual.getMethod().value());
    }
}
