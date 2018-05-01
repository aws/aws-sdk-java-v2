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

import java.util.UUID;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.iam.model.AccessKeyMetadata;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.DeleteGroupRequest;
import software.amazon.awssdk.services.iam.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.DeleteSigningCertificateRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserRequest;
import software.amazon.awssdk.services.iam.model.GetGroupRequest;
import software.amazon.awssdk.services.iam.model.GetGroupResponse;
import software.amazon.awssdk.services.iam.model.Group;
import software.amazon.awssdk.services.iam.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysResponse;
import software.amazon.awssdk.services.iam.model.ListGroupsRequest;
import software.amazon.awssdk.services.iam.model.ListGroupsResponse;
import software.amazon.awssdk.services.iam.model.ListSigningCertificatesRequest;
import software.amazon.awssdk.services.iam.model.ListSigningCertificatesResponse;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesResponse;
import software.amazon.awssdk.services.iam.model.ListUsersRequest;
import software.amazon.awssdk.services.iam.model.ListUsersResponse;
import software.amazon.awssdk.services.iam.model.RemoveUserFromGroupRequest;
import software.amazon.awssdk.services.iam.model.SigningCertificate;
import software.amazon.awssdk.services.iam.model.User;

public class IAMUtil {
    private static final IAMClient client;
    public static String TEST_PATH = "/IntegrationTests/IAM/";

    static {
        try {
            IntegrationTestBase.setUpCredentials();
            client = IAMClient.builder()
                              .credentialsProvider(IntegrationTestBase.CREDENTIALS_PROVIDER_CHAIN)
                              .overrideConfiguration(c -> c.retryPolicy(RetryPolicy.builder().numRetries(50).build()))
                              .region(Region.AWS_GLOBAL)
                              .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String makePath(String... elements) {
        String path = TEST_PATH;
        for (String s : elements) {
            path = String.format("%s%s/", path, s);
        }

        return path;
    }

    public static void deleteUsersAndGroupsInTestNameSpace() {
        ListGroupsResponse lgRes = client.listGroups(ListGroupsRequest.builder().pathPrefix(TEST_PATH).build());
        for (Group g : lgRes.groups()) {
            GetGroupResponse ggRes = client.getGroup(GetGroupRequest.builder().groupName(g.groupName()).build());
            for (User u : ggRes.users()) {
                client.removeUserFromGroup(RemoveUserFromGroupRequest.builder()
                                                                     .groupName(g.groupName()).userName(u.userName()).build());
            }
            client.deleteGroup(DeleteGroupRequest.builder().groupName(g.groupName()).build());
        }

        ListUsersResponse luRes = client.listUsers(ListUsersRequest.builder()
                                                                 .pathPrefix(TEST_PATH).build());
        for (User u : luRes.users()) {
            deleteTestUsers(u.userName());
        }
    }

    public static void deleteAccessKeysForUser(String username) {
        ListAccessKeysResponse response = client
                .listAccessKeys(ListAccessKeysRequest.builder()
                                                     .userName(username).build());
        for (AccessKeyMetadata akm : response.accessKeyMetadata()) {
            client.deleteAccessKey(DeleteAccessKeyRequest.builder().userName(
                    username).accessKeyId(akm.accessKeyId()).build());
        }
    }

    public static void deleteUserPoliciesForUser(String username) {
        ListUserPoliciesResponse response = client
                .listUserPolicies(ListUserPoliciesRequest.builder()
                                                         .userName(username).build());
        for (String pName : response.policyNames()) {
            client.deleteUserPolicy(DeleteUserPolicyRequest.builder().userName(
                    username).policyName(pName).build());
        }
    }

    public static void deleteCertificatesForUser(String username) {
        ListSigningCertificatesResponse response = client
                .listSigningCertificates(ListSigningCertificatesRequest.builder()
                                                                       .userName(username).build());
        for (SigningCertificate cert : response.certificates()) {
            client.deleteSigningCertificate(DeleteSigningCertificateRequest.builder()
                                                                           .userName(username).certificateId(
                            cert.certificateId()).build());
        }
    }

    public static String createTestUser() {
        String username = uniqueName();
        client.createUser(CreateUserRequest.builder().userName(username)
                                           .path(IAMUtil.TEST_PATH).build());
        return username;
    }

    public static String uniqueName() {
        return "IamIntegrationTests" + UUID.randomUUID().toString().replace('-', '0');
    }

    public static void deleteTestUsers(String... usernames) {
        for (String s : usernames) {
            deleteAccessKeysForUser(s);
            deleteUserPoliciesForUser(s);
            deleteCertificatesForUser(s);
            try {
                client.deleteLoginProfile(DeleteLoginProfileRequest.builder()
                                                                   .userName(s).build());
            } catch (Exception e) {
                /* Nobody cares. */
            }
            client.deleteUser(DeleteUserRequest.builder().userName(s).build());
        }
    }
}
