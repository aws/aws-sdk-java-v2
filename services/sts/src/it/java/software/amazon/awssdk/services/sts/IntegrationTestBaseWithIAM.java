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

package software.amazon.awssdk.services.sts;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IAMClient;

/**
 * Base class for all STS integration tests that also need IAM
 */
public abstract class IntegrationTestBaseWithIAM extends IntegrationTestBase {

    /** The shared IAM client for all tests to use. */
    protected static IAMClient iam;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        IntegrationTestBase.setUp();
        iam = IAMClient.builder()
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .region(Region.AWS_GLOBAL)
                       .build();
    }
}
