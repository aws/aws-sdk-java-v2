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

import static org.assertj.core.api.Assertions.assertThat;
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
import software.amazon.awssdk.testutils.Waiter;

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
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname).path(IAMUtil.TEST_PATH).build());
            waitForGroupsToBeCreated(groupname);
            GetGroupResponse response = iam.getGroup(GetGroupRequest.builder()
                                                                  .groupName(groupname).build());
            assertEquals(0, response.users().size());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void TestGroupWithUsers() {
        String username1 = IAMUtil.uniqueName(), username2 = IAMUtil
                .uniqueName(), username3 = IAMUtil.uniqueName(), groupname = IAMUtil
                .uniqueName();

        iam.createUser(CreateUserRequest.builder().userName(username1)
                                        .path(IAMUtil.TEST_PATH).build());
        iam.createUser(CreateUserRequest.builder().userName(username2)
                                        .path(IAMUtil.TEST_PATH).build());
        iam.createUser(CreateUserRequest.builder().userName(username3)
                                        .path(IAMUtil.TEST_PATH).build());

        iam.createGroup(CreateGroupRequest.builder().groupName(groupname)
                                          .path(IAMUtil.TEST_PATH).build());

        waitForUsersToBeCreated(username1, username2, username3);
        waitForGroupsToBeCreated(groupname);

        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username1).build());
        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username2).build());
        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username3).build());

        GetGroupResponse response =
                Waiter.run(() -> iam.getGroup(r -> r.groupName(groupname)))
                      .until(r -> r.users().size() == 3)
                      .orFail();

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
    }

    @Test
    public void TestRemoveUsersFromGroup() {
        String username1 = IAMUtil.uniqueName();
        String username2 = IAMUtil.uniqueName();
        String username3 = IAMUtil.uniqueName();
        String groupname = IAMUtil.uniqueName();

        iam.createUser(CreateUserRequest.builder().userName(username1)
                                        .path(IAMUtil.TEST_PATH).build());
        iam.createUser(CreateUserRequest.builder().userName(username2)
                                        .path(IAMUtil.TEST_PATH).build());
        iam.createUser(CreateUserRequest.builder().userName(username3)
                                        .path(IAMUtil.TEST_PATH).build());

        iam.createGroup(CreateGroupRequest.builder().groupName(groupname)
                                          .path(IAMUtil.TEST_PATH).build());

        waitForUsersToBeCreated(username1, username2, username3);
        waitForGroupsToBeCreated(groupname);

        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username1).build());
        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username2).build());
        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username3).build());

        Waiter.run(() -> iam.getGroup(r -> r.groupName(groupname)))
              .until(r -> r.users().size() == 3)
              .orFail();

        iam.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                          .groupName(groupname).userName(username2).build());


        GetGroupResponse response = Waiter.run(() -> iam.getGroup(r -> r.groupName(groupname)))
                                          .until(r -> r.users().size() == 2)
                                          .orFail();

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
    }

    @Test
    public void TestGroupPaging() {
        String username1 = IAMUtil.uniqueName(), username2 = IAMUtil
                .uniqueName(), username3 = IAMUtil.uniqueName(), username4 = IAMUtil
                .uniqueName(), groupname = IAMUtil.uniqueName();

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

        waitForUsersToBeCreated(username1, username2, username3, username4);
        waitForGroupsToBeCreated(groupname);

        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username1).build());
        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username2).build());
        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username3).build());
        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                groupname).userName(username4).build());

        Waiter.run(() -> iam.getGroup(r -> r.groupName(groupname)))
              .until(r -> r.users().size() == 4)
              .orFail();

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

            waitForGroupsToBeCreated(groupname1, groupname2, groupname3, groupname4);

            ListGroupsResponse response = iam.listGroups(r -> r.pathPrefix(pathA));

            assertThat(response.groups().size()).isEqualTo(2);

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

            response = iam.listGroups(r -> r.pathPrefix(pathB));

            assertThat(response.groups().size()).isEqualTo(2);

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
        } finally {
            deleteGroupsAndTheirUsersQuietly(groupname1, groupname2, groupname3, groupname4);
        }
    }

    @Test
    public void TestListGroupsPaging() {
        String groupname1 = IAMUtil.uniqueName(), groupname2 = IAMUtil
                .uniqueName(), groupname3 = IAMUtil.uniqueName(), groupname4 = IAMUtil
                .uniqueName();

        iam.createGroup(CreateGroupRequest.builder().groupName(groupname1)
                                          .path(IAMUtil.TEST_PATH).build());
        iam.createGroup(CreateGroupRequest.builder().groupName(groupname2)
                                          .path(IAMUtil.TEST_PATH).build());
        iam.createGroup(CreateGroupRequest.builder().groupName(groupname3)
                                          .path(IAMUtil.TEST_PATH).build());
        iam.createGroup(CreateGroupRequest.builder().groupName(groupname4)
                                          .path(IAMUtil.TEST_PATH).build());

        waitForGroupsToBeCreated(groupname1, groupname2, groupname3, groupname4);

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

    }

    @Test(expected = NoSuchEntityException.class)
    public void AddUserToNonExistentGroup() {
        String username = IAMUtil.uniqueName(), grpname = IAMUtil.uniqueName();

        iam.createUser(CreateUserRequest.builder().userName(username)
                                        .path(IAMUtil.TEST_PATH).build());
        waitForUsersToBeCreated(username);
        iam.addUserToGroup(AddUserToGroupRequest.builder().groupName(
                grpname).userName(username).build());
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void TestDoubleCreation() {
        String grpname = IAMUtil.uniqueName();

        iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                          .path(IAMUtil.TEST_PATH).build());
        waitForGroupsToBeCreated(grpname);
        iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                          .path(IAMUtil.TEST_PATH).build());
    }

    @Test(expected = DeleteConflictException.class)
    public void TestDeleteUserInGroupThrowsException() {
        String username = IAMUtil.uniqueName(), grpname = IAMUtil.uniqueName();

        iam.createUser(CreateUserRequest.builder().userName(username)
                                        .path(IAMUtil.TEST_PATH).build());
        iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                          .path(IAMUtil.TEST_PATH).build());
        waitForUsersToBeCreated(username);
        waitForGroupsToBeCreated(grpname);
        iam.addUserToGroup(AddUserToGroupRequest.builder().userName(
                username).groupName(grpname).build());

        Waiter.run(() -> iam.getGroup(r -> r.groupName(grpname)))
              .until(group -> group.users().size() == 1)
              .orFail();

        iam.deleteUser(DeleteUserRequest.builder().userName(username).build());
    }

    @Test(expected = DeleteConflictException.class)
    public void TestDeleteGroupWithUsersThrowsException() {
        String username = IAMUtil.uniqueName(), grpname = IAMUtil.uniqueName();

        iam.createUser(CreateUserRequest.builder().userName(username)
                                        .path(IAMUtil.TEST_PATH).build());
        iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                          .path(IAMUtil.TEST_PATH).build());
        waitForUsersToBeCreated(username);
        waitForGroupsToBeCreated(grpname);
        iam.addUserToGroup(AddUserToGroupRequest.builder().userName(
                username).groupName(grpname).build());

        Waiter.run(() -> iam.getGroup(r -> r.groupName(grpname)))
              .until(group -> group.users().size() == 1)
              .orFail();

        iam.deleteGroup(DeleteGroupRequest.builder().groupName(grpname).build());
    }
}
