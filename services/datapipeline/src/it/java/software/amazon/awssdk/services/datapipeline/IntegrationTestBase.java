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

package software.amazon.awssdk.services.datapipeline;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Base class for all STS integration tests. Loads AWS credentials from a
 * properties file on disk, provides helper methods for tests, and instantiates
 * the STS client object for all tests to use.
 */
public class IntegrationTestBase extends AwsTestBase {

    /** The shared DP client for all tests to use. */
    protected static DataPipelineClient dataPipeline;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();

        dataPipeline = DataPipelineClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }
}
