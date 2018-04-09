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

package software.amazon.awssdk.services.route53;

import java.io.IOException;
import org.junit.Before;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Base class for Route53 integration tests.
 */
public abstract class IntegrationTestBase extends AwsTestBase {

    /** Shared client for all tests to use. */
    protected static Route53Client route53;


    /**
     * Loads the AWS account info for the integration tests and creates a client
     * for tests to use.
     */
    @Before
    public void setUp() throws IOException {
        setUpCredentials();
        route53 = Route53Client.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(Region.AWS_GLOBAL)
                .build();
    }

}
