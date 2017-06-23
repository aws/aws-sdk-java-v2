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

package software.amazon.awssdk.services.directory;

import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.EC2Client;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class IntegrationTestBase extends AwsIntegrationTestBase {

    protected static DirectoryClient dsClient;
    protected static EC2Client ec2Client;

    @BeforeClass
    public static void baseSetupFixture() {
        dsClient = DirectoryClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(Region.US_EAST_1)
                .build();
        ec2Client = EC2Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .build();
    }
}
