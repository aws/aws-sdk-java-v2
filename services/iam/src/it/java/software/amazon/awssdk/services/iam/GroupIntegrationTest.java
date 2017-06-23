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

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.iam.model.AddUserToGroupRequest;
import software.amazon.awssdk.services.iam.model.CreateGroupRequest;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.DeleteConflictException;
import software.amazon.awssdk.services.iam.model.DeleteGroupRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserRequest;
import software.amazon.awssdk.services.iam.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.iam.model.GetGroupRequest;
import software.amazon.awssdk.services.iam.model.GetGroupResponse;
import software.amazon.awssdk.services.iam.model.Group;
import software.amazon.awssdk.services.iam.model.ListGroupsRequest;
import software.amazon.awssdk.services.iam.model.ListGroupsResponse;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.RemoveUserFromGroupRequest;
import software.amazon.awssdk.services.iam.model.User;

/**
 * Integration tests for group-related IAM interfaces.
 */
public class GroupIntegrationTest extends IntegrationTestBase {

    @Before
    public void PreTestRun() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void TestCreateGetGroup() {
        String groupname = UUID.randomUUID().toString().replace('-', '0');

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname).build());
            GetGroupResponse response = iam.getGroup(GetGroupRequest.builder()
                                                                  .groupName(groupname).build());
            assertEquals(0, response.users().size());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname).build());
        }
    }

    @Test
    public void TestGroupWithUsers() {
        String username1 = IAMUtil.uniqueName(), username2 = IAMUtil
                .uniqueName(), username3 = IAMUtil.uniqueName(), groupname = IAMUtil
                .uniqueName();

        try {
            iam.createUser(CreateUserRequest.builder().userName(username1)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createUser(CreateUserRequest.builder().userName(username2)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createUser(CreateUserRequest.builder().userName(username3)
                                            .path(IAMUtil.TEST_PATH).build());

            iam.createGroup(CreateGroupRequest.builder().groupName(groupname)
                                              .path(IAMUtil.TEST_PATH).build());

            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username1).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username2).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username3).build());

            GetGroupResponse response = iam.getGroup(GetGroupRequest.builder()
                                                                  .groupName(groupname).build());

            assertEquals(3, response.users().size());
            assertFalse(response.isTruncated());

            int matches = 0;

            for (User u : response.users()) {
                if (u.userName().equals(username1)) {
                    matches |= 1;
                }
                if (u.userName().equals(username2)) {
                    matches |= 2;
                }
                if (u.userName().equals(username3)) {
                    matches |= 4;
                }
            }

            assertEquals(7, matches);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username1).build());
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username2).build());
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username3).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username1).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username2).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username3).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname).build());
        }
    }

    @Test
    public void TestRemoveUsersFromGroup() {
        String username1 = IAMUtil.uniqueName(), username2 = IAMUtil
                .uniqueName(), username3 = IAMUtil.uniqueName(), groupname = IAMUtil
                .uniqueName();

        try {
            iam.createUser(CreateUserRequest.builder().userName(username1)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createUser(CreateUserRequest.builder().userName(username2)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createUser(CreateUserRequest.builder().userName(username3)
                                            .path(IAMUtil.TEST_PATH).build());

            iam.createGroup(CreateGroupRequest.builder().groupName(groupname)
                                              .path(IAMUtil.TEST_PATH).build());

            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username1).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username2).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username3).build());

            GetGroupResponse response = iam.getGroup(GetGroupRequest.builder()
                                                                  .groupName(groupname).build());

            assertEquals(3, response.users().size());

            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username2).build());

            response = iam.getGroup(GetGroupRequest.builder()
                                                   .groupName(groupname).build());

            assertEquals(2, response.users().size());

            int matches = 0;

            for (User u : response.users()) {
                if (u.userName().equals(username1)) {
                    matches |= 1;
                }
                if (u.userName().equals(username2)) {
                    fail();
                }
                if (u.userName().equals(username3)) {
                    matches |= 4;
                }
            }

            assertEquals(5, matches);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username1).build());
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username3).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username1).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username2).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username3).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname).build());
        }
    }

    @Test
    public void TestGroupPaging() {
        String username1 = IAMUtil.uniqueName(), username2 = IAMUtil
                .uniqueName(), username3 = IAMUtil.uniqueName(), username4 = IAMUtil
                .uniqueName(), groupname = IAMUtil.uniqueName();

        try {
            iam.createUser(CreateUserRequest.builder().userName(username1)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createUser(CreateUserRequest.builder().userName(username2)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createUser(CreateUserRequest.builder().userName(username3)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createUser(CreateUserRequest.builder().userName(username4)
                                            .path(IAMUtil.TEST_PATH).build());

            iam.createGroup(CreateGroupRequest.builder().groupName(groupname)
                                              .path(IAMUtil.TEST_PATH).build());

            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username1).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username2).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username3).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    groupname).userName(username4).build());

            GetGroupResponse response = iam.getGroup(GetGroupRequest.builder()
                                                                  .groupName(groupname).maxItems(2).build());

            assertEquals(2, response.users().size());
            assertTrue(response.isTruncated());

            String marker = response.marker();

            int matches = 0;

            for (User u : response.users()) {
                if (u.userName().equals(username1)) {
                    matches |= 1;
                }
                if (u.userName().equals(username2)) {
                    matches |= 2;
                }
                if (u.userName().equals(username3)) {
                    matches |= 4;
                }
                if (u.userName().equals(username4)) {
                    matches |= 8;
                }
            }

            response = iam.getGroup(GetGroupRequest.builder().marker(marker)
                                                   .groupName(groupname).build());

            assertEquals(2, response.users().size());
            assertFalse(response.isTruncated());

            for (User u : response.users()) {
                if (u.userName().equals(username1)) {
                    matches |= 1;
                }
                if (u.userName().equals(username2)) {
                    matches |= 2;
                }
                if (u.userName().equals(username3)) {
                    matches |= 4;
                }
                if (u.userName().equals(username4)) {
                    matches |= 8;
                }
            }

            assertEquals(15, matches);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username1).build());
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username2).build());
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username3).build());
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(groupname).userName(username4).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username1).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username2).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username3).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username4).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname).build());
        }
    }

    @Test
    public void TestListGroupWithPaths() {
        String groupname1 = IAMUtil.uniqueName(), groupname2 = IAMUtil
                .uniqueName(), groupname3 = IAMUtil.uniqueName(), groupname4 = IAMUtil
                .uniqueName();

        String pathA = IAMUtil.makePath("A"), pathB = IAMUtil.makePath("B");

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname1)
                                              .path(pathA).build());
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname2)
                                              .path(pathA).build());
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname3)
                                              .path(pathB).build());
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname4)
                                              .path(pathB).build());

            ListGroupsResponse response = iam.listGroups(ListGroupsRequest.builder()
                                                                        .pathPrefix(pathA).build());

            assertEquals(2, response.groups().size());

            int matches = 0;

            for (Group g : response.groups()) {
                if (g.groupName().equals(groupname1)) {
                    matches |= 1;
                }
                if (g.groupName().equals(groupname2)) {
                    matches |= 2;
                }
                if (g.groupName().equals(groupname3)) {
                    fail();
                }
                if (g.groupName().equals(groupname4)) {
                    fail();
                }
            }

            response = iam.listGroups(ListGroupsRequest.builder()
                                                       .pathPrefix(pathB).build());

            assertEquals(2, response.groups().size());

            for (Group g : response.groups()) {
                if (g.groupName().equals(groupname1)) {
                    fail();
                }
                if (g.groupName().equals(groupname2)) {
                    fail();
                }
                if (g.groupName().equals(groupname3)) {
                    matches |= 4;
                }
                if (g.groupName().equals(groupname4)) {
                    matches |= 8;
                }
            }

            assertEquals(15, matches);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname1).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname2).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname3).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname4).build());
        }
    }

    @Test
    public void TestListGroupsPaging() {
        String groupname1 = IAMUtil.uniqueName(), groupname2 = IAMUtil
                .uniqueName(), groupname3 = IAMUtil.uniqueName(), groupname4 = IAMUtil
                .uniqueName();

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname1)
                                              .path(IAMUtil.TEST_PATH).build());
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname2)
                                              .path(IAMUtil.TEST_PATH).build());
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname3)
                                              .path(IAMUtil.TEST_PATH).build());
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname4)
                                              .path(IAMUtil.TEST_PATH).build());

            ListGroupsResponse response = iam.listGroups(ListGroupsRequest.builder()
                                                                        .maxItems(2).pathPrefix(IAMUtil.TEST_PATH).build());

            assertEquals(2, response.groups().size());
            assertTrue(response.isTruncated());

            String marker = response.marker();

            int matches = 0;

            for (Group g : response.groups()) {
                if (g.groupName().equals(groupname1)) {
                    matches |= 1;
                }
                if (g.groupName().equals(groupname2)) {
                    matches |= 2;
                }
                if (g.groupName().equals(groupname3)) {
                    matches |= 4;
                }
                if (g.groupName().equals(groupname4)) {
                    matches |= 8;
                }
            }

            response = iam.listGroups(ListGroupsRequest.builder()
                                                       .marker(marker).pathPrefix(IAMUtil.TEST_PATH).build());

            assertEquals(2, response.groups().size());
            assertFalse(response.isTruncated());

            for (Group g : response.groups()) {
                if (g.groupName().equals(groupname1)) {
                    matches |= 1;
                }
                if (g.groupName().equals(groupname2)) {
                    matches |= 2;
                }
                if (g.groupName().equals(groupname3)) {
                    matches |= 4;
                }
                if (g.groupName().equals(groupname4)) {
                    matches |= 8;
                }
            }

            assertEquals(15, matches);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname1).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname2).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname3).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname4).build());
        }

    }

    @Test(expected = NoSuchEntityException.class)
    public void AddUserToNonExistentGroup() {
        String username = IAMUtil.uniqueName(), grpname = IAMUtil.uniqueName();
        try {
            iam.createUser(CreateUserRequest.builder().userName(username)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                    grpname).userName(username).build());
        } finally {
            iam.deleteUser(DeleteUserRequest.builder().userName(username).build());
        }
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void TestDoubleCreation() {
        String grpname = IAMUtil.uniqueName();

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                              .path(IAMUtil.TEST_PATH).build());
            iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                              .path(IAMUtil.TEST_PATH).build());
        } finally {
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(grpname).build());
        }
    }

    @Test(expected = DeleteConflictException.class)
    public void TestDeleteUserInGroupThrowsException() {
        String username = IAMUtil.uniqueName(), grpname = IAMUtil.uniqueName();

        try {
            iam.createUser(CreateUserRequest.builder().userName(username)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                              .path(IAMUtil.TEST_PATH).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().userName(
                    username).groupName(grpname).build());

            iam.deleteUser(DeleteUserRequest.builder().userName(username).build());
        } finally {
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(grpname).userName(username).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(grpname).build());
        }
    }

    @Test(expected = DeleteConflictException.class)
    public void TestDeleteGroupWithUsersThrowsException() {
        String username = IAMUtil.uniqueName(), grpname = IAMUtil.uniqueName();

        try {
            iam.createUser(CreateUserRequest.builder().userName(username)
                                            .path(IAMUtil.TEST_PATH).build());
            iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                              .path(IAMUtil.TEST_PATH).build());
            iam.addUserToGroup(AddUserToGroupRequest.builder().userName(
                    username).groupName(grpname).build());

            iam.deleteGroup(DeleteGroupRequest.builder().groupName(grpname).build());
        } finally {
            iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                              .groupName(grpname).userName(username).build());
            iam.deleteUser(DeleteUserRequest.builder().userName(username).build());
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(grpname).build());
        }
    }
}
