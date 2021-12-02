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

package software.amazon.awssdk.services.cloudhsm;

import org.junit.BeforeClass;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class IntegrationTestBase extends AwsTestBase {

    protected static CloudHsmClient client;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        client = CloudHsmClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }
}
