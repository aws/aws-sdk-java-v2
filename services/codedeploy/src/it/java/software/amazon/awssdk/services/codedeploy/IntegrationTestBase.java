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

package software.amazon.awssdk.services.codedeploy;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class IntegrationTestBase extends AwsTestBase {

    /**
     * The code deploy client reference used for testing.
     */
    protected static CodeDeployClient codeDeploy;

    /**
     * Reads the credentials and sets up the code deploy the client.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        codeDeploy = CodeDeployClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }
}
