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

package software.amazon.awssdk.services.dynamodb;

import java.util.UUID;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetFederationTokenRequest;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Simple smoke test to make sure the new JSON error unmarshaller works as expected.
 */
public class DynamoDbJavaClientExceptionIntegrationTest extends AwsTestBase {

    private static DynamoDbClient ddb;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        ddb = DynamoDbClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @Test
    public void testResourceNotFoundException() {
        try {
            ddb.describeTable(DescribeTableRequest.builder().tableName(UUID.randomUUID().toString()).build());
            Assert.fail("ResourceNotFoundException is expected.");
        } catch (ResourceNotFoundException e) {
            Assert.assertNotNull(e.awsErrorDetails().errorCode());
            Assert.assertNotNull(e.awsErrorDetails().errorMessage());
            Assert.assertNotNull(e.awsErrorDetails().rawResponse());
        }
    }

    @Test
    public void testPermissionError() {
        StsClient sts = StsClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(Region.US_EAST_1)
                .build();

        Credentials creds = sts.getFederationToken(GetFederationTokenRequest.builder()
                .name("NoAccess")
                .policy(
                        "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Deny\",\"Action\":\"*\",\"Resource\":\"*\"}]}")
                .durationSeconds(900).build()).credentials();


        DynamoDbClient client = DynamoDbClient.builder().credentialsProvider(
                StaticCredentialsProvider.create(AwsSessionCredentials.create(
                creds.accessKeyId(),
                creds.secretAccessKey(),
                creds.sessionToken()))).build();

        try {
            client.listTables(ListTablesRequest.builder().build());
        } catch (AwsServiceException e) {
            Assert.assertEquals("AccessDeniedException", e.awsErrorDetails().errorCode());
            Assert.assertNotNull(e.awsErrorDetails().errorMessage());
            Assert.assertNotNull(e.getMessage());
        }
    }
}
