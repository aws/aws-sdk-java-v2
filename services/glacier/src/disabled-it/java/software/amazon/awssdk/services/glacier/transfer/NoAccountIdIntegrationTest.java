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

package software.amazon.awssdk.services.glacier.transfer;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.glacier.GlacierIntegrationTestBase;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.test.util.RandomTempFile;

// hchar: last took ~4 hours to run
public class NoAccountIdIntegrationTest extends GlacierIntegrationTestBase {
    private static final long contentLength = 1024 * 1024 * 100 + 123;
    private static final boolean cleanup = true;
    private File randomTempFile;
    private File downloadFile;

    @Before
    public void setup() throws IOException {
        randomTempFile = new RandomTempFile("NoAccountIdIntegrationTest-", contentLength);
        downloadFile = new File(randomTempFile.getParentFile(),
                                randomTempFile.getName() + ".download");

    }

    @After
    public void teanDown() {
        if (cleanup) {
            randomTempFile.delete();
            downloadFile.delete();
        }
    }

    private ArchiveTransferManager newArchiveTransferManager() {
        return new ArchiveTransferManager(glacier, new StaticCredentialsProvider(credentials), new LegacyClientConfiguration());
    }

    /**
     * Tests that the GlacierUtils class can correctly upload and download
     * archives without an account ID specified.
     */
    @Test
    public void testGlacierUtilsWithNoAccountId() throws Exception {
        initializeClient();
        ArchiveTransferManager glacierUtils = newArchiveTransferManager();

        // Upload
        UploadResult uploadResult = glacierUtils.upload(vaultName, "archiveDescription", randomTempFile);
        String archiveId = uploadResult.getArchiveId();
        assertNotNull(archiveId);

        int retries = 0;
        // Download
        for (; ; ) {
            try {
                glacierUtils.download(vaultName, archiveId, downloadFile);
                break;
            } catch (QueueDoesNotExistException ex) {
                ex.printStackTrace();
                if (retries++ > 3) {
                    break;
                }
            }
        }
        assertFileEqualsFile(randomTempFile, downloadFile);
    }
}
