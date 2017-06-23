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

package software.amazon.awssdk.services.glacier;

import org.junit.AfterClass;
import software.amazon.awssdk.services.glacier.model.DeleteVaultRequest;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Integration tests for AWS Glacier.
 */
public class GlacierIntegrationTestBase extends AwsTestBase {

    /** Size of data to upload to Glacier. */
    static final long CONTENT_LENGTH = 1024 * 1024 * 5 + 123;

    protected static GlacierClient glacier;

    protected static String accountId = "599169622985";
    protected static String vaultName = "java-sdk-1332366353936";
    //    protected static String accountId = "-";
    //    protected static String vaultName = "java-sdk-140703";

    /** Release any resources created by the tests. */
    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (vaultName != null) {
                glacier.deleteVault(new DeleteVaultRequest().withAccountId(accountId).withVaultName(vaultName));
            }
        } catch (Exception e) {
            // Ignored or expected.
        }
    }

    protected void initializeClient() throws Exception {
        setUpCredentials();
        glacier = GlacierClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }
}

