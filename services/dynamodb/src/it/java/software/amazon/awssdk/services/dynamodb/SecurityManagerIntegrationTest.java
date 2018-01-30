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

package software.amazon.awssdk.services.dynamodb;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class SecurityManagerIntegrationTest extends AwsIntegrationTestBase {

    private static final String JAVA_SECURITY_POLICY_PROPERTY = "java.security.policy";

    @AfterClass
    public static void tearDownFixture() {
        System.setSecurityManager(null);
        System.clearProperty(JAVA_SECURITY_POLICY_PROPERTY);
    }

    /**
     * Basic smoke test that the SDK works with a security manager when given appropriate
     * permissions
     */
    @Test
    public void securityManagerEnabled() {
        System.setProperty(JAVA_SECURITY_POLICY_PROPERTY, getPolicyUrl());
        SecurityManager securityManager = new SecurityManager();
        System.setSecurityManager(securityManager);
        DynamoDBClient ddb = DynamoDBClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        assertNotNull(ddb.listTables(ListTablesRequest.builder().build()));
    }

    private String getPolicyUrl() {
        return getClass().getResource("security-manager-integ-test.policy").toExternalForm();
    }
}
