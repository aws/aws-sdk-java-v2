/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteMarkerReplication;
import software.amazon.awssdk.services.s3.model.DeleteMarkerReplicationStatus;
import software.amazon.awssdk.services.s3.model.Destination;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ReplicationConfiguration;
import software.amazon.awssdk.services.s3.model.ReplicationRule;
import software.amazon.awssdk.services.s3.model.ReplicationRuleFilter;
import software.amazon.awssdk.services.s3.model.ReplicationRuleStatus;
import software.amazon.awssdk.services.s3.model.ReplicationStatus;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

public class ReplicationStandardResponsesIntegrationTest extends S3IntegrationTestBase {
    private static String sourceBucket = temporaryBucketName("replication-integ-test-source-bucket");
    private static String targetBucket = temporaryBucketName("replication-integ-test-target-bucket");
    private static String replicationRoleName = "get-replication-status-test-role";
    private static Role replicationRole = assignRole(replicationRoleName, sourceBucket, targetBucket);

    @BeforeClass
    public static void setupSuite() {
        createBucket(sourceBucket);
        enableVersioning(sourceBucket);
        createBucket(targetBucket);
        enableVersioning(targetBucket);
    }

    @AfterClass
    public static void cleanup() {
        deleteBucketAndAllContents(sourceBucket);
        deleteBucketAndAllContents(targetBucket);
        cleanupRole("get-replication-status-test-role");
    }

    @Test
    public void getReplicationStatusReturnsAResult() throws InterruptedException {

        ReplicationRule replicationRule = createReplicationRule(targetBucket);
        ReplicationConfiguration replicationConfig =
            ReplicationConfiguration.builder().role(replicationRole.arn()).rules(replicationRule).build();
        PutBucketReplicationRequest replicationRequest =
            PutBucketReplicationRequest.builder().bucket(sourceBucket).replicationConfiguration(replicationConfig).build();


        s3.putBucketReplication(replicationRequest);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(sourceBucket)
                                     .key("replication-status-test-key")
                                     .build(),
                     RequestBody.fromString("test: test"));
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                                     .bucket(sourceBucket)
                                                     .key("replication-status-test-key")
                                                     .build();
        HeadObjectResponse headObjectResponse = s3.headObject(headObjectRequest);
        ReplicationStatus replicationStatus = headObjectResponse.replicationStatus();
        while(replicationStatus == ReplicationStatus.PENDING) {
            Thread.sleep(1000);
            headObjectResponse = s3.headObject(headObjectRequest);
            replicationStatus = headObjectResponse.replicationStatus();
        }

        assertThat(replicationStatus).isEqualTo(ReplicationStatus.COMPLETE);

    }

    private ReplicationRule createReplicationRule(String destinationBucket) {
        ReplicationRule replicationRule = ReplicationRule.builder()
                                                         .priority(0)
                                                         .status(ReplicationRuleStatus.ENABLED)
                                                         .deleteMarkerReplication(DeleteMarkerReplication.builder()
                                                                                                         .status(DeleteMarkerReplicationStatus.DISABLED)
                                                                                                         .build())
                                                         .filter(ReplicationRuleFilter.builder().prefix("").build())
                                                         .destination(Destination.builder()
                                                                                 .bucket("arn:aws:s3:::" + destinationBucket)
                                                                                 .build()).build();

        return  replicationRule;

    }

    private static void enableVersioning(String bucketName) {
        VersioningConfiguration configuration = VersioningConfiguration.builder().status("Enabled").build();

        PutBucketVersioningRequest versioningRequest =
            PutBucketVersioningRequest.builder().bucket(bucketName).versioningConfiguration(configuration).build();

        s3.putBucketVersioning(versioningRequest);

    }

    private static Role assignRole(String roleName, String sourceBucket, String destinationBucket) {
        IamClient iamClient = IamClient.builder().build();
        StringBuilder trustPolicy = new StringBuilder();
        trustPolicy.append("{\r\n   ");
        trustPolicy.append("\"Version\":\"2012-10-17\",\r\n   ");
        trustPolicy.append("\"Statement\":[\r\n      {\r\n         ");
        trustPolicy.append("\"Effect\":\"Allow\",\r\n         \"Principal\":{\r\n            ");
        trustPolicy.append("\"Service\":\"s3.amazonaws.com\"\r\n         },\r\n         ");
        trustPolicy.append("\"Action\":\"sts:AssumeRole\"\r\n      }\r\n   ]\r\n}");

        String str = trustPolicy.toString();
        CreateRoleRequest createRoleRequest = CreateRoleRequest.builder()
            .roleName(roleName)
            .assumeRolePolicyDocument(trustPolicy.toString()).build();

        CreateRoleResponse createRoleResponse = iamClient.createRole(createRoleRequest);

        StringBuilder permissionPolicy = new StringBuilder();
        permissionPolicy.append("{\r\n   \"Version\":\"2012-10-17\",\r\n   \"Statement\":[\r\n      {\r\n         ");
        permissionPolicy.append("\"Effect\":\"Allow\",\r\n         \"Action\":[\r\n             ");
        permissionPolicy.append("\"s3:GetObjectVersionForReplication\",\r\n            ");
        permissionPolicy.append("\"s3:GetObjectVersionAcl\"\r\n         ],\r\n         \"Resource\":[\r\n            ");
        permissionPolicy.append("\"arn:aws:s3:::");
        permissionPolicy.append(sourceBucket);
        permissionPolicy.append("/*\"\r\n         ]\r\n      },\r\n      {\r\n         ");
        permissionPolicy.append("\"Effect\":\"Allow\",\r\n         \"Action\":[\r\n            ");
        permissionPolicy.append("\"s3:ListBucket\",\r\n            \"s3:GetReplicationConfiguration\"\r\n         ");
        permissionPolicy.append("],\r\n         \"Resource\":[\r\n            \"arn:aws:s3:::");
        permissionPolicy.append(sourceBucket);
        permissionPolicy.append("\"\r\n         ");
        permissionPolicy.append("]\r\n      },\r\n      {\r\n         \"Effect\":\"Allow\",\r\n         ");
        permissionPolicy.append("\"Action\":[\r\n            \"s3:ReplicateObject\",\r\n            ");
        permissionPolicy.append("\"s3:ReplicateDelete\",\r\n            \"s3:ReplicateTags\",\r\n            ");
        permissionPolicy.append("\"s3:GetObjectVersionTagging\"\r\n\r\n         ],\r\n         ");
        permissionPolicy.append("\"Resource\":\"arn:aws:s3:::");
        permissionPolicy.append(destinationBucket);
        permissionPolicy.append("/*\"\r\n      }\r\n   ]\r\n}");

        PutRolePolicyRequest putRolePolicyRequest = PutRolePolicyRequest.builder()
            .roleName(roleName)
            .policyDocument(permissionPolicy.toString())
            .policyName("crrRolePolicy").build();

        iamClient.putRolePolicy(putRolePolicyRequest);

        return createRoleResponse.role();

    }
    private static void cleanupRole(String roleName) {
        IamClient iamClient = IamClient.builder().build();
        iamClient.deleteRolePolicy(DeleteRolePolicyRequest.builder().policyName("crrRolePolicy").roleName(replicationRoleName).build());
        iamClient.deleteRole(DeleteRoleRequest.builder().roleName(roleName).build());
    }
}
