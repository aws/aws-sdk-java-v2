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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.services.iam.model.DeleteAccountPasswordPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetAccountPasswordPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetAccountPasswordPolicyResponse;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.UpdateAccountPasswordPolicyRequest;

public class PasswordPoliciesIntegrationTest extends IntegrationTestBase {

    /** Tests that we can create, list and delete account aliases. */
    @Test
    public void testAccountAliases() throws Exception {
        int minimumPasswordLength = 8;
        iam.updateAccountPasswordPolicy(UpdateAccountPasswordPolicyRequest.builder()
                                                                          .minimumPasswordLength(minimumPasswordLength)
                                                                          .requireLowercaseCharacters(true)
                                                                          .requireNumbers(true)
                                                                          .requireSymbols(true)
                                                                          .requireUppercaseCharacters(true).build());

        GetAccountPasswordPolicyResponse accountPasswordPolicy =
                iam.getAccountPasswordPolicy(GetAccountPasswordPolicyRequest.builder().build());
        assertEquals(minimumPasswordLength, accountPasswordPolicy.passwordPolicy().minimumPasswordLength().intValue());
        assertTrue(accountPasswordPolicy.passwordPolicy().requireLowercaseCharacters());
        assertTrue(accountPasswordPolicy.passwordPolicy().requireNumbers());
        assertTrue(accountPasswordPolicy.passwordPolicy().requireSymbols());
        assertTrue(accountPasswordPolicy.passwordPolicy().requireUppercaseCharacters());

        minimumPasswordLength = 6;
        iam.updateAccountPasswordPolicy(UpdateAccountPasswordPolicyRequest.builder()
                                                                          .minimumPasswordLength(minimumPasswordLength)
                                                                          .requireLowercaseCharacters(false)
                                                                          .requireNumbers(false)
                                                                          .requireSymbols(false)
                                                                          .requireUppercaseCharacters(false).build());

        accountPasswordPolicy = iam.getAccountPasswordPolicy(GetAccountPasswordPolicyRequest.builder().build());
        assertEquals(minimumPasswordLength, accountPasswordPolicy.passwordPolicy().minimumPasswordLength().intValue());
        assertFalse(accountPasswordPolicy.passwordPolicy().requireLowercaseCharacters());
        assertFalse(accountPasswordPolicy.passwordPolicy().requireNumbers());
        assertFalse(accountPasswordPolicy.passwordPolicy().requireSymbols());
        assertFalse(accountPasswordPolicy.passwordPolicy().requireUppercaseCharacters());

        iam.deleteAccountPasswordPolicy(DeleteAccountPasswordPolicyRequest.builder().build());
        try {
            iam.getAccountPasswordPolicy(GetAccountPasswordPolicyRequest.builder().build()).passwordPolicy();
            fail("Should have thrown an exception for a missing policy");
        } catch (NoSuchEntityException e) {
            // Ignored or expected.
        }
    }
}
