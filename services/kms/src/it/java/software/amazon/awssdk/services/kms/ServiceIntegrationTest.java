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

package software.amazon.awssdk.services.kms;

import junit.framework.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.kms.model.CreateAliasRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.DisableKeyRequest;
import software.amazon.awssdk.services.kms.model.EnableKeyRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ListKeysRequest;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static void checkValid_KeyMetadata(KeyMetadata kmd) {
        Assert.assertNotNull(kmd);

        Assert.assertNotNull(kmd.arn());
        Assert.assertNotNull(kmd.awsAccountId());
        Assert.assertNotNull(kmd.description());
        Assert.assertNotNull(kmd.keyId());
        Assert.assertNotNull(kmd.keyUsage());
        Assert.assertNotNull(kmd.creationDate());
        Assert.assertNotNull(kmd.enabled());
    }

    @Test
    public void testKeyOperations() {

        // CreateKey
        CreateKeyResponse createKeyResult = kms.createKey(CreateKeyRequest.builder()
                                                                        .description("My KMS Key")
                                                                        .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                                                                        .build());
        checkValid_KeyMetadata(createKeyResult.keyMetadata());

        final String keyId = createKeyResult.keyMetadata().keyId();

        // DescribeKey
        DescribeKeyResponse describeKeyResult = kms.describeKey(DescribeKeyRequest.builder().keyId(keyId).build());
        checkValid_KeyMetadata(describeKeyResult.keyMetadata());

        // Enable/DisableKey
        kms.enableKey(EnableKeyRequest.builder().keyId(keyId).build());
        kms.disableKey(DisableKeyRequest.builder().keyId(keyId).build());

        // ListKeys
        ListKeysResponse listKeysResult = kms.listKeys(ListKeysRequest.builder().build());
        Assert.assertFalse(listKeysResult.keys().isEmpty());

        // CreateAlias
        kms.createAlias(CreateAliasRequest.builder()
                                          .aliasName("alias/my_key" + System.currentTimeMillis())
                                          .targetKeyId(keyId)
                                          .build());

        GetKeyPolicyResponse getKeyPolicyResult = kms.getKeyPolicy(GetKeyPolicyRequest.builder()
                                                                                    .keyId(keyId)
                                                                                    .policyName("default")
                                                                                    .build());
        Assert.assertNotNull(getKeyPolicyResult.policy());

    }
}
