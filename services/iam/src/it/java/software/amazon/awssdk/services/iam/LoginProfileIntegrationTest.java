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
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.iam.model.CreateLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.CreateLoginProfileResponse;
import software.amazon.awssdk.services.iam.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.iam.model.GetLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.GetLoginProfileResponse;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;

/**
 * Integration tests of the login profile APIs of IAM.
 */
public class LoginProfileIntegrationTest extends IntegrationTestBase {

    @Before
    public void TestSetup() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void TestCreateGetLoginProfile() throws InterruptedException {
        String username = IAMUtil.createTestUser();
        String password = IAMUtil.uniqueName();

        try {
            CreateLoginProfileResponse createRes = iam
                    .createLoginProfile(CreateLoginProfileRequest.builder()
                                                                 .userName(username).password(password).build());

            Thread.sleep(3 * 3600);

            assertEquals(username, createRes.loginProfile().userName());

            GetLoginProfileResponse res = iam
                    .getLoginProfile(GetLoginProfileRequest.builder()
                                                           .userName(username).build());

            assertEquals(username, res.loginProfile().userName());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void TestCreateLoginProfileTwiceException()
            throws InterruptedException {
        String username = IAMUtil.createTestUser();
        String password = IAMUtil.uniqueName();

        try {
            iam.createLoginProfile(CreateLoginProfileRequest.builder()
                                                            .userName(username).password(password).build());
            Thread.sleep(3 * 3600);
            iam.createLoginProfile(CreateLoginProfileRequest.builder()
                                                            .userName(username).password(password).build());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestDeleteLoginProfile() throws InterruptedException {
        String username = IAMUtil.createTestUser();
        String password = IAMUtil.uniqueName();

        try {
            iam.createLoginProfile(CreateLoginProfileRequest.builder()
                                                            .userName(username).password(password).build());
            Thread.sleep(3 * 3600);
            iam.deleteLoginProfile(DeleteLoginProfileRequest.builder()
                                                            .userName(username).build());
            Thread.sleep(3 * 3600);
            iam.getLoginProfile(GetLoginProfileRequest.builder()
                                                      .userName(username).build());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

}
