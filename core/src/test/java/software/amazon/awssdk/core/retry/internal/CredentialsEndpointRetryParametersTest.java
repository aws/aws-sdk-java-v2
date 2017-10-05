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

package software.amazon.awssdk.core.retry.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

/**
 * Unit tests for {@link CredentialsEndpointRetryParameters}.
 */
public class CredentialsEndpointRetryParametersTest {

    @Test
    public void defaultParams() {
        CredentialsEndpointRetryParameters params = CredentialsEndpointRetryParameters.builder().build();
        assertNull(params.getStatusCode());
        assertNull(params.getException());
    }

    @Test
    public void defaultParams_withStatusCode() {
        Integer statusCode = new Integer(400);
        CredentialsEndpointRetryParameters params = CredentialsEndpointRetryParameters.builder()
                                                                                      .withStatusCode(statusCode)
                                                                                      .build();
        assertEquals(statusCode, params.getStatusCode());
        assertNull(params.getException());
    }

    @Test
    public void defaultParams_withStatusCodeAndException() {
        Integer statusCode = new Integer(500);
        CredentialsEndpointRetryParameters params = CredentialsEndpointRetryParameters.builder()
                                                                                      .withStatusCode(statusCode)
                                                                                      .withException(new IOException())
                                                                                      .build();
        assertEquals(statusCode, params.getStatusCode());
        assertTrue(params.getException() instanceof IOException);
    }

}
