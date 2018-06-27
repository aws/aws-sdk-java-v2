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

package software.amazon.awssdk.services.ecr;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ecr.model.CreateRepositoryRequest;
import software.amazon.awssdk.services.ecr.model.CreateRepositoryResponse;
import software.amazon.awssdk.services.ecr.model.DeleteRepositoryRequest;
import software.amazon.awssdk.services.ecr.model.DescribeRepositoriesRequest;
import software.amazon.awssdk.services.ecr.model.Repository;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class EcrIntegrationTest extends AwsIntegrationTestBase {

    private static final String REPO_NAME = "java-sdk-test-repo-" + System.currentTimeMillis();
    private static EcrClient ecr;

    @BeforeClass
    public static void setUpClient() throws Exception {
        ecr = EcrClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (ecr != null) {
            ecr.deleteRepository(DeleteRepositoryRequest.builder()
                    .repositoryName(REPO_NAME)
                    .build());
        }
    }

    @Test
    public void basicTest() {
        CreateRepositoryResponse result = ecr.createRepository(
                CreateRepositoryRequest.builder()
                        .repositoryName(REPO_NAME)
                        .build());

        Assert.assertNotNull(result.repository());
        Assert.assertEquals(result.repository().repositoryName(), REPO_NAME);
        Assert.assertNotNull(result.repository().repositoryArn());
        Assert.assertNotNull(result.repository().registryId());

        String repoArn = result.repository().repositoryArn();
        String registryId = result.repository().registryId();

        Repository repo = ecr.describeRepositories(DescribeRepositoriesRequest.builder()
                .repositoryNames(REPO_NAME)
                .build())
                .repositories().get(0);

        Assert.assertEquals(repo.registryId(), registryId);
        Assert.assertEquals(repo.repositoryName(), REPO_NAME);
        Assert.assertEquals(repo.repositoryArn(), repoArn);
    }

}
