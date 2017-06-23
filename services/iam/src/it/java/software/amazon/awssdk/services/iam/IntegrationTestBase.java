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

package software.amazon.awssdk.services.iam;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Before;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Base class for IAM integration tests. Provides convenience methods for
 * creating test data, and automatically loads AWS credentials from a properties
 * file on disk and instantiates clients for the individual tests to use.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class IntegrationTestBase extends AwsTestBase {

    /** The IAM client for all tests to use. */
    protected IAMClient iam;

    /**
     * Loads the AWS account info for the integration tests and creates an
     * IAM client for tests to use.
     */
    @Before
    public void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        iam = IAMClient.builder()
                       .credentialsProvider(new StaticCredentialsProvider(credentials))
                       .region(Region.AWS_GLOBAL)
                       .build();
        System.out.println(iam);
    }

}
