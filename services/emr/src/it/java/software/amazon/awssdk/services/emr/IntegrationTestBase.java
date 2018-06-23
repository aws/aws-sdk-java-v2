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

package software.amazon.awssdk.services.emr;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Base class for EMR integration tests. Provides convenience methods for
 * creating test data, and automatically loads AWS credentials from a properties
 * file on disk and instantiates clients for the individual tests to use.
 */
public class IntegrationTestBase extends AwsTestBase {

    /** The EMR client for all tests to use. */
    protected static EmrClient emr;

    /**
     * Loads the AWS account info for the integration tests and creates an
     * EMR client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        emr = EmrClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }
}
