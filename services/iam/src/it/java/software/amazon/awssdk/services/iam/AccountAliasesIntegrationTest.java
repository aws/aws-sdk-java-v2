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

package software.amazon.awssdk.services.iam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.iam.model.CreateAccountAliasRequest;
import software.amazon.awssdk.services.iam.model.DeleteAccountAliasRequest;
import software.amazon.awssdk.services.iam.model.ListAccountAliasesRequest;

public class AccountAliasesIntegrationTest extends IntegrationTestBase {

    private static final String ACCOUNT_ALIAS = "java-sdk-alias-" + System.currentTimeMillis();


    /** Tests that we can create, list and delete account aliases. */
    @Test
    public void testAccountAliases() throws Exception {
        iam.createAccountAlias(CreateAccountAliasRequest.builder().accountAlias(ACCOUNT_ALIAS).build());

        List<String> accountAliases = iam.listAccountAliases(ListAccountAliasesRequest.builder().build()).accountAliases();
        assertNotNull(accountAliases);
        assertEquals(1, accountAliases.size());
        assertEquals(ACCOUNT_ALIAS, accountAliases.get(0));

        iam.deleteAccountAlias(DeleteAccountAliasRequest.builder().accountAlias(ACCOUNT_ALIAS).build());
        assertTrue(iam.listAccountAliases(ListAccountAliasesRequest.builder().build()).accountAliases().isEmpty());
    }
}
