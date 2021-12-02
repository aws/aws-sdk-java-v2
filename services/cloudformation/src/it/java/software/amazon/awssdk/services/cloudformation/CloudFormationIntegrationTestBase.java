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

package software.amazon.awssdk.services.cloudformation;

import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Base class for CloudFormation integration tests. Loads AWS credentials from a properties file and
 * creates a client for callers to use.
 */
public class CloudFormationIntegrationTestBase extends AwsTestBase {

    protected static CloudFormationClient cf;

    /**
     * Loads the AWS account info for the integration tests and creates an S3 client for tests to
     * use.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        cf = CloudFormationClient.builder()
                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                 .region(Region.AP_NORTHEAST_1)
                                 .build();
    }
}
