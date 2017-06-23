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

package software.amazon.awssdk.services.glacier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.glacier.model.DescribeJobRequest;
import software.amazon.awssdk.services.glacier.model.ListJobsRequest;

public class ErrorHandlingIntegrationTest extends GlacierIntegrationTestBase {
    /**
     * Tests that the Glacier client correctly parses error responses, including
     * pulling out the error code, which Glacier sends a little differently than
     * other services.
     */
    @Test
    public void testErrorHandling() throws Exception {
        initializeClient();

        try {
            glacier.describeJob(new DescribeJobRequest().withJobId("foo").withVaultName("bar"));
            fail("Expected an error, but none thrown");
        } catch (AmazonServiceException e) {
            assertNotNull(e.getErrorCode());
            assertNotNull(e.getErrorType());
            assertNotNull(e.getMessage());
            assertNotNull(e.getRequestId());
            assertNotNull(e.getServiceName());
            e.getStatusCode();
        }

        try {
            glacier.listJobs(new ListJobsRequest().withVaultName("foo"));
            fail("Expected an ResourceNotFoundException, but none thrown");
        } catch (AmazonServiceException e) {
            assertEquals("ResourceNotFoundException".toLowerCase(), e.getErrorCode().toLowerCase());
        }
    }
}
