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

package software.amazon.awssdk.services.glacier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glacier.model.UploadArchiveRequest;
import software.amazon.awssdk.services.glacier.model.UploadArchiveResponse;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class UploadArchiveIntegrationTest extends AwsIntegrationTestBase {
    private static final Region REGION = Region.US_WEST_2;
    private static final String VAULT_NAME = "test-vault-" + System.currentTimeMillis();
    private static final List<String> CREATED_ARCHIVES = new ArrayList<>();
    private static GlacierAsyncClient asyncClient;

    @BeforeClass
    public static void setup() {
        asyncClient = GlacierAsyncClient.builder()
                .credentialsProvider(getCredentialsProvider())
                .region(REGION)
                .build();

        try {
            asyncClient.createVault(r -> r.vaultName(VAULT_NAME)).join();
        } catch (Exception e) {
            asyncClient.close();
            throw e;
        }
    }

    @AfterClass
    // Vault deletion takes at least 24 hours since that's how often Glacier does an inventory and you cannot
    // delete a vault if a vault has been written to since the last inventory!
    public static void teardown() {
        try {
            for (String archiveId : CREATED_ARCHIVES) {
                asyncClient.deleteArchive(r -> r.vaultName(VAULT_NAME).archiveId(archiveId)).join();
            }
            asyncClient.deleteVault(r -> r.vaultName(VAULT_NAME)).join();
        } finally {
            asyncClient.close();
        }
    }

    @Test
    @Ignore
    public void test_uploadArchive_succeeds() {
        byte[] contents = "Hello Glacier".getBytes(StandardCharsets.UTF_8);
        // echo -n "Hello Glacier" | shasum -a 256
        String checksum = "8290066453a21341f7777318287ade5a52e920109a59d557060520988793345d";

        UploadArchiveRequest request = UploadArchiveRequest.builder()
                .vaultName(VAULT_NAME)
                .contentLength((long) contents.length)
                .checksum(checksum)
                .build();

        UploadArchiveResponse response = asyncClient.uploadArchive(request, AsyncRequestBody.fromBytes(contents))
                .join();

        CREATED_ARCHIVES.add(response.archiveId());
    }
}
