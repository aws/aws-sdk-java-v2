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

package software.amazon.awssdk.services.efs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.services.efs.model.CreateFileSystemRequest;
import software.amazon.awssdk.services.efs.model.DeleteFileSystemRequest;
import software.amazon.awssdk.services.efs.model.DescribeFileSystemsRequest;
import software.amazon.awssdk.services.efs.model.FileSystemAlreadyExistsException;
import software.amazon.awssdk.services.efs.model.FileSystemNotFoundException;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class ElasticFileSystemIntegrationTest extends AwsIntegrationTestBase {

    private static EFSClient client;
    private String fileSystemId;

    @BeforeClass
    public static void setupFixture() throws Exception {
        client = EFSClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.US_WEST_2).build();
    }

    @After
    public void tearDown() {
        if (!StringUtils.isNullOrEmpty(fileSystemId)) {
            client.deleteFileSystem(DeleteFileSystemRequest.builder().fileSystemId(fileSystemId).build());
        }
    }

    @Test
    public void describeFileSystems_ReturnsNonNull() {
        assertNotNull(client.describeFileSystems(DescribeFileSystemsRequest.builder().build()));
    }

    @Test
    public void describeFileSystem_NonExistentFileSystem_ThrowsException() {
        try {
            client.describeFileSystems(DescribeFileSystemsRequest.builder().fileSystemId("fs-00000000").build());
        } catch (FileSystemNotFoundException e) {
            assertEquals("FileSystemNotFound", e.errorCode());
        }
    }

    /**
     * Tests that an exception with a member in it is serialized properly. See TT0064111680
     */
    @Test
    public void createFileSystem_WithDuplicateCreationToken_ThrowsExceptionWithFileSystemIdPresent() {
        String creationToken = UUID.randomUUID().toString();
        this.fileSystemId = client.createFileSystem(CreateFileSystemRequest.builder().creationToken(creationToken).build())
                                  .fileSystemId();
        try {
            client.createFileSystem(CreateFileSystemRequest.builder().creationToken(creationToken).build()).fileSystemId();
        } catch (FileSystemAlreadyExistsException e) {
            assertEquals(fileSystemId, e.fileSystemId());
        }
    }
}
