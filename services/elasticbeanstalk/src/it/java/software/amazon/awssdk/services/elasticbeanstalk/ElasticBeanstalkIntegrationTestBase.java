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

package software.amazon.awssdk.services.elasticbeanstalk;

import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Base class for ElasticBeanstalk integration tests; responsible for loading AWS account info for
 * running the tests, and instantiating clients for tests to use.
 */
public abstract class ElasticBeanstalkIntegrationTestBase extends AwsTestBase {

    protected static ElasticBeanstalkClient elasticbeanstalk;

    /**
     * Loads the AWS account info for the integration tests and creates an clients for tests to use.
     */
    @BeforeClass
    public static void setUp() throws IOException {
        setUpCredentials();

        elasticbeanstalk = ElasticBeanstalkClient.builder()
                                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                 .build();
    }

}
