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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.services.iam.model.AccessKeyMetadata;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyResponse;
import software.amazon.awssdk.services.iam.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.LimitExceededException;
import software.amazon.awssdk.services.iam.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysResponse;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;

/**
 * Integration tests for access key methods in IAM.
 */
public class AccessKeyIntegrationTest extends IntegrationTestBase {

    private static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

    @Before
    public void testSetup() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void testCreateAccessKey() {
        String username = IAMUtil.createTestUser();
        String keyId = null;
        try {
            CreateAccessKeyResponse response = iam
                    .createAccessKey(CreateAccessKeyRequest.builder()
                                                           .userName(username).build());
            keyId = response.accessKey().accessKeyId();
            assertEquals(System.currentTimeMillis() / MILLISECONDS_IN_DAY,
                         response.accessKey().createDate().getTime()
                         / MILLISECONDS_IN_DAY);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (keyId != null) {
                iam.deleteAccessKey(DeleteAccessKeyRequest.builder().userName(
                        username).accessKeyId(keyId).build());
            }

            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void testCreateAccessKeyNonExistentUserException() {
        String username = IAMUtil.uniqueName();
        iam.createAccessKey(CreateAccessKeyRequest.builder().userName(username).build());
    }

    @Test
    public void testListAccessKeys() {
        String username = IAMUtil.createTestUser();
        String[] keyIds = new String[2];
        try {
            for (int i = 0; i < 2; i++) {
                CreateAccessKeyResponse response = iam
                        .createAccessKey(CreateAccessKeyRequest.builder()
                                                               .userName(username).build());

                keyIds[i] = response.accessKey().accessKeyId();
            }

            ListAccessKeysResponse listRes = iam
                    .listAccessKeys(ListAccessKeysRequest.builder()
                                                         .userName(username).build());

            int matches = 0;
            for (AccessKeyMetadata akm : listRes.accessKeyMetadata()) {
                if (akm.accessKeyId().equals(keyIds[0])) {
                    matches |= 1;
                }
                if (akm.accessKeyId().equals(keyIds[1])) {
                    matches |= 2;
                }
            }
            assertEquals(3, matches);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    // There is a limit of 2 access keys per user
    @Test(expected = LimitExceededException.class)
    public void testLimitExceedException() {
        String username = IAMUtil.createTestUser();

        try {
            for (int i = 0; i < 3; i++) {
                iam.createAccessKey(CreateAccessKeyRequest.builder()
                                                          .userName(username).build());
            }
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void testDeleteAccessKey() {
        String username = IAMUtil.createTestUser();
        String[] keyIds = new String[2];
        try {
            for (int i = 0; i < 2; i++) {
                CreateAccessKeyResponse response = iam
                        .createAccessKey(CreateAccessKeyRequest.builder()
                                                               .userName(username).build());

                keyIds[i] = response.accessKey().accessKeyId();
            }

            ListAccessKeysResponse lakRes = iam
                    .listAccessKeys(ListAccessKeysRequest.builder()
                                                         .userName(username).build());

            assertEquals(2, lakRes.accessKeyMetadata().size());

            iam.deleteAccessKey(DeleteAccessKeyRequest.builder().userName(
                    username).accessKeyId(keyIds[0]).build());

            lakRes = iam.listAccessKeys(ListAccessKeysRequest.builder()
                                                             .userName(username).build());

            assertEquals(1, lakRes.accessKeyMetadata().size());
            assertEquals(keyIds[1], lakRes.accessKeyMetadata().get(0)
                                          .accessKeyId());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void testDeleteNonExistentAccessKeyException() {
        String username = IAMUtil.createTestUser();
        try {
            CreateAccessKeyResponse response = iam
                    .createAccessKey(CreateAccessKeyRequest.builder()
                                                           .userName(username).build());

            String keyId = response.accessKey().accessKeyId();

            iam.deleteAccessKey(DeleteAccessKeyRequest.builder().userName(
                    username).accessKeyId(keyId).build());
            iam.deleteAccessKey(DeleteAccessKeyRequest.builder().userName(
                    username).accessKeyId(keyId).build());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

}
