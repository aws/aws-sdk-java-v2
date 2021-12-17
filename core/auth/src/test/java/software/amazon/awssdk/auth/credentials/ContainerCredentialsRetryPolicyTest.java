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

package software.amazon.awssdk.auth.credentials;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.internal.ContainerCredentialsRetryPolicy;
import software.amazon.awssdk.regions.util.ResourcesEndpointRetryParameters;

public class ContainerCredentialsRetryPolicyTest {

    private static ContainerCredentialsRetryPolicy retryPolicy;

    @BeforeClass
    public static void setup() {
        retryPolicy = new ContainerCredentialsRetryPolicy();
    }

    @Test
    public void shouldRetry_ReturnsTrue_For5xxStatusCode() {
        assertTrue(retryPolicy.shouldRetry(1, ResourcesEndpointRetryParameters.builder().withStatusCode(501).build()));
    }

    @Test
    public void shouldRetry_ReturnsFalse_ForNon5xxStatusCode() {
        assertFalse(retryPolicy.shouldRetry(1, ResourcesEndpointRetryParameters.builder().withStatusCode(404).build()));
        assertFalse(retryPolicy.shouldRetry(1, ResourcesEndpointRetryParameters.builder().withStatusCode(300).build()));
        assertFalse(retryPolicy.shouldRetry(1, ResourcesEndpointRetryParameters.builder().withStatusCode(202).build()));
    }

    @Test
    public void shouldRetry_ReturnsTrue_ForIoException() {
        assertTrue(retryPolicy.shouldRetry(1, ResourcesEndpointRetryParameters.builder()
                                                                              .withException(new IOException()).build()));

    }

    @Test
    public void shouldRetry_ReturnsFalse_ForNonIoException() {
        assertFalse(retryPolicy.shouldRetry(1, ResourcesEndpointRetryParameters.builder()
                                                                               .withException(new RuntimeException()).build()));
        assertFalse(retryPolicy.shouldRetry(1, ResourcesEndpointRetryParameters.builder()
                                                                               .withException(new Exception()).build()));
    }

    @Test
    public void shouldRetry_ReturnsFalse_WhenMaxRetriesExceeded() {
        assertFalse(retryPolicy.shouldRetry(5, ResourcesEndpointRetryParameters.builder().withStatusCode(501).build()));
    }
}
