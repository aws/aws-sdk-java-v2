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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Ignore
// FIXME: Depends on S3 properly parsing region information from the endpoint (see AmazonS3#getRegionName())
public class S3ClientCacheIntegrationTest {
    private AwsCredentials credentials;

    @Before
    public void setUp() {
        credentials = AwsCredentials.create("mock", "mock");
    }

    @Test
    public void testBadClientCache() throws Exception {
        S3ClientCache s3cc = new S3ClientCache(credentials);
        S3Client notAnAWSEndpoint = S3Client.builder()
                                            .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                            .endpointOverride(new URI("i.am.an.invalid.aws.endpoint.com"))
                                            .build();

        try {
            s3cc.useClient(notAnAWSEndpoint, Region.US_EAST_2);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No valid region has been specified. Unable to return region name"));
            return;
        }

        fail("Expected exception to be thrown");
    }

    @Test
    public void testNonExistantRegion() throws Exception {
        S3ClientCache s3cc = new S3ClientCache(credentials);
        S3Client notAnAWSEndpoint = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(new URI("s3.mordor.amazonaws.com"))
                .build();

        try {
            s3cc.useClient(notAnAWSEndpoint, Region.US_EAST_2);
        } catch (IllegalStateException e) {
            assertEquals("No valid region has been specified. Unable to return region name", e.getMessage());
            return;
        }

        fail("Expected IllegalStateException to be thrown");
    }
}