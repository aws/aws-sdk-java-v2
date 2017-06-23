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

package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.simpledb.model.ListDomainsRequest;

/**
 * Tests for client authentication errors.
 *
 * @author fulghum@amazon.com
 */
public class AuthenticationErrorsIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that using an invalid access key and secret key throw an AmazonServiceException with
     * the InvalidClientTokenId error code.
     */
    @Test
    public void testInvalidClientTokenId() {
        SimpleDBClient client = SimpleDBClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(new AwsCredentials("akid", "skid")))
                .build();

        try {
            client.listDomains(ListDomainsRequest.builder().build());
            fail("Expected exception not thrown");
        } catch (AmazonServiceException e) {
            assertEquals("InvalidClientTokenId", e.getErrorCode());
            assertTrue(e.getMessage().length() > 10);
            assertTrue(e.getRequestId().length() > 10);
        }
    }

    /**
     * Tests that using a valid access key with an invalid secret key throw an
     * AmazonServiceException with the SignatureDoesNotMatch error code.
     */
    @Test
    public void testSignatureDoesNotMatch() {
        String accessKey = credentials.accessKeyId();
        SimpleDBClient client = SimpleDBClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(new AwsCredentials(accessKey, "skid")))
                .build();
        try {
            client.listDomains(ListDomainsRequest.builder().build());
            fail("Expected exception not thrown");
        } catch (AmazonServiceException e) {
            assertEquals("SignatureDoesNotMatch", e.getErrorCode());
            assertTrue(e.getMessage().length() > 10);
            assertTrue(e.getRequestId().length() > 10);
        }
    }

}
