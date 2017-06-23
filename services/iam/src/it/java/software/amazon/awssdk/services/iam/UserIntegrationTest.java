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
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.CreateUserResponse;
import software.amazon.awssdk.services.iam.model.DeleteUserRequest;
import software.amazon.awssdk.services.iam.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.iam.model.GetUserRequest;
import software.amazon.awssdk.services.iam.model.GetUserResponse;
import software.amazon.awssdk.services.iam.model.ListUsersRequest;
import software.amazon.awssdk.services.iam.model.ListUsersResponse;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.UpdateUserRequest;
import software.amazon.awssdk.services.iam.model.User;

/**
 * Integration tests of the user APIs of IAM.
 */
public class UserIntegrationTest extends IntegrationTestBase {

    @Before
    public void PreTestRun() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void TestCreateGetUser() {
        String username = IAMUtil.uniqueName();

        try {
            CreateUserRequest request = CreateUserRequest.builder().userName(
                    username).path(IAMUtil.TEST_PATH).build();
            CreateUserResponse result = iam.createUser(request);
            assertEquals(username, result.user().userName());
            GetUserResponse getUserResult = iam.getUser(GetUserRequest.builder()
                                                                    .userName(username).build());
            assertEquals(username, getUserResult.user().userName());
        } finally {
            iam.deleteUser(DeleteUserRequest.builder().userName(username).build());
        }
    }

    @Test
    public void TestListUsers() {
        String username1 = IAMUtil.createTestUser();
        String username2 = IAMUtil.createTestUser();
        String username3 = IAMUtil.createTestUser();
        try {
            ListUsersResponse Result = iam.listUsers(ListUsersRequest.builder()
                                                                   .pathPrefix(IAMUtil.TEST_PATH).build());

            assertEquals(3, Result.users().size());

            int matches = 0;
            for (User user : Result.users()) {
                if (user.userName().equals(username1)) {
                    matches |= 1;
                }
                if (user.userName().equals(username2)) {
                    matches |= 2;
                }
                if (user.userName().equals(username3)) {
                    matches |= 4;
                }
            }
            assertEquals(7, matches);
        } finally {
            IAMUtil.deleteTestUsers(username1, username2, username3);
        }
    }

