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

package software.amazon.awssdk.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.AmazonClientException;
import software.amazon.awssdk.core.util.LogCaptor;

/**
 * Unit tests for the InstanceProfileCredentialsProvider.
 */
public class InstanceProfileCredentialsProviderIntegrationTest extends LogCaptor.LogCaptorTestBase {

    private EC2MetadataServiceMock mockServer;

    /** Starts up the mock EC2 Instance Metadata Service. */
    @Before
    public void setUp() throws Exception {
        mockServer = new EC2MetadataServiceMock();
        mockServer.start();
    }

    /** Shuts down the mock EC2 Instance Metadata Service. */
    @After
    public void tearDown() throws Exception {
        mockServer.stop();
        Thread.sleep(1000);
    }

    /** Tests that we correctly handle the metadata service returning credentials. */
    @Test
    public void testSessionCredentials() throws Exception {
        mockServer.setResponseFileName("sessionResponse");
        mockServer.setAvailableSecurityCredentials("aws-dr-tools-test");

        InstanceProfileCredentialsProvider credentialsProvider = InstanceProfileCredentialsProvider.create();
        AwsSessionCredentials credentials = (AwsSessionCredentials) credentialsProvider.getCredentials();

        assertEquals("ACCESS_KEY_ID", credentials.accessKeyId());
        assertEquals("SECRET_ACCESS_KEY", credentials.secretAccessKey());
        assertEquals("TOKEN_TOKEN_TOKEN", credentials.sessionToken());
    }

    /**
     * Tests that we correctly handle the metadata service returning credentials
     * when multiple instance profiles are available.
     */
    @Test
    public void testSessionCredentials_MultipleInstanceProfiles() throws Exception {
        mockServer.setResponseFileName("sessionResponse");
        mockServer.setAvailableSecurityCredentials("test-credentials");

        InstanceProfileCredentialsProvider credentialsProvider = InstanceProfileCredentialsProvider.create();
        AwsSessionCredentials credentials = (AwsSessionCredentials) credentialsProvider.getCredentials();

        assertEquals("ACCESS_KEY_ID", credentials.accessKeyId());
        assertEquals("SECRET_ACCESS_KEY", credentials.secretAccessKey());
        assertEquals("TOKEN_TOKEN_TOKEN", credentials.sessionToken());
    }

    /**
     * Tests that we correctly handle when no instance profiles are available
     * through the metadata service.
     */
    @Test
    public void testNoInstanceProfiles() throws Exception {
        mockServer.setResponseFileName("sessionResponse");
        mockServer.setAvailableSecurityCredentials("");

        InstanceProfileCredentialsProvider credentialsProvider = InstanceProfileCredentialsProvider.create();

        try {
            credentialsProvider.getCredentials();
            fail("Expected an AmazonClientException, but wasn't thrown");
        } catch (AmazonClientException ace) {
            assertNotNull(ace.getMessage());
        }
    }
}
