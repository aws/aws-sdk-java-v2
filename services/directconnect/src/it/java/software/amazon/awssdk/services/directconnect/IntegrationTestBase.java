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

package software.amazon.awssdk.services.directconnect;

import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.test.AwsTestBase;

public class IntegrationTestBase extends AwsTestBase {

    protected static DirectConnectClient dc;

    @BeforeClass
    public static void setUp() throws IOException {
        setUpCredentials();
        dc = DirectConnectClient.builder()
                .region(Region.US_WEST_1)
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .build();
    }
}