    @Test
    public void TestUserWithPath() {
        String username = IAMUtil.uniqueName();
        String path = IAMUtil.makePath("one", "two", "three");
        try {
            iam.createUser(CreateUserRequest.builder().path(path).userName(
                    username).build());
            GetUserResponse Result = iam.getUser(GetUserRequest.builder()
                                                             .userName(username).build());
            assertEquals(username, Result.user().userName());
            assertEquals(path, Result.user().path());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestListUsersByPath() {
        String username1 = IAMUtil.uniqueName();
        String username2 = IAMUtil.uniqueName();
        String username3 = IAMUtil.uniqueName();
        String username4 = IAMUtil.uniqueName();

        String pathA = IAMUtil.makePath("A");
        String pathB = IAMUtil.makePath("B");

        try {
            iam.createUser(CreateUserRequest.builder().userName(username1)
                                            .path(pathA).build());
            iam.createUser(CreateUserRequest.builder().userName(username2)
                                            .path(pathA).build());
            iam.createUser(CreateUserRequest.builder().userName(username3)
                                            .path(pathB).build());
            iam.createUser(CreateUserRequest.builder().userName(username4)
                                            .path(pathA).build());

            ListUsersResponse Result = iam.listUsers(ListUsersRequest.builder()
                                                                   .pathPrefix(pathA).build());

            assertEquals(3, Result.users().size());

            int matches = 0;

            for (User u : Result.users()) {
                if (u.userName().equals(username1)) {
                    matches |= 1;
                }
                if (u.userName().equals(username2)) {
                    matches |= 2;
                }
                if (u.userName().equals(username4)) {
                    matches |= 4;
                }
                if (u.userName().equals(username3)) {
                    fail();
                }
            }
            assertEquals(7, matches);

            Result = iam
                    .listUsers(ListUsersRequest.builder().pathPrefix(pathB).build());

            assertEquals(1, Result.users().size());

            matches = 0;

            for (User u : Result.users()) {
                if (u.userName().equals(username1)) {
                    fail();
                }
                if (u.userName().equals(username2)) {
                    fail();
                }
                if (u.userName().equals(username4)) {
                    fail();
                }
                if (u.userName().equals(username3)) {
                    matches = 1;
                }
            }
            assertEquals(1, matches);

            Result = iam.listUsers(ListUsersRequest.builder()
                                                   .pathPrefix(IAMUtil.TEST_PATH).build());
            assertEquals(4, Result.users().size());

        } finally {
            IAMUtil.deleteTestUsers(username1, username2, username3, username4);
        }
    }

    @Test
    public void TestListUsersMaxResults() {
        String username1 = IAMUtil.createTestUser();
        String username2 = IAMUtil.createTestUser();
        String username3 = IAMUtil.createTestUser();
        String username4 = IAMUtil.createTestUser();

        try {
            ListUsersResponse Result = iam.listUsers(ListUsersRequest.builder()
                                                                   .maxItems(2).pathPrefix(IAMUtil.TEST_PATH).build());

            assertEquals(2, Result.users().size());
            assertEquals(true, Result.isTruncated());

            int matches = 0;

            for (User u : Result.users()) {
                if (u.userName().equals(username1)) {
                    matches |= 1;
                }
                if (u.userName().equals(username2)) {
                    matches |= 2;
                }
                if (u.userName().equals(username4)) {
                    matches |= 3;
                }
                if (u.userName().equals(username3)) {
                    matches |= 4;
                }
            }

            String marker = Result.marker();

            Result = iam.listUsers(ListUsersRequest.builder().pathPrefix(
                    IAMUtil.TEST_PATH).marker(marker).build());

            assertEquals(2, Result.users().size());
            assertEquals(false, Result.isTruncated());

            for (User u : Result.users()) {
                if (u.userName().equals(username1)) {
                    matches |= 1;
                }
                if (u.userName().equals(username2)) {
                    matches |= 2;
                }
                if (u.userName().equals(username4)) {
                    matches |= 3;
                }
                if (u.userName().equals(username3)) {
                    matches |= 4;
                }
            }

            assertEquals(7, matches);
        } finally {
            IAMUtil.deleteTestUsers(username1, username2, username3, username4);
        }
    }

    @Test
    public void TestUpdateUser() {
        String username = IAMUtil.uniqueName(), newusername = IAMUtil
                .uniqueName();
        String firstPath = IAMUtil.makePath("first"), secondPath = IAMUtil
                .makePath("second");

        try {
            iam.createUser(CreateUserRequest.builder().userName(username)
                                            .path(firstPath).build());

            GetUserResponse Result = iam.getUser(GetUserRequest.builder()
                                                             .userName(username).build());
            assertEquals(firstPath, Result.user().path());

            String id = Result.user().userId();

            iam.updateUser(UpdateUserRequest.builder().userName(username)
                                            .newPath(secondPath).newUserName(newusername).build());

            Result = iam.getUser(GetUserRequest.builder().userName(newusername).build());

            assertEquals(newusername, Result.user().userName());
            assertEquals(secondPath, Result.user().path());
            assertEquals(id, Result.user().userId());
        } finally {
            iam.deleteUser(DeleteUserRequest.builder().userName(newusername).build());
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestDeleteUser() {
        String username = IAMUtil.uniqueName();

        iam.createUser(CreateUserRequest.builder().userName(username).path(
                IAMUtil.TEST_PATH).build());

        GetUserResponse Result = iam.getUser(GetUserRequest.builder()
                                                         .userName(username).build());
        assertEquals(username, Result.user().userName());

        iam.deleteUser(DeleteUserRequest.builder().userName(username).build());

        iam.getUser(GetUserRequest.builder().userName(username).build());
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void TestDoubleCreateUser() {
        String username = IAMUtil.uniqueName();

        try {
            iam.createUser(CreateUserRequest.builder().userName(username)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createUser(CreateUserRequest.builder().userName(username)
                                            .path(IAMUtil.TEST_PATH).build());
        } finally {
            iam.deleteUser(DeleteUserRequest.builder().userName(username).build());
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestUpdateNonexistantUser() {
        String username = IAMUtil.uniqueName();

        iam.updateUser(UpdateUserRequest.builder().userName(username)
                                        .newPath("/lala/").build());
    }

}
