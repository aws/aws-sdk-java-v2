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

package software.amazon.awssdk.services.codecommit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.codecommit.model.CreateRepositoryRequest;
import software.amazon.awssdk.services.codecommit.model.DeleteRepositoryRequest;
import software.amazon.awssdk.services.codecommit.model.GetRepositoryRequest;
import software.amazon.awssdk.services.codecommit.model.RepositoryDoesNotExistException;
import software.amazon.awssdk.services.codecommit.model.RepositoryMetadata;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Smoke test for {@link CodeCommitClient}.
 */
public class AwsCodeCommitServiceIntegrationTest extends AwsTestBase {

    private static final String REPO_NAME = "java-sdk-test-repo-" + System.currentTimeMillis();
    private static CodeCommitClient client;

    @BeforeClass
    public static void setup() throws FileNotFoundException, IOException {
        setUpCredentials();
        client = CodeCommitClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @AfterClass
    public static void cleanup() {
        try {
            client.deleteRepository(DeleteRepositoryRequest.builder()
                    .repositoryName(REPO_NAME)
                    .build());
        } catch (Exception ignored) {
            System.err.println("Failed to delete repository " + ignored);
        }
    }

    @Test
    public void testOperations() {

        // CreateRepository
        client.createRepository(CreateRepositoryRequest.builder()
                .repositoryName(REPO_NAME)
                .repositoryDescription("My test repo")
                .build());

        // GetRepository
        RepositoryMetadata repoMd = client.getRepository(GetRepositoryRequest.builder()
                .repositoryName(REPO_NAME)
                .build()
        ).repositoryMetadata();
        Assert.assertEquals(REPO_NAME, repoMd.repositoryName());
        assertValid_RepositoryMetadata(repoMd);

        // Can't perform any branch-related operations since we need to create
        // the first branch by pushing a commit via git.

        // DeleteRepository
        client.deleteRepository(DeleteRepositoryRequest.builder()
                .repositoryName(REPO_NAME)
                .build());

    }

    @Test(expected = RepositoryDoesNotExistException.class)
    public void testExceptionHandling() {
        String nonExistentRepoName = UUID.randomUUID().toString();
        client.getRepository(GetRepositoryRequest.builder()
                .repositoryName(nonExistentRepoName)
                .build());
    }

    private void assertValid_RepositoryMetadata(RepositoryMetadata md) {
        Assert.assertNotNull(md.accountId());
        Assert.assertNotNull(md.arn());
        Assert.assertNotNull(md.cloneUrlHttp());
        Assert.assertNotNull(md.cloneUrlSsh());
        Assert.assertNotNull(md.repositoryDescription());
        Assert.assertNotNull(md.repositoryId());
        Assert.assertNotNull(md.repositoryName());
        Assert.assertNotNull(md.creationDate());
        Assert.assertNotNull(md.lastModifiedDate());
    }

}
