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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.iam.model.CreateGroupRequest;
import software.amazon.awssdk.services.iam.model.DeleteGroupPolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteGroupRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetGroupPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetGroupPolicyResponse;
import software.amazon.awssdk.services.iam.model.GetUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetUserPolicyResponse;
import software.amazon.awssdk.services.iam.model.ListGroupPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListGroupPoliciesResponse;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesResponse;
import software.amazon.awssdk.services.iam.model.MalformedPolicyDocumentException;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.PutGroupPolicyRequest;
import software.amazon.awssdk.services.iam.model.PutUserPolicyRequest;

/**
 * Integration tests of the policy APIs of IAM.
 */
public class PolicyIntegrationTest extends IntegrationTestBase {

    public static final String TEST_ALLOW_POLICY = "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]}";
    public static final String TEST_DENY_POLICY = "{\"Statement\":[{\"Effect\":\"Deny\",\"Action\":\"*\",\"Resource\":\"*\"}]}";

    @Before
    public void TestSetup() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void TestPutGetUserPolicy() throws UnsupportedEncodingException {
        String username = IAMUtil.createTestUser();
        String policyName = IAMUtil.uniqueName();

        try {
            iam.putUserPolicy(PutUserPolicyRequest.builder().userName(username)
                                                  .policyName(policyName)
                                                  .policyDocument(TEST_ALLOW_POLICY).build());

            GetUserPolicyResponse response = iam
                    .getUserPolicy(GetUserPolicyRequest.builder().userName(
                            username).policyName(policyName).build());

            assertEquals(username, response.userName());
            assertEquals(policyName, response.policyName());
            assertEquals(TEST_ALLOW_POLICY,
                         URLDecoder.decode(response.policyDocument(), "UTF-8"));
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestPutGetGroupPolicy() throws UnsupportedEncodingException {
        String groupname = IAMUtil.uniqueName();
        String policyName = IAMUtil.uniqueName();

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname)
                                              .path(IAMUtil.TEST_PATH).build());

            iam.putGroupPolicy(PutGroupPolicyRequest.builder()
                                                    .groupName(groupname).policyName(policyName)
                                                    .policyDocument(TEST_ALLOW_POLICY).build());

            GetGroupPolicyResponse response = iam
                    .getGroupPolicy(GetGroupPolicyRequest.builder().groupName(
                            groupname).policyName(policyName).build());

            assertEquals(groupname, response.groupName());
            assertEquals(policyName, response.policyName());
            assertEquals(TEST_ALLOW_POLICY,
                         URLDecoder.decode(response.policyDocument(), "UTF-8"));
        } finally {
            iam.deleteGroupPolicy(DeleteGroupPolicyRequest.builder().groupName(
                    groupname).policyName(policyName).build());
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestGetNonExistantPolicy() {
        String username = IAMUtil.createTestUser();
        String policyName = IAMUtil.uniqueName();

        try {
            iam.getUserPolicy(GetUserPolicyRequest.builder().userName(username)
                                                  .policyName(policyName).build());
        } finally {
            IAMUtil.deleteTestUsers();
        }
    }

    @Test
    public void TestListUserPolicies() {
        String username = IAMUtil.createTestUser();
        String[] policyNames = new String[3];
        int nPolicies = 3;

        try {
            for (int i = 0; i < nPolicies; i++) {
                policyNames[i] = IAMUtil.uniqueName();
                iam.putUserPolicy(PutUserPolicyRequest.builder()
                                                      .userName(username).policyName(policyNames[i])
                                                      .policyDocument(TEST_ALLOW_POLICY).build());
            }

            ListUserPoliciesResponse response = iam
                    .listUserPolicies(ListUserPoliciesRequest.builder()
                                                             .userName(username).build());

            assertEquals(nPolicies, response.policyNames().size());

            int matches = 0;
            for (String name : response.policyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }
            assertEquals((1 << nPolicies) - 1, matches);
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestListGroupPolicies() {
        String grpname = IAMUtil.uniqueName();
        String[] policyNames = new String[3];
        int nPolicies = 3;

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                              .path(IAMUtil.TEST_PATH).build());

            for (int i = 0; i < nPolicies; i++) {
                policyNames[i] = IAMUtil.uniqueName();
                iam.putGroupPolicy(PutGroupPolicyRequest.builder()
                                                        .groupName(grpname).policyName(policyNames[i])
                                                        .policyDocument(TEST_ALLOW_POLICY).build());
            }

            ListGroupPoliciesResponse response = iam
                    .listGroupPolicies(ListGroupPoliciesRequest.builder()
                                                               .groupName(grpname).build());

            assertEquals(nPolicies,
                         response.policyNames().size());

            int matches = 0;
            for (String name : response.policyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }
            assertEquals((1 << nPolicies) - 1, matches);
        } finally {
            for (int i = 0; i < nPolicies; i++) {
                iam.deleteGroupPolicy(DeleteGroupPolicyRequest.builder()
                                                              .groupName(grpname).policyName(policyNames[i]).build());
            }

            iam.deleteGroup(DeleteGroupRequest.builder().groupName(grpname).build());
        }
    }

    @Test
    public void TestListUserPoliciesPaging() {
        String username = IAMUtil.createTestUser();
        int nPolicies = 4;
        String[] policyNames = new String[nPolicies];

        try {
            for (int i = 0; i < nPolicies; i++) {
                policyNames[i] = IAMUtil.uniqueName();
                iam.putUserPolicy(PutUserPolicyRequest.builder()
                                                      .userName(username).policyName(policyNames[i])
                                                      .policyDocument(TEST_ALLOW_POLICY).build());
            }

            ListUserPoliciesResponse response = iam
                    .listUserPolicies(ListUserPoliciesRequest.builder()
                                                             .userName(username).maxItems(2).build());

            assertEquals(2, response.policyNames().size());
            assertTrue(response.isTruncated());
            String marker = response.marker();

            int matches = 0;
            for (String name : response.policyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }

            response = iam.listUserPolicies(ListUserPoliciesRequest.builder()
                                                                   .userName(username).marker(marker).build());

            assertEquals(nPolicies - 2,
                         response.policyNames().size());
            assertFalse(response.isTruncated());

            for (String name : response.policyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }

            assertEquals((1 << nPolicies) - 1, matches);
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestListGroupPoliciesPaging() {
        String grpname = IAMUtil.uniqueName();
        int nPolicies = 3;
        String[] policyNames = new String[nPolicies];

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(grpname)
                                              .path(IAMUtil.TEST_PATH).build());

            for (int i = 0; i < nPolicies; i++) {
                policyNames[i] = IAMUtil.uniqueName();
                iam.putGroupPolicy(PutGroupPolicyRequest.builder()
                                                        .groupName(grpname).policyName(policyNames[i])
                                                        .policyDocument(TEST_ALLOW_POLICY).build());
            }

            ListGroupPoliciesResponse response = iam
                    .listGroupPolicies(ListGroupPoliciesRequest.builder()
                                                               .groupName(grpname).maxItems(2).build());

            assertEquals(2,
                         response.policyNames().size());
            assertTrue(response.isTruncated());
            String marker = response.marker();

            int matches = 0;
            for (String name : response.policyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }

            response = iam.listGroupPolicies(ListGroupPoliciesRequest.builder()
                                                                     .groupName(grpname).marker(marker).build());

            assertEquals(nPolicies - 2,
                         response.policyNames().size());
            assertFalse(response.isTruncated());

            for (String name : response.policyNames()) {
                for (int i = 0; i < nPolicies; i++) {
                    if (name.equals(policyNames[i])) {
                        matches |= (1 << i);
                    }
                }
            }

            assertEquals((1 << nPolicies) - 1, matches);
        } finally {
            for (int i = 0; i < nPolicies; i++) {
                iam.deleteGroupPolicy(DeleteGroupPolicyRequest.builder()
                                                              .groupName(grpname).policyName(policyNames[i]).build());
            }

            iam.deleteGroup(DeleteGroupRequest.builder().groupName(grpname).build());
        }
    }

    @Test
    public void TestDeleteUserPolicy() {
        String username = IAMUtil.createTestUser();
        String pName = IAMUtil.uniqueName();

        try {
            iam.putUserPolicy(PutUserPolicyRequest.builder().userName(username)
                                                  .policyName(pName)
                                                  .policyDocument(TEST_ALLOW_POLICY).build());

            ListUserPoliciesResponse response = iam
                    .listUserPolicies(ListUserPoliciesRequest.builder()
                                                             .userName(username).build());

            assertEquals(1, response.policyNames().size());

            iam.deleteUserPolicy(DeleteUserPolicyRequest.builder().userName(
                    username).policyName(pName).build());

            response = iam.listUserPolicies(ListUserPoliciesRequest.builder()
                                                                   .userName(username).build());

            assertEquals(0, response.policyNames().size());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test
    public void TestDeleteGroupPolicy() {
        String groupname = IAMUtil.uniqueName();
        String pName = IAMUtil.uniqueName();

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname)
                                              .path(IAMUtil.TEST_PATH).build());

            iam.putGroupPolicy(PutGroupPolicyRequest.builder()
                                                    .groupName(groupname).policyName(pName)
                                                    .policyDocument(TEST_ALLOW_POLICY).build());

            ListGroupPoliciesResponse response = iam
                    .listGroupPolicies(ListGroupPoliciesRequest.builder()
                                                               .groupName(groupname).build());

            assertEquals(1,
                         response.policyNames().size());

            iam.deleteGroupPolicy(DeleteGroupPolicyRequest.builder().groupName(
                    groupname).policyName(pName).build());

            response = iam.listGroupPolicies(ListGroupPoliciesRequest.builder()
                                                                     .groupName(groupname).build());

            assertEquals(0,
                         response.policyNames().size());
        } finally {
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname).build());
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestDeleteNonExistentGroupPolicyException() {
        String groupname = IAMUtil.uniqueName();

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname)
                                              .path(IAMUtil.TEST_PATH).build());
            iam.deleteGroupPolicy(DeleteGroupPolicyRequest.builder().groupName(
                    groupname).policyName(IAMUtil.uniqueName()).build());
        } finally {
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname).build());
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestGetNonExistentGroupPolicyException() {
        String groupname = IAMUtil.uniqueName();

        try {
            iam.createGroup(CreateGroupRequest.builder().groupName(groupname)
                                              .path(IAMUtil.TEST_PATH).build());
            iam.getGroupPolicy(GetGroupPolicyRequest.builder().groupName(
                    groupname).policyName(IAMUtil.uniqueName()).build());
        } finally {
            iam.deleteGroup(DeleteGroupRequest.builder().groupName(groupname).build());
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestDeleteNonExistentUserPolicyException() {
        String username = IAMUtil.createTestUser();

        try {
            iam.deleteUserPolicy(DeleteUserPolicyRequest.builder().userName(
                    username).policyName(IAMUtil.uniqueName()).build());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestGetNonExistentUserPolicyException() {
        String username = IAMUtil.createTestUser();

        try {
            iam.getUserPolicy(GetUserPolicyRequest.builder().userName(username)
                                                  .policyName(IAMUtil.uniqueName()).build());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = MalformedPolicyDocumentException.class)
    public void TestPutUserPolicyMalformedPolicyDocumentException() {
        String username = IAMUtil.createTestUser();
        String policyName = IAMUtil.uniqueName();

        try {
            iam.putUserPolicy(PutUserPolicyRequest.builder().userName(username)
                                                  .policyName(policyName).policyDocument("[").build());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

}
